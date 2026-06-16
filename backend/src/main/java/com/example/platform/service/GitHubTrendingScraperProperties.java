package com.example.platform.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.scraper.github-trending")
public record GitHubTrendingScraperProperties(
        int timeoutMillis,
        int retries,
        String proxyHost,
        Integer proxyPort
) {

    public GitHubTrendingScraperProperties {
        if (timeoutMillis <= 0) {
            timeoutMillis = 30_000;
        }
        if (retries <= 0) {
            retries = 2;
        }
        if (proxyHost != null && proxyHost.isBlank()) {
            proxyHost = null;
        }
        if (proxyHost == null || proxyPort == null || proxyPort <= 0) {
            proxyHost = null;
            proxyPort = null;
        }
    }
}
