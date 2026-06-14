package com.example.platform.service;

import com.example.platform.entity.GitHubTrendingEntry;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import java.io.IOException;
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

    public record TrendingRow(
            GitHubTrendingEntry.Period period,
            int rank,
            String repoFullName,
            String repoUrl,
            String description,
            String language,
            Integer stars,
            Integer forks,
            Integer starsGained
    ) {}

    public String buildTrendingUrl(GitHubTrendingEntry.Period period, String languageFilter) {
        String since = period == GitHubTrendingEntry.Period.MONTHLY ? "monthly" : "weekly";
        String languagePath = normalizeLanguagePath(languageFilter);
        return BASE_URL + "/trending" + languagePath + "?since=" + since;
    }

    public List<TrendingRow> fetch(GitHubTrendingEntry.Period period, String languageFilter) throws IOException {
        String url = buildTrendingUrl(period, languageFilter);
        Document document = Jsoup.connect(url).get();
        return parseTrendingHtml(document.html(), period);
    }

    public List<TrendingRow> parseTrendingHtml(String html, GitHubTrendingEntry.Period period) {
        Document document = Jsoup.parse(html, BASE_URL);
        List<TrendingRow> rows = new ArrayList<>();
        int rank = 1;
        for (Element article : document.select("article.Box-row")) {
            TrendingRow row = parseArticle(article, period, rank);
            if (row != null) {
                rows.add(row);
                rank++;
            }
        }
        return rows;
    }

    private TrendingRow parseArticle(Element article, GitHubTrendingEntry.Period period, int rank) {
        Element repoLink = article.selectFirst("h2 a[href]");
        if (repoLink == null) {
            return null;
        }

        String repoFullName = normalizeRepoFullName(repoLink);
        if (repoFullName == null || repoFullName.isBlank()) {
            return null;
        }

        return new TrendingRow(
                period,
                rank,
                repoFullName,
                BASE_URL + "/" + repoFullName,
                textOrNull(article.selectFirst("p")),
                textOrNull(article.selectFirst("[itemprop=programmingLanguage]")),
                parseRepositoryCount(article, "/stargazers"),
                parseRepositoryCount(article, "/forks"),
                parseStarsGained(article.text())
        );
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
