package com.example.platform.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class GitHubTrendingScheduler {

    private static final Logger log = LoggerFactory.getLogger(GitHubTrendingScheduler.class);

    private final GitHubTrendingService gitHubTrendingService;

    public GitHubTrendingScheduler(GitHubTrendingService gitHubTrendingService) {
        this.gitHubTrendingService = gitHubTrendingService;
    }

    @Scheduled(cron = "0 0 8 * * *", zone = "Asia/Shanghai")
    public void syncDailyTrending() {
        try {
            gitHubTrendingService.startSyncAsync();
        } catch (GitHubTrendingService.SyncAlreadyRunningException ex) {
            log.info("Scheduled GitHub trending sync skipped because another sync is already running");
        } catch (Exception ex) {
            log.error("Scheduled GitHub trending sync failed", ex);
        }
    }
}
