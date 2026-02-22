package com.example.platform.service;

import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter;
import com.example.platform.entity.McpServer;
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

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 爬取 MCP 服务器数据，来源 ai.codefather.cn/mcp
 */
@Service
public class McpScraperService {

    private static final Logger log = LoggerFactory.getLogger(McpScraperService.class);
    private static final String TARGET_URL = "https://ai.codefather.cn/mcp";
    private static final String BASE_URL = "https://ai.codefather.cn";
    private static final int NAME_MAX = 200;
    private static final int DESC_MAX = 50000;
    private static final int URL_MAX = 500;
    private static final Pattern NEXT_IMAGE_URL = Pattern.compile("url=([^&]+)");

    private final McpServerService mcpServerService;

    @Value("${app.scraper.mcp.enabled:true}")
    private boolean enabled;

    @Value("${app.scraper.mcp.headless:true}")
    private boolean headless;

    @Value("${app.scraper.mcp.max-items:0}")
    private int maxItems;

    @Value("${app.scraper.mcp.max-pages:232}")
    private int maxPages;

    private volatile ScrapeStatus lastStatus = new ScrapeStatus("idle", null, null, null);

    public McpScraperService(McpServerService mcpServerService) {
        this.mcpServerService = mcpServerService;
    }

    @Async("scraperTaskExecutor")
    public void doScrapeAsync() {
        lastStatus = new ScrapeStatus("running", null, null, null);
        try {
            int added = doScrape();
            lastStatus = new ScrapeStatus("completed", added, null, Instant.now());
            log.info("Async MCP scrape completed, added {} items", added);
        } catch (Exception e) {
            lastStatus = new ScrapeStatus("failed", -1, e.getMessage(), Instant.now());
            log.error("Async MCP scrape failed", e);
        }
    }

    public ScrapeStatus getLastStatus() {
        return lastStatus;
    }

    public record ScrapeStatus(String status, Integer added, String error, Instant finishedAt) {}

    public int doScrape() throws Exception {
        if (!enabled) {
            log.debug("MCP scraper is disabled");
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
            Set<String> seenUrls = new HashSet<>();
            int totalAdded = 0;

            for (int page = 1; page <= maxPages; page++) {
                if (maxItems > 0 && totalAdded >= maxItems) break;

                String listUrl = TARGET_URL + "?current=" + page;
                log.info("MCP scrape page {}/{}: {}", page, maxPages, listUrl);

                driver.get(listUrl);
                new WebDriverWait(driver, Duration.ofSeconds(20))
                        .until(d -> d.getPageSource().length() > 5000);

                Thread.sleep(2000);

                for (int i = 0; i < 3; i++) {
                    ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("window.scrollTo(0, document.body.scrollHeight);");
                    Thread.sleep(500);
                }

                String pageSource = driver.getPageSource();
                Document doc = Jsoup.parse(pageSource, listUrl);
                int added = processDocument(driver, doc, seenUrls);
                totalAdded += added;

                if (added == 0 && page > 1) {
                    log.info("No new cards on page {}, may have reached end", page);
                    break;
                }

                log.info("Page {}: added {}, total {}", page, added, totalAdded);

                if (page < maxPages) Thread.sleep(1000);
            }

            log.info("MCP scrape completed, total added {} items", totalAdded);
            return totalAdded;
        } finally {
            driver.quit();
        }
    }

    private static final String CARD_SELECTOR = "a.flex.bg-white[href^='/mcp/']";
    private static final String CARD_FALLBACK = "a[href^='/mcp/'][class*='flex'][class*='bg-white']";

    private int processDocument(WebDriver driver, Document listDoc, Set<String> seenUrls) {
        int added = 0;

        Elements cards = listDoc.select(CARD_SELECTOR);
        if (cards.isEmpty()) {
            cards = listDoc.select(CARD_FALLBACK);
        }
        if (cards.isEmpty()) {
            cards = listDoc.select("a[href^='/mcp/']");
        }
        log.info("Found {} MCP cards", cards.size());

        if (cards.isEmpty()) return 0;

        Elements cardsToProcess = maxItems > 0 ? new Elements(cards.subList(0, Math.min(maxItems, cards.size()))) : cards;

        for (Element card : cardsToProcess) {
            String href = card.attr("href");
            if (href == null || href.isBlank() || !href.startsWith("/mcp/")) continue;

            String detailUrl = href.startsWith("http") ? href : BASE_URL + (href.startsWith("/") ? "" : "/") + href;
            if (detailUrl.length() > URL_MAX) continue;
            if (seenUrls.contains(detailUrl)) continue;

            String name = extractName(card);
            if (name == null || name.isBlank() || name.length() < 2) continue;
            if (name.length() > NAME_MAX) name = name.substring(0, NAME_MAX);
            if (mcpServerService.existsByName(name)) {
                log.debug("Skip duplicate MCP by name: {}", name);
                continue;
            }

            String cardDesc = extractDescription(card);
            String logoUrl = extractLogoUrl(card);

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
                if (briefEl == null) briefEl = detailDoc.selectFirst("[class*='mt-2'][class*='text-slate-600']");
                if (briefEl == null) briefEl = detailDoc.selectFirst("[class*='text-slate-600']");
                if (briefEl != null) briefDesc = briefEl.text().trim();

                Element markdownEl = detailDoc.selectFirst(".markdown-body");
                if (markdownEl != null) {
                    String html = markdownEl.html();
                    detailedContent = htmlToMarkdown(html);
                }

                officialUrl = extractOfficialUrl(detailDoc);
                tagNames = extractTagNames(detailDoc);
            } catch (Exception e) {
                log.warn("Failed to load MCP detail page {}: {}", detailUrl, e.getMessage());
            }

            String summary = briefDesc != null && !briefDesc.isBlank() ? briefDesc : (cardDesc != null ? cardDesc : null);
            String content = detailedContent != null && !detailedContent.isBlank() ? detailedContent : null;
            if (content != null && content.length() > DESC_MAX) content = content.substring(0, DESC_MAX);

            String toolUrl = (officialUrl != null && !officialUrl.isBlank() && officialUrl.length() <= URL_MAX)
                    ? officialUrl : detailUrl;

            try {
                mcpServerService.create(name, summary, content, toolUrl, logoUrl, tagNames);
                seenUrls.add(detailUrl);
                added++;
                log.info("Added MCP: name={}, url={}", name, toolUrl);
            } catch (Exception e) {
                log.warn("Failed to save MCP {}: {}", name, e.getMessage());
            }
            if (maxItems > 0 && added >= maxItems) break;
        }
        return added;
    }

    private String extractOfficialUrl(Document detailDoc) {
        for (Element a : detailDoc.select("a[href]")) {
            String text = a.text().trim();
            if ("立即体验".equals(text) || "访问官网".equals(text) || "GitHub".equals(text)) {
                String href = a.attr("abs:href");
                if (href != null && !href.isBlank() && !href.startsWith("javascript:") && href.length() <= URL_MAX) {
                    return href;
                }
            }
        }
        return null;
    }

    private String extractTagNames(Document detailDoc) {
        // 优先匹配详情页标签容器: mt-2 sm:mt-auto pt-2 sm:pt-3 flex flex-wrap items-center gap-2
        Element container = detailDoc.selectFirst(".mt-2.sm\\:mt-auto.pt-2.sm\\:pt-3.flex.flex-wrap.items-center.gap-2");
        if (container == null) container = detailDoc.selectFirst("[class*='sm:mt-auto'][class*='flex-wrap'][class*='gap-2']");
        if (container == null) container = detailDoc.selectFirst("[class*='mt-2'][class*='pt-2'][class*='flex-wrap'][class*='gap-2']");
        if (container == null) container = detailDoc.selectFirst("[class*='flex-wrap'][class*='gap-2']");
        if (container == null) container = detailDoc.selectFirst("[class*='pt-2'][class*='flex']");
        if (container == null) return null;
        List<String> tags = new ArrayList<>();
        for (Element el : container.select("a, span, [class*='rounded']")) {
            String text = el.ownText();
            if (text.isBlank()) text = el.text().trim();
            if (!text.isBlank() && !"立即体验".equals(text) && !"访问官网".equals(text) && !"GitHub".equals(text) && text.length() <= 50) {
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
        for (String sel : new String[]{"h3", "h4", "[class*='font-medium']", "[class*='font-semibold']"}) {
            Element el = card.selectFirst(sel);
            if (el != null) {
                String t = el.text().trim();
                if (t.length() >= 2 && t.length() <= 150) return t;
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
        for (String sel : new String[]{"[class*='text-gray']", "[class*='text-slate']", "p"}) {
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

    private String htmlToMarkdown(String html) {
        if (html == null || html.isBlank()) return null;
        try {
            return FlexmarkHtmlConverter.builder().build().convert(html).trim();
        } catch (Exception e) {
            log.debug("HTML to Markdown conversion failed, falling back to plain text: {}", e.getMessage());
            return Jsoup.parse(html).text().trim();
        }
    }
}
