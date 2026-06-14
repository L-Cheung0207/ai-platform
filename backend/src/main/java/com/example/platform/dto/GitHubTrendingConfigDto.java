package com.example.platform.dto;

import com.example.platform.entity.GitHubTrendingConfig;

import java.time.Instant;

public record GitHubTrendingConfigDto(
        Long id,
        String languageFilter,
        String keywordFilter,
        int homeDisplayCount,
        String refreshCron,
        GitHubTrendingConfig.SyncStatus lastSyncStatus,
        String lastSyncError,
        Instant lastSyncStartedAt,
        Instant lastSyncFinishedAt,
        Instant latestWeeklyBatch,
        Instant latestMonthlyBatch,
        Instant createdAt,
        Instant updatedAt
) {
    public static GitHubTrendingConfigDto fromEntity(GitHubTrendingConfig config) {
        return new GitHubTrendingConfigDto(
                config.getId(),
                config.getLanguageFilter(),
                config.getKeywordFilter(),
                config.getHomeDisplayCount(),
                config.getRefreshCron(),
                config.getLastSyncStatus(),
                config.getLastSyncError(),
                config.getLastSyncStartedAt(),
                config.getLastSyncFinishedAt(),
                config.getLatestWeeklyBatch(),
                config.getLatestMonthlyBatch(),
                config.getCreatedAt(),
                config.getUpdatedAt()
        );
    }
}
