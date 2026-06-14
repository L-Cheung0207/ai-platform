package com.example.platform.service;

import com.example.platform.entity.GitHubTrendingEntry;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GitHubTrendingScraperService {

    private static final String BASE_URL = "https://github.com";
    private static final Pattern STARS_GAINED_PATTERN = Pattern.compile("([\\d,.]+\\s*[kKmM]?)\\s+stars?\\s+this\\s+(week|month)");

    public String buildTrendingUrl(GitHubTrendingEntry.Period period, String languageFilter) {
        String since = period == GitHubTrendingEntry.Period.MONTHLY ? "monthly" : "weekly";
        String languagePath = normalizeLanguagePath(languageFilter);
        return BASE_URL + "/trending" + languagePath + "?since=" + since;
    }

    public List<GitHubTrendingEntry> parseTrendingHtml(String html, GitHubTrendingEntry.Period period) {
        Document document = Jsoup.parse(html, BASE_URL);
        List<GitHubTrendingEntry> entries = new ArrayList<>();
        int rank = 1;
        for (Element article : document.select("article.Box-row")) {
            GitHubTrendingEntry entry = parseArticle(article, period, rank);
            if (entry != null) {
                entries.add(entry);
                rank++;
            }
        }
        return entries;
    }

    private GitHubTrendingEntry parseArticle(Element article, GitHubTrendingEntry.Period period, int rank) {
        Element repoLink = article.selectFirst("h2 a[href]");
        if (repoLink == null) {
            return null;
        }

        String repoFullName = normalizeRepoFullName(repoLink);
        if (repoFullName == null || repoFullName.isBlank()) {
            return null;
        }

        GitHubTrendingEntry entry = new GitHubTrendingEntry();
        entry.setPeriod(period);
        entry.setRank(rank);
        entry.setRepoFullName(repoFullName);
        entry.setRepoUrl(BASE_URL + "/" + repoFullName);
        entry.setDescription(textOrNull(article.selectFirst("p")));
        entry.setLanguage(textOrNull(article.selectFirst("[itemprop=programmingLanguage]")));
        entry.setStars(parseRepositoryCount(article, "/stargazers"));
        entry.setForks(parseRepositoryCount(article, "/forks"));
        entry.setStarsGained(parseStarsGained(article.text()));
        return entry;
    }

    private String normalizeLanguagePath(String languageFilter) {
        if (languageFilter == null || languageFilter.isBlank()) {
            return "";
        }
        String slug = languageFilter.trim().toLowerCase(Locale.ROOT);
        return "/" + URLEncoder.encode(slug, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private String normalizeRepoFullName(Element repoLink) {
        String href = repoLink.attr("href").trim();
        if (href.startsWith("/")) {
            href = href.substring(1);
        }
        if (href.split("/").length >= 2) {
            String[] parts = href.split("/");
            return parts[0] + "/" + parts[1];
        }
        return repoLink.text().replaceAll("\\s*/\\s*", "/").replaceAll("\\s+", "").trim();
    }

    private String textOrNull(Element element) {
        if (element == null) {
            return null;
        }
        String text = element.text().trim();
        return text.isBlank() ? null : text;
    }

    private Integer parseRepositoryCount(Element article, String hrefSuffix) {
        for (Element link : article.select("a[href$=" + hrefSuffix + "]")) {
            Integer count = parseCompactNumber(link.text());
            if (count != null) {
                return count;
            }
        }
        return null;
    }

    private Integer parseStarsGained(String text) {
        Matcher matcher = STARS_GAINED_PATTERN.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        return parseCompactNumber(matcher.group(1));
    }

    private Integer parseCompactNumber(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().replace(",", "").replaceAll("\\s+", "");
        if (normalized.isBlank()) {
            return null;
        }
        double multiplier = 1;
        char last = normalized.charAt(normalized.length() - 1);
        if (last == 'k' || last == 'K') {
            multiplier = 1_000;
            normalized = normalized.substring(0, normalized.length() - 1);
        } else if (last == 'm' || last == 'M') {
            multiplier = 1_000_000;
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        try {
            return (int) Math.round(Double.parseDouble(normalized) * multiplier);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
