package com.example.platform.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "github_trending_config")
public class GitHubTrendingConfig {

    public static final Long SINGLETON_ID = 1L;
    public enum SyncStatus { IDLE, RUNNING, COMPLETED, FAILED }

    @Id
    private Long id;

    @Column(name = "language_filter", length = 100)
    private String languageFilter;

    @Column(name = "keyword_filter", length = 500)
    private String keywordFilter;

    @Column(name = "home_display_count", nullable = false)
    private int homeDisplayCount = 10;

    @Column(name = "refresh_cron", nullable = false, length = 100)
    private String refreshCron = "0 0 8 * * *";

    @Enumerated(EnumType.STRING)
    @Column(name = "last_sync_status", nullable = false, length = 30)
    private SyncStatus lastSyncStatus = SyncStatus.IDLE;

    @Column(name = "last_sync_error", length = 1000)
    private String lastSyncError;

    @Column(name = "last_sync_started_at")
    private Instant lastSyncStartedAt;

    @Column(name = "last_sync_finished_at")
    private Instant lastSyncFinishedAt;

    @Column(name = "latest_weekly_batch")
    private Instant latestWeeklyBatch;

    @Column(name = "latest_monthly_batch")
    private Instant latestMonthlyBatch;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public static GitHubTrendingConfig defaultConfig() {
        GitHubTrendingConfig config = new GitHubTrendingConfig();
        config.setId(SINGLETON_ID);
        return config;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getLanguageFilter() { return languageFilter; }
    public void setLanguageFilter(String languageFilter) { this.languageFilter = languageFilter; }
    public String getKeywordFilter() { return keywordFilter; }
    public void setKeywordFilter(String keywordFilter) { this.keywordFilter = keywordFilter; }
    public int getHomeDisplayCount() { return homeDisplayCount; }
    public void setHomeDisplayCount(int homeDisplayCount) { this.homeDisplayCount = homeDisplayCount; }
    public String getRefreshCron() { return refreshCron; }
    public void setRefreshCron(String refreshCron) { this.refreshCron = refreshCron; }
    public SyncStatus getLastSyncStatus() { return lastSyncStatus; }
    public void setLastSyncStatus(SyncStatus lastSyncStatus) { this.lastSyncStatus = lastSyncStatus; }
    public String getLastSyncError() { return lastSyncError; }
    public void setLastSyncError(String lastSyncError) { this.lastSyncError = lastSyncError; }
    public Instant getLastSyncStartedAt() { return lastSyncStartedAt; }
    public void setLastSyncStartedAt(Instant lastSyncStartedAt) { this.lastSyncStartedAt = lastSyncStartedAt; }
    public Instant getLastSyncFinishedAt() { return lastSyncFinishedAt; }
    public void setLastSyncFinishedAt(Instant lastSyncFinishedAt) { this.lastSyncFinishedAt = lastSyncFinishedAt; }
    public Instant getLatestWeeklyBatch() { return latestWeeklyBatch; }
    public void setLatestWeeklyBatch(Instant latestWeeklyBatch) { this.latestWeeklyBatch = latestWeeklyBatch; }
    public Instant getLatestMonthlyBatch() { return latestMonthlyBatch; }
    public void setLatestMonthlyBatch(Instant latestMonthlyBatch) { this.latestMonthlyBatch = latestMonthlyBatch; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
