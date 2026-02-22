package com.example.platform.service;

import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter;
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

/**
 * 爬取外部 Skill 数据，来源 ai.codefather.cn/skills
 * 详情页提取「访问 GitHub」链接作为 sourceUrl
 */
@Service
public class ExternalSkillScraperService {

    private static final Logger log = LoggerFactory.getLogger(ExternalSkillScraperService.class);
    private static final String TARGET_URL = "https://ai.codefather.cn/skills";
    private static final String BASE_URL = "https://ai.codefather.cn";
    private static final int NAME_MAX = 200;
    private static final int DESC_MAX = 10000;
    private static final int CONTENT_MAX = 50000;
    private static final int URL_MAX = 500;
    private static final int INSTALL_CMD_MAX = 500;

    private final ExternalSkillService externalSkillService;

    @Value("${app.scraper.external-skills.enabled:true}")
    private boolean enabled;

    @Value("${app.scraper.external-skills.headless:true}")
    private boolean headless;

    @Value("${app.scraper.external-skills.max-items:0}")
    private int maxItems;

    @Value("${app.scraper.external-skills.max-pages:341}")
    private int maxPages;

    private volatile ScrapeStatus lastStatus = new ScrapeStatus("idle", null, null, null);

    public ExternalSkillScraperService(ExternalSkillService externalSkillService) {
        this.externalSkillService = externalSkillService;
    }

    @Async("scraperTaskExecutor")
    public void doScrapeAsync() {
        lastStatus = new ScrapeStatus("running", null, null, null);
        try {
            int added = doScrape();
            lastStatus = new ScrapeStatus("completed", added, null, Instant.now());
            log.info("Async ExternalSkill scrape completed, added {} items", added);
        } catch (Exception e) {
            lastStatus = new ScrapeStatus("failed", -1, e.getMessage(), Instant.now());
            log.error("Async ExternalSkill scrape failed", e);
        }
    }

    public ScrapeStatus getLastStatus() {
        return lastStatus;
    }

    public record ScrapeStatus(String status, Integer added, String error, Instant finishedAt) {}

    public int doScrape() throws Exception {
        if (!enabled) {
            log.debug("ExternalSkill scraper is disabled");
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
                log.info("ExternalSkill scrape page {}/{}: {}", page, maxPages, listUrl);

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
                int limit = maxItems > 0 ? Math.max(0, maxItems - totalAdded) : Integer.MAX_VALUE;
                int added = processDocument(driver, doc, seenUrls, limit);
                totalAdded += added;

                if (added == 0 && page > 1) {
                    log.info("No new cards on page {}, may have reached end", page);
                    break;
                }

                log.info("Page {}: added {}, total {}", page, added, totalAdded);

                if (page < maxPages) Thread.sleep(1000);
            }

            log.info("ExternalSkill scrape completed, total added {} items", totalAdded);
            return totalAdded;
        } finally {
            driver.quit();
        }
    }

    private static final String CARD_SELECTOR = "a[href^='/skills/']";

    private int processDocument(WebDriver driver, Document listDoc, Set<String> seenUrls, int maxToAdd) {
        int added = 0;

        Elements cards = listDoc.select(CARD_SELECTOR);
        log.info("Found {} skill cards", cards.size());

        if (cards.isEmpty()) return 0;

        int take = maxToAdd <= 0 ? cards.size() : Math.min(cards.size(), maxToAdd);
        Elements cardsToProcess = take <= 0 ? new Elements() : new Elements(cards.subList(0, take));

        for (Element card : cardsToProcess) {
            String href = card.attr("href");
            if (href == null || href.isBlank() || !href.startsWith("/skills/")) continue;

            String detailUrl = href.startsWith("http") ? href : BASE_URL + (href.startsWith("/") ? "" : "/") + href;
            if (detailUrl.length() > URL_MAX) continue;
            if (seenUrls.contains(detailUrl)) continue;

            String name = extractName(card);
            if (name == null || name.isBlank() || name.length() < 2) continue;
            if (name.length() > NAME_MAX) name = name.substring(0, NAME_MAX);
            if (externalSkillService.existsByName(name)) {
                log.debug("Skip duplicate ExternalSkill by name: {}", name);
                continue;
            }

            String cardDesc = extractDescription(card);

            String description = cardDesc;
            String content = null;
            String sourceUrl = null;
            String installCommand = null;
            List<String> tagNames = null;

            try {
                driver.get(detailUrl);
                new WebDriverWait(driver, Duration.ofSeconds(15))
                        .until(d -> d.getPageSource().length() > 3000);
                Thread.sleep(800);

                Document detailDoc = Jsoup.parse(driver.getPageSource(), detailUrl);

                sourceUrl = extractGitHubUrl(detailDoc);
                String detailDesc = extractDetailDescription(detailDoc);
                if (detailDesc != null && !detailDesc.isBlank()) {
                    description = detailDesc.length() > DESC_MAX ? detailDesc.substring(0, DESC_MAX) : detailDesc;
                } else if (description == null || description.isBlank()) {
                    description = "见来源链接";
                }
                content = extractDetailContent(detailDoc);
                if (content != null && content.length() > CONTENT_MAX) content = content.substring(0, CONTENT_MAX);
                installCommand = extractInstallCommand(detailDoc);
                tagNames = extractTagNames(detailDoc);
            } catch (Exception e) {
                log.warn("Failed to load ExternalSkill detail page {}: {}", detailUrl, e.getMessage());
            }

            if (description == null || description.isBlank()) description = "见来源链接";
            if (description.length() > DESC_MAX) description = description.substring(0, DESC_MAX);

            try {
                externalSkillService.createFromScraper(name, description, content, sourceUrl, installCommand, tagNames);
                seenUrls.add(detailUrl);
                added++;
                log.info("Added ExternalSkill: name={}, sourceUrl={}", name, sourceUrl);
            } catch (Exception e) {
                log.warn("Failed to save ExternalSkill {}: {}", name, e.getMessage());
            }

            if (maxItems > 0 && added >= maxItems) break;
        }
        return added;
    }

    /** 详情页提取「访问 GitHub」链接 */
    private String extractGitHubUrl(Document detailDoc) {
        for (Element a : detailDoc.select("a[href]")) {
            String text = a.text().trim();
            String href = a.attr("abs:href");
            if (href == null || href.isBlank() || href.startsWith("javascript:")) continue;
            if (href.length() > URL_MAX) continue;
            if ("访问 GitHub".equals(text) || text.contains("GitHub") && href.contains("github.com")) {
                return href;
            }
        }
        return null;
    }

    /** 详情页 description：从 mt-2 text-sm text-slate-600 sm:flex-1 sm:overflow-hidden 获取 */
    private String extractDetailDescription(Document detailDoc) {
        Element descEl = detailDoc.selectFirst("[class*='mt-2'][class*='text-sm'][class*='text-slate-600'][class*='sm:flex-1'][class*='sm:overflow-hidden']");
        if (descEl == null) descEl = detailDoc.selectFirst("[class*='text-slate-600'][class*='sm:overflow-hidden']");
        if (descEl == null) descEl = detailDoc.selectFirst("[class*='mt-2'][class*='text-slate-600']");
        if (descEl != null) {
            String t = descEl.text().trim();
            if (t.length() > 10) return t;
        }
        Element brief = detailDoc.selectFirst(".mt-2.text-sm.text-slate-600");
        if (brief == null) brief = detailDoc.selectFirst("[class*='text-slate-600']");
        if (brief == null) brief = detailDoc.selectFirst("[class*='description']");
        if (brief != null) return brief.text().trim();
        return null;
    }

    /** 详情页 content：从 relative rounded-sm 取 HTML 转成 Markdown */
    private String extractDetailContent(Document detailDoc) {
        Element relativeRounded = detailDoc.selectFirst("[class*='relative'][class*='rounded-sm']");
        if (relativeRounded == null) return null;
        String html = relativeRounded.html();
        if (html == null || html.isBlank()) return null;
        String md = htmlToMarkdown(html);
        return md != null && md.trim().length() > 20 ? md.trim() : null;
    }

    private String htmlToMarkdown(String html) {
        if (html == null || html.isBlank()) return null;
        try {
            String md = FlexmarkHtmlConverter.builder().build().convert(html).trim();
            return fixMarkdownTableLeading(md);
        } catch (Exception e) {
            log.debug("HTML to Markdown failed, using plain text: {}", e.getMessage());
            return Jsoup.parse(html).text().trim();
        }
    }

    /** 纠正表格开头：首行若为畸形分隔行（仅 | - 空格），去掉并在第二行前补标准表头与分隔行，使呈「字段|值」两列表格 */
    private String fixMarkdownTableLeading(String md) {
        if (md == null || !md.contains("|")) return md;
        String[] lines = md.split("\n");
        if (lines.length == 0) return md;
        String first = lines[0].trim();
        if (!first.startsWith("|") || !first.endsWith("|")) return md;
        String rest = first.replace("|", "").replace("-", "").replace(" ", "").replace("\t", "");
        if (!rest.isEmpty()) return md;
        StringBuilder out = new StringBuilder();
        out.append("| 字段 | 值 |\n");
        out.append("| --- | --- |\n");
        for (int i = 1; i < lines.length; i++) {
            out.append(lines[i]).append("\n");
        }
        return out.toString().trim();
    }

    /** 详情页「快捷安装」下的安装命令：从 border rounded-xl ... bg-gray-50 区块获取，子元素（如 span）文本用空格拼接 */
    private String extractInstallCommand(Document detailDoc) {
        Element installBlock = detailDoc.selectFirst("[class*='rounded-xl'][class*='bg-gray-50'][class*='flex'][class*='items-center']");
        if (installBlock != null) {
            Element code = installBlock.selectFirst("pre, code");
            if (code != null) {
                String text = code.text().trim();
                if (text.contains("npx") && (text.contains("add-skill") || text.contains("skill"))) {
                    return normalizeInstallCommand(text);
                }
            }
            Element cmdContainer = installBlock.selectFirst("[class*='font-mono']");
            if (cmdContainer != null) {
                List<String> parts = new ArrayList<>();
                for (Element child : cmdContainer.children()) {
                    String t = child.text().trim();
                    if (!t.isBlank()) parts.add(t);
                }
                if (!parts.isEmpty()) {
                    String cmd = String.join(" ", parts);
                    if (cmd.contains("npx") && (cmd.contains("add-skill") || cmd.contains("skill"))) {
                        return normalizeInstallCommand(cmd);
                    }
                }
            }
            String text = installBlock.text().trim();
            if (text.contains("npx") && (text.contains("add-skill") || text.contains("skill"))) {
                return normalizeInstallCommand(text);
            }
        }
        for (Element block : detailDoc.select("pre, code, [class*='copy']")) {
            String text = block.ownText();
            if (text.isBlank()) text = block.text().trim();
            if (text.contains("npx") && (text.contains("add-skill") || text.contains("skill"))) {
                return normalizeInstallCommand(text);
            }
        }
        return null;
    }

    private String normalizeInstallCommand(String raw) {
        String cmd = raw.replaceAll("\\s+", " ").trim();
        if (cmd.length() > INSTALL_CMD_MAX) cmd = cmd.substring(0, INSTALL_CMD_MAX);
        return cmd;
    }

    /** 详情页标签：优先 Ant Design .ant-tag，兼容原有 flex/gap 容器 */
    private List<String> extractTagNames(Document detailDoc) {
        List<String> tags = new ArrayList<>();
        for (Element tag : detailDoc.select(".ant-tag")) {
            String text = tag.ownText();
            if (text.isBlank()) text = tag.text().trim();
            if (!text.isBlank() && !"访问 GitHub".equals(text) && text.length() <= 50) {
                tags.add(text);
            }
        }
        if (!tags.isEmpty()) return tags;
        Element container = detailDoc.selectFirst("[class*='flex-wrap'][class*='gap-2']");
        if (container == null) container = detailDoc.selectFirst("[class*='flex'][class*='gap-']");
        if (container == null) return null;
        for (Element el : container.select("a, span, [class*='tag']")) {
            String text = el.ownText();
            if (text.isBlank()) text = el.text().trim();
            if (!text.isBlank() && !"访问 GitHub".equals(text) && text.length() <= 50) {
                tags.add(text);
            }
        }
        return tags.isEmpty() ? null : tags;
    }

    private String extractName(Element card) {
        Element h5 = card.selectFirst("h5");
        if (h5 != null) {
            String t = h5.text().trim();
            if (!t.isBlank()) return t;
        }
        for (String sel : new String[]{"h3", "h4", "[class*='font-medium']", "[class*='font-semibold']", "[class*='item-card-name']"}) {
            Element el = card.selectFirst(sel);
            if (el != null) {
                String t = el.text().trim();
                if (t.length() >= 2 && t.length() <= 150) return t;
            }
        }
        return null;
    }

    private String extractDescription(Element card) {
        for (String sel : new String[]{"[class*='text-gray']", "[class*='text-slate']", "p", "[class*='item-card-desc']"}) {
            Element el = card.selectFirst(sel);
            if (el != null) {
                String t = el.text().trim();
                if (t.length() > 10 && t.length() < 2000) return t;
            }
        }
        return null;
    }
}
