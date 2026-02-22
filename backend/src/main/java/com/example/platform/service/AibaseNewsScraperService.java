package com.example.platform.service;

import com.example.platform.dto.NewsWriteRequest;
import com.example.platform.entity.News;
import com.example.platform.repository.NewsRepository;
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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 定时爬取 AIBase「最新AI日报」并入库。
 * 使用 Selenium 无头浏览器处理 JavaScript 渲染的 SPA 页面。
 * 来源: https://news.aibase.com/zh/news
 */
@Service
public class AibaseNewsScraperService {

    private static final Logger log = LoggerFactory.getLogger(AibaseNewsScraperService.class);
    private static final String AIBASE_NEWS_URL = "https://news.aibase.com/zh/news";
    private static final String SOURCE_NAME = "AIBase";
    private static final Pattern NEWS_LINK_PATTERN = Pattern.compile("https?://news\\.aibase\\.com/zh/news/(\\d+)");
    private static final Pattern TITLE_SUFFIX_PATTERN = Pattern.compile(
            "\\s+(刚刚|\\d+\\s*小时前|昨天|\\d+\\s*天前|前天)\\s+[\\d.]+\\s*[kK]?\\s*$");

    private final NewsService newsService;
    private final NewsRepository newsRepository;

    @Value("${app.scraper.aibase.enabled:true}")
    private boolean enabled;

    @Value("${app.scraper.aibase.headless:true}")
    private boolean headless;

    private volatile ScrapeStatus lastStatus = new ScrapeStatus("idle", null, null, null);

    public AibaseNewsScraperService(NewsService newsService, NewsRepository newsRepository) {
        this.newsService = newsService;
        this.newsRepository = newsRepository;
    }

    @Scheduled(cron = "${app.scraper.aibase.cron:0 0 8 * * ?}")
    public void scrape() {
        if (!enabled) {
            log.debug("AIBase news scraper is disabled, skipping");
            return;
        }
        log.info("Starting AIBase news scrape...");
        try {
            int added = doScrape();
            log.info("AIBase news scrape completed, added {} new items", added);
        } catch (Exception e) {
            log.error("AIBase news scrape failed", e);
        }
    }

    @Async("scraperTaskExecutor")
    public void doScrapeAsync() {
        lastStatus = new ScrapeStatus("running", null, null, null);
        try {
            int added = doScrape();
            lastStatus = new ScrapeStatus("completed", added, null, Instant.now());
            log.info("Async AIBase scrape completed, added {} items", added);
        } catch (Exception e) {
            lastStatus = new ScrapeStatus("failed", -1, e.getMessage(), Instant.now());
            log.error("Async AIBase scrape failed", e);
        }
    }

    public ScrapeStatus getLastStatus() {
        return lastStatus;
    }

    public record ScrapeStatus(String status, Integer added, String error, Instant finishedAt) {}

    public int doScrape() throws Exception {
        ChromeOptions options = new ChromeOptions();
        if (headless) {
            options.addArguments("--headless=new");
        }
        options.addArguments("--no-sandbox", "--disable-dev-shm-usage", "--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--user-agent=Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

        WebDriver driver = new ChromeDriver(options);
        try {
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(20));
            driver.get(AIBASE_NEWS_URL);

            new WebDriverWait(driver, Duration.ofSeconds(15))
                    .until(d -> d.getPageSource().contains("最新AI日报") || d.getPageSource().contains("/zh/news/"));

            String pageSource = driver.getPageSource();
            Document doc = Jsoup.parse(pageSource, AIBASE_NEWS_URL);

            Element dailySection = findDailySection(doc);
            if (dailySection == null) {
                log.warn("Could not find 最新AI日报 section, falling back to full page");
                dailySection = doc.body();
            }
            Elements links = dailySection.select("a[href*='/zh/news/']");
            log.info("Found {} links in section", links.size());
            int added = processLinks(driver, links);
            log.info("Scrape result: {} new items added", added);
            return added;
        } finally {
            driver.quit();
        }
    }

    private static final Pattern DATE_PATTERN = Pattern.compile("(\\d{4})年(\\d{1,2})月(\\d{1,2})号");
    private static final int SUMMARY_MAX = 2000;

    private int processLinks(WebDriver driver, Elements links) {
        int added = 0;
        for (Element link : links) {
            String href = link.attr("abs:href");
            if (href == null || href.isBlank()) {
                href = link.attr("href");
                if (href != null && !href.startsWith("http")) {
                    href = "https://news.aibase.com" + (href.startsWith("/") ? "" : "/") + href;
                }
            }

            Matcher m = NEWS_LINK_PATTERN.matcher(href);
            if (!m.matches()) {
                continue;
            }

            if (newsRepository.existsBySourceUrl(href)) {
                continue;
            }

            try {
                driver.get(href);
                new WebDriverWait(driver, Duration.ofSeconds(10))
                        .until(d -> d.getPageSource().contains("正文") || d.getPageSource().contains("<h1"));
                Thread.sleep(500);

                String pageSource = driver.getPageSource();
                Document detailDoc = Jsoup.parse(pageSource, href);

                String title = extractDetailTitle(detailDoc);
                String summary = extractDetailSummary(detailDoc);
                LocalDate publishDate = extractDetailDate(detailDoc);

                if (title == null || title.isBlank()) {
                    title = cleanTitle(link.text());
                }
                if (title.length() > 300) {
                    title = title.substring(0, 300);
                }
                if (summary == null || summary.isBlank()) {
                    summary = title;
                } else if (summary.length() > SUMMARY_MAX) {
                    summary = summary.substring(0, SUMMARY_MAX);
                }
                if (publishDate == null) {
                    publishDate = LocalDate.now();
                }

                NewsWriteRequest req = new NewsWriteRequest();
                req.setTitle(title);
                req.setSummary(summary);
                req.setSourceUrl(href);
                req.setSourceName(SOURCE_NAME);
                req.setPublishDate(publishDate);

                newsService.create(req);
                added++;
            } catch (Exception e) {
                log.warn("Failed to scrape detail page {}: {}", href, e.getMessage());
            }
        }
        return added;
    }

    private String extractDetailTitle(Document doc) {
        Element h1 = doc.selectFirst("h1");
        return h1 != null ? h1.text().trim() : null;
    }

    private String extractDetailSummary(Document doc) {
        StringBuilder sb = new StringBuilder();
        for (Element el : doc.select("article p, [class*='content'] p, [class*='article'] p, .prose p")) {
            String t = el.text().trim();
            if (!t.isEmpty() && !t.contains("相关推荐") && !t.matches("^#+\\s.*")) {
                if (sb.length() > 0) sb.append("\n\n");
                sb.append(t);
                if (sb.length() >= SUMMARY_MAX) break;
            }
        }
        if (sb.length() > 0) return sb.toString();
        for (Element p : doc.select("p")) {
            String t = p.text().trim();
            if (t.length() > 50 && !t.contains("相关推荐")) {
                if (sb.length() > 0) sb.append("\n\n");
                sb.append(t);
                if (sb.length() >= SUMMARY_MAX) break;
            }
        }
        return sb.length() > 0 ? sb.toString() : null;
    }

    private LocalDate extractDetailDate(Document doc) {
        String text = doc.body().text();
        Matcher matcher = DATE_PATTERN.matcher(text);
        if (matcher.find()) {
            try {
                int y = Integer.parseInt(matcher.group(1));
                int mo = Integer.parseInt(matcher.group(2));
                int d = Integer.parseInt(matcher.group(3));
                return LocalDate.of(y, mo, d);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private Element findDailySection(Document doc) {
        for (Element el : doc.select("h2, h3, h4, [class*='title'], [class*='header'], [class*='daily']")) {
            if (el.text().contains("最新AI日报")) {
                Element section = el.parent();
                for (int i = 0; i < 3 && section != null; i++) {
                    if (!section.select("a[href*='/zh/news/']").isEmpty()) {
                        return section;
                    }
                    section = section.parent();
                }
                return el.parent();
            }
        }
        return null;
    }

    private String cleanTitle(String raw) {
        String s = raw.trim();
        s = s.replaceFirst("^#\\d+\\s*", "");
        Matcher m = TITLE_SUFFIX_PATTERN.matcher(s);
        if (m.find()) {
            return s.substring(0, m.start()).trim();
        }
        return s;
    }

    private LocalDate parsePublishDate(String rawTitle) {
        if (rawTitle.contains("前天") || rawTitle.matches(".*2\\s*天前.*")) {
            return LocalDate.now().minusDays(2);
        }
        if (rawTitle.contains("昨天") || rawTitle.matches(".*1\\s*天前.*")) {
            return LocalDate.now().minusDays(1);
        }
        if (rawTitle.matches(".*\\d+\\s*天前.*")) {
            Matcher m = Pattern.compile("(\\d+)\\s*天前").matcher(rawTitle);
            if (m.find()) {
                int days = Math.min(30, Math.max(1, Integer.parseInt(m.group(1))));
                return LocalDate.now().minusDays(days);
            }
        }
        return LocalDate.now();
    }

}
