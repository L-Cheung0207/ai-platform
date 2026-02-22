package com.example.platform.service;

import com.example.platform.entity.LlmLeaderboardEntry;
import com.example.platform.repository.LlmLeaderboardRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 爬取 OpenLM Chatbot Arena 排行榜：https://openlm.ai/chatbot-arena/
 * 解析页面表格得到 Model, Arena Elo, Coding, Vision, AAII, MMLU-Pro, ARC-AGI, Organization, License
 */
@Service
public class OpenLmArenaScraperService {

    private static final Logger log = LoggerFactory.getLogger(OpenLmArenaScraperService.class);
    private static final String TARGET_URL = "https://openlm.ai/chatbot-arena/";
    private static final int MODEL_NAME_MAX = 300;
    private static final int URL_MAX = 500;
    private static final int ORG_MAX = 200;
    private static final int LICENSE_MAX = 100;
    private static final Pattern RANK_BADGE = Pattern.compile("^([🏆🥇🥈🥉🪙])\\s*");

    private final LlmLeaderboardRepository repository;

    @Value("${app.scraper.llm-leaderboard.enabled:true}")
    private boolean enabled;

    @Value("${app.scraper.llm-leaderboard.headless:true}")
    private boolean headless;

    private volatile ScrapeStatus lastStatus = new ScrapeStatus("idle", null, null, null);

    public OpenLmArenaScraperService(LlmLeaderboardRepository repository) {
        this.repository = repository;
    }

    @Async("scraperTaskExecutor")
    public void doScrapeAsync() {
        lastStatus = new ScrapeStatus("running", null, null, null);
        try {
            int count = doScrape();
            lastStatus = new ScrapeStatus("completed", count, null, Instant.now());
            log.info("OpenLM Arena scrape completed, saved {} entries", count);
        } catch (Exception e) {
            lastStatus = new ScrapeStatus("failed", -1, e.getMessage(), Instant.now());
            log.error("OpenLM Arena scrape failed", e);
        }
    }

    public ScrapeStatus getLastStatus() {
        return lastStatus;
    }

    public record ScrapeStatus(String status, Integer added, String error, Instant finishedAt) {}

    public int doScrape() throws Exception {
        if (!enabled) {
            log.debug("LLM leaderboard scraper is disabled");
            return 0;
        }
        ChromeOptions options = new ChromeOptions();
        if (headless) {
            options.addArguments("--headless=new");
        }
        options.addArguments("--no-sandbox", "--disable-dev-shm-usage", "--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--user-agent=Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

        WebDriver driver = new ChromeDriver(options);
        try {
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(45));
            log.info("Loading OpenLM Arena: {}", TARGET_URL);
            driver.get(TARGET_URL);
            new WebDriverWait(driver, Duration.ofSeconds(25))
                    .until(d -> d.getPageSource().length() > 10000);
            Thread.sleep(3000);
            for (int i = 0; i < 5; i++) {
                ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("window.scrollTo(0, document.body.scrollHeight);");
                Thread.sleep(400);
            }
            String html = driver.getPageSource();
            List<LlmLeaderboardEntry> entries = parseTable(html);
            if (entries.isEmpty()) {
                log.warn("No table rows parsed from OpenLM Arena page");
                return 0;
            }
            return saveAll(entries);
        } finally {
            driver.quit();
        }
    }

    private List<LlmLeaderboardEntry> parseTable(String html) {
        List<LlmLeaderboardEntry> list = new ArrayList<>();
        Document doc = Jsoup.parse(html, TARGET_URL);
        Elements tables = doc.select("table");
        if (tables.isEmpty()) {
            log.debug("No <table> found, trying data attributes or div grid");
            parseTableFallback(doc, list);
            return list;
        }
        for (Element table : tables) {
            Elements rows = table.select("tbody tr");
            if (rows.isEmpty()) {
                rows = table.select("tr");
            }
            int order = 0;
            for (Element tr : rows) {
                Elements tds = tr.select("td");
                if (tds.size() < 2) continue;
                LlmLeaderboardEntry entry = parseRow(tds, ++order);
                if (entry != null && entry.getModelName() != null && !entry.getModelName().isBlank()) {
                    list.add(entry);
                }
            }
            if (!list.isEmpty()) break;
        }
        if (list.isEmpty()) {
            parseTableFallback(doc, list);
        }
        return list;
    }

    /** 兼容：页面可能是 div 或其它结构，通过链接 + 数字列推断行 */
    private void parseTableFallback(Document doc, List<LlmLeaderboardEntry> list) {
        Elements links = doc.select("a[href]");
        for (Element a : links) {
            String href = a.attr("abs:href");
            String text = a.text().trim();
            if (text.length() < 2 || text.length() > MODEL_NAME_MAX) continue;
            if (href == null || href.isBlank() || href.startsWith("javascript:")) continue;
            Element row = a.closest("tr");
            if (row == null) row = a.parent();
            if (row == null) continue;
            Elements cells = row.select("td");
            if (cells.isEmpty()) cells = row.select("th");
            if (cells.size() < 2) continue;
            int order = list.size() + 1;
            LlmLeaderboardEntry entry = parseRowFromCells(cells, text, href, order);
            if (entry != null) list.add(entry);
        }
    }

    private LlmLeaderboardEntry parseRow(Elements tds, int order) {
        String modelName = null;
        String modelUrl = null;
        String rankBadge = null;
        Element first = tds.get(0);
        Element link = first.selectFirst("a[href]");
        if (link != null) {
            modelName = link.text().trim();
            modelUrl = link.attr("abs:href");
            String firstText = first.text().trim();
            var m = RANK_BADGE.matcher(firstText);
            if (m.find()) rankBadge = m.group(1);
        } else {
            modelName = first.text().trim();
            var m = RANK_BADGE.matcher(modelName);
            if (m.find()) {
                rankBadge = m.group(1);
                modelName = modelName.substring(m.group(0).length()).trim();
            }
        }
        if (modelName == null || modelName.isBlank()) return null;
        if (modelName.length() > MODEL_NAME_MAX) modelName = modelName.substring(0, MODEL_NAME_MAX);
        if (modelUrl != null && modelUrl.length() > URL_MAX) modelUrl = modelUrl.substring(0, URL_MAX);

        LlmLeaderboardEntry entry = new LlmLeaderboardEntry();
        entry.setModelName(modelName);
        entry.setModelUrl(modelUrl);
        entry.setRankBadge(rankBadge);
        entry.setDisplayOrder(order);
        entry.setScrapedAt(Instant.now());

        // 页面表格在「模型」后多一列，数据从 index 2 起：Arena Elo, Coding, Vision, AAII, MMLU-Pro, ARC-AGI, Organization, License
        if (tds.size() > 2) entry.setArenaElo(parseIntOrNull(tds.get(2).text()));
        if (tds.size() > 3) entry.setCoding(parseIntOrNull(tds.get(3).text()));
        if (tds.size() > 4) entry.setVision(parseIntOrNull(tds.get(4).text()));
        if (tds.size() > 5) entry.setAaii(parseIntOrNull(tds.get(5).text()));
        if (tds.size() > 6) entry.setMmluPro(parseIntOrNull(tds.get(6).text()));
        if (tds.size() > 7) entry.setArcAgi(parseIntOrNull(tds.get(7).text()));
        if (tds.size() > 8) entry.setOrganization(trimTo(tds.get(8).text(), ORG_MAX));
        if (tds.size() > 9) entry.setLicenseName(trimTo(tds.get(9).text(), LICENSE_MAX));
        return entry;
    }

    private LlmLeaderboardEntry parseRowFromCells(Elements cells, String modelName, String modelUrl, int order) {
        if (modelName.length() > MODEL_NAME_MAX) modelName = modelName.substring(0, MODEL_NAME_MAX);
        if (modelUrl != null && modelUrl.length() > URL_MAX) modelUrl = modelUrl.substring(0, URL_MAX);
        LlmLeaderboardEntry entry = new LlmLeaderboardEntry();
        entry.setModelName(modelName);
        entry.setModelUrl(modelUrl);
        entry.setDisplayOrder(order);
        entry.setScrapedAt(Instant.now());
        // 与 parseRow 一致：数据从 index 2 起
        if (cells.size() > 2) entry.setArenaElo(parseIntOrNull(cells.get(2).text()));
        if (cells.size() > 3) entry.setCoding(parseIntOrNull(cells.get(3).text()));
        if (cells.size() > 4) entry.setVision(parseIntOrNull(cells.get(4).text()));
        if (cells.size() > 5) entry.setAaii(parseIntOrNull(cells.get(5).text()));
        if (cells.size() > 6) entry.setMmluPro(parseIntOrNull(cells.get(6).text()));
        if (cells.size() > 7) entry.setArcAgi(parseIntOrNull(cells.get(7).text()));
        if (cells.size() > 8) entry.setOrganization(trimTo(cells.get(8).text(), ORG_MAX));
        if (cells.size() > 9) entry.setLicenseName(trimTo(cells.get(9).text(), LICENSE_MAX));
        return entry;
    }

    private static Integer parseIntOrNull(String s) {
        if (s == null) return null;
        s = s.replaceAll("[^0-9.-]", "").trim();
        if (s.isEmpty()) return null;
        try {
            return (int) Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String trimTo(String s, int max) {
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty()) return null;
        return s.length() > max ? s.substring(0, max) : s;
    }

    @Transactional
    public int saveAll(List<LlmLeaderboardEntry> entries) {
        repository.deleteAll();
        Instant scrapedAt = Instant.now();
        for (LlmLeaderboardEntry e : entries) {
            if (e.getScrapedAt() == null) e.setScrapedAt(scrapedAt);
        }
        repository.saveAll(entries);
        return entries.size();
    }
}
