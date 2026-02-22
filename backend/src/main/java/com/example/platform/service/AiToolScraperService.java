package com.example.platform.service;

import com.example.platform.entity.AiTool;
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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 爬取 AI 工具数据，并下载存储 logo。
 */
@Service
public class AiToolScraperService {

    private static final Logger log = LoggerFactory.getLogger(AiToolScraperService.class);
    private static final String TARGET_URL = "https://ai.codefather.cn/tool";
    private static final String SOURCE_NAME = "鱼皮AI导航";
    private static final int NAME_MAX = 200;
    private static final int DESC_MAX = 50000;
    private static final int URL_MAX = 500;
    private static final String BASE_URL = "https://ai.codefather.cn";
    private static final Pattern NEXT_IMAGE_URL = Pattern.compile("url=([^&]+)");

    private final AiToolService aiToolService;

    @Value("${app.upload.dir:./upload}")
    private String uploadDir;

    @Value("${app.scraper.ai-tools.enabled:true}")
    private boolean enabled;

    @Value("${app.scraper.ai-tools.headless:true}")
    private boolean headless;

    @Value("${app.scraper.ai-tools.max-items:0}")
    private int maxItems;

    private volatile ScrapeStatus lastStatus = new ScrapeStatus("idle", null, null, null);

    public AiToolScraperService(AiToolService aiToolService) {
        this.aiToolService = aiToolService;
    }

    @Async("scraperTaskExecutor")
    public void doScrapeAsync() {
        lastStatus = new ScrapeStatus("running", null, null, null);
        try {
            int added = doScrape();
            lastStatus = new ScrapeStatus("completed", added, null, Instant.now());
            log.info("Async AI tool scrape completed, added {} items", added);
        } catch (Exception e) {
            lastStatus = new ScrapeStatus("failed", -1, e.getMessage(), Instant.now());
            log.error("Async AI tool scrape failed", e);
        }
    }

    public ScrapeStatus getLastStatus() {
        return lastStatus;
    }

    public record ScrapeStatus(String status, Integer added, String error, Instant finishedAt) {}

    public int doScrape() throws Exception {
        if (!enabled) {
            log.debug("AI tool scraper is disabled");
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
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
            driver.get(TARGET_URL);
            new WebDriverWait(driver, Duration.ofSeconds(20))
                    .until(d -> d.getPageSource().length() > 5000);

            Thread.sleep(3000);

            for (int i = 0; i < 3; i++) {
                ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("window.scrollTo(0, document.body.scrollHeight);");
                Thread.sleep(800);
            }

            String pageSource = driver.getPageSource();
            Document doc = Jsoup.parse(pageSource, TARGET_URL);
            int added = processDocument(driver, doc);
            log.info("AI tool scrape completed, added {} items", added);
            return added;
        } finally {
            driver.quit();
        }
    }

    /** 工具卡片：a.flex.bg-white.shadow-sm，href 为 /tool/xxx，含 img、h5、sub */
    private static final String CARD_SELECTOR = "a.flex.bg-white[href^='/tool/']";

    private int processDocument(WebDriver driver, Document listDoc) {
        Set<String> seenUrls = new HashSet<>();
        int added = 0;

        Elements cards = listDoc.select(CARD_SELECTOR);
        log.info("Found {} tool cards with selector {}", cards.size(), CARD_SELECTOR);

        if (cards.isEmpty()) {
            Elements fallback = listDoc.select("a[href^='/tool/'][class*='flex'][class*='bg-white']");
            if (!fallback.isEmpty()) {
                cards = fallback;
                log.info("Fallback selector found {} cards", cards.size());
            }
        }
        if (cards.isEmpty()) {
            log.warn("No tool cards found");
            return 0;
        }

        Elements cardsToProcess = cards;
        if (maxItems > 0) {
            cardsToProcess = new Elements(cards.subList(0, Math.min(1, cards.size())));
            log.info("max-items={}, processing only first card", maxItems);
        }

        for (Element card : cardsToProcess) {
            String href = card.attr("href");
            if (href == null || href.isBlank() || !href.startsWith("/tool/")) continue;

            String detailUrl = href.startsWith("http") ? href : BASE_URL + (href.startsWith("/") ? "" : "/") + href;
            if (detailUrl.length() > URL_MAX) continue;
            if (seenUrls.contains(detailUrl)) continue;
            if (aiToolService.existsBySourceUrl(detailUrl)) continue;

            String name = extractName(card);
            if (name == null || name.isBlank() || name.length() < 2) continue;
            if (name.length() > NAME_MAX) name = name.substring(0, NAME_MAX);

            String cardDesc = extractDescription(card);
            String logoUrl = extractLogoUrl(card);
            String logoPath = null;
            if (logoUrl != null && !logoUrl.isBlank() && !logoUrl.startsWith("data:")) {
                try {
                    logoPath = downloadLogo(logoUrl, seenUrls.size() + added);
                } catch (Exception e) {
                    log.debug("Could not download logo {}: {}", logoUrl, e.getMessage());
                }
            }

            String category = extractCategory(card);

            String briefDesc = null;
            String detailedContent = null;
            String officialUrl = null;
            String tagNames = null;
            try {
                driver.get(detailUrl);
                new WebDriverWait(driver, Duration.ofSeconds(15))
                        .until(d -> d.getPageSource().length() > 3000);
                Thread.sleep(800);

                Document detailDoc = Jsoup.parse(driver.getPageSource(), detailUrl);

                Element briefEl = detailDoc.selectFirst(".mt-2.text-sm.text-slate-600");
                if (briefEl == null) {
                    briefEl = detailDoc.selectFirst("[class*='mt-2'][class*='text-slate-600']");
                }
                if (briefEl == null) {
                    briefEl = detailDoc.selectFirst("[class*='text-slate-600'][class*='text-sm']");
                }
                if (briefEl != null) briefDesc = briefEl.text().trim();

                Element markdownEl = detailDoc.selectFirst(".markdown-body");
                if (markdownEl != null) detailedContent = markdownEl.text().trim();

                officialUrl = extractOfficialUrl(detailDoc);
                tagNames = extractTagNames(detailDoc);
            } catch (Exception e) {
                log.warn("Failed to load detail page {}: {}", detailUrl, e.getMessage());
            }

            String summary = briefDesc != null && !briefDesc.isBlank() ? briefDesc : (cardDesc != null ? cardDesc : null);
            String content = detailedContent != null && !detailedContent.isBlank() ? detailedContent : null;
            if (content != null && content.length() > DESC_MAX) content = content.substring(0, DESC_MAX);

            String toolUrl = (officialUrl != null && !officialUrl.isBlank() && officialUrl.length() <= URL_MAX)
                    ? officialUrl : detailUrl;

            try {
                aiToolService.create(name, summary, content, toolUrl, logoUrl, logoPath, category, tagNames, detailUrl);
                seenUrls.add(detailUrl);
                added++;
                log.info("Added tool: name={}, url={}, tags={}", name, toolUrl, tagNames);
            } catch (Exception e) {
                log.warn("Failed to save tool {}: {}", name, e.getMessage());
            }
            if (maxItems > 0 && added >= maxItems) {
                log.info("Reached max-items limit ({})", maxItems);
                break;
            }
        }
        return added;
    }

    /**
     * 从详情页提取「访问官网」链接的 href 作为工具官网 URL。
     */
    private String extractOfficialUrl(Document detailDoc) {
        for (Element a : detailDoc.select("a[href]")) {
            if ("访问官网".equals(a.text().trim())) {
                String href = a.attr("abs:href");
                if (href != null && !href.isBlank() && !href.startsWith("javascript:")) {
                    return href;
                }
            }
        }
        return null;
    }

    /**
     * 从详情页提取标签：容器 class 含 mt-2 sm:mt-auto pt-2 sm:pt-3 flex flex-wrap items-center gap-2
     * 返回逗号分隔的标签文本，如 "AI写作工具,AI写作"
     */
    private String extractTagNames(Document detailDoc) {
        Element container = detailDoc.selectFirst("[class*='flex-wrap'][class*='gap-2']");
        if (container == null) {
            container = detailDoc.selectFirst("[class*='pt-2'][class*='pt-3'][class*='flex']");
        }
        if (container == null) return null;
        List<String> tags = new ArrayList<>();
        for (Element el : container.select("a, span")) {
            String text = el.text().trim();
            if (!text.isBlank() && !"访问官网".equals(text) && text.length() <= 50) {
                tags.add(text);
            }
        }
        return tags.isEmpty() ? null : String.join(",", tags);
    }

    private String extractName(Element card) {
        Element h5 = card.selectFirst("h5");
        if (h5 != null) {
            String t = h5.text().trim();
            if (!t.isBlank()) return t;
        }
        Element img = card.selectFirst("img");
        if (img != null) {
            String alt = img.attr("alt");
            if (alt != null && !alt.isBlank()) return alt.trim();
        }
        for (String sel : new String[]{".tool-card-title", "h3", "h4", "[class*='font-medium']"}) {
            Element el = card.selectFirst(sel);
            if (el != null) {
                String t = el.text().trim();
                if (t.length() >= 2 && t.length() <= 150 && !t.equals("全部")) return t;
            }
        }
        return null;
    }

    private String extractDescription(Element card) {
        Element sub = card.selectFirst("sub");
        if (sub != null) {
            String t = sub.text().trim();
            if (t.length() > 5 && t.length() < 500) return t;
        }
        for (String sel : new String[]{".tool-card-desc", "[class*='text-gray']", "p"}) {
            Element el = card.selectFirst(sel);
            if (el != null) {
                String t = el.text().trim();
                if (t.length() > 10 && t.length() < 500) return t;
            }
        }
        return null;
    }

    private String extractLogoUrl(Element card) {
        Element img = card.selectFirst("img");
        if (img == null) return null;
        String src = img.attr("src");
        if (src == null || src.isBlank() || src.startsWith("data:")) return null;
        if (src.contains("url=")) {
            var m = NEXT_IMAGE_URL.matcher(src);
            if (m.find()) {
                try {
                    return java.net.URLDecoder.decode(m.group(1), java.nio.charset.StandardCharsets.UTF_8);
                } catch (Exception e) {
                    log.debug("Could not decode image URL: {}", e.getMessage());
                }
            }
        }
        return img.attr("abs:src");
    }

    private String extractCategory(Element card) {
        Element section = card;
        for (int i = 0; i < 10 && section != null; i++) {
            Element h2 = section.selectFirst("h2, h3");
            if (h2 != null) {
                String cat = h2.text().trim();
                if (cat.length() <= 50 && !cat.contains("http")) return cat;
            }
            section = section.parent();
        }
        return "AI工具";
    }

    private String normalizeUrl(String url) {
        try {
            return URI.create(url).normalize().toString();
        } catch (Exception e) {
            return url;
        }
    }

    private String downloadLogo(String logoUrl, int index) throws IOException {
        Path dir = Paths.get(uploadDir, "ai-tools");
        Files.createDirectories(dir);

        String ext = "png";
        if (logoUrl.toLowerCase().contains(".jpg") || logoUrl.toLowerCase().contains(".jpeg")) ext = "jpg";
        else if (logoUrl.toLowerCase().contains(".webp")) ext = "webp";
        else if (logoUrl.toLowerCase().contains(".gif")) ext = "gif";
        else if (logoUrl.toLowerCase().contains(".svg")) ext = "png";

        String filename = "logo_" + index + "_" + System.currentTimeMillis() + "." + ext;
        Path file = dir.resolve(filename);

        try (InputStream in = URI.create(logoUrl).toURL().openStream()) {
            Files.copy(in, file);
        }
        return "ai-tools/" + filename;
    }
}
