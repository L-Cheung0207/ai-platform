package com.example.platform.dto;

import com.example.platform.entity.GitHubTrendingConfig;

import java.time.Instant;

public record GitHubTrendingStatusDto(
        GitHubTrendingConfig.SyncStatus status,
        String error,
        Instant startedAt,
        Instant finishedAt,
        Instant latestWeeklyBatch,
        Instant latestMonthlyBatch
) {
    public static GitHubTrendingStatusDto fromConfig(GitHubTrendingConfig config) {
        return new GitHubTrendingStatusDto(
                config.getLastSyncStatus(),
                config.getLastSyncError(),
                config.getLastSyncStartedAt(),
                config.getLastSyncFinishedAt(),
                config.getLatestWeeklyBatch(),
                config.getLatestMonthlyBatch()
        );
    }
}
