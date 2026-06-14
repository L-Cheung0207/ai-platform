package com.example.platform.service;

import com.example.platform.dto.GitHubTrendingConfigDto;
import com.example.platform.dto.GitHubTrendingConfigRequest;
import com.example.platform.dto.GitHubTrendingDto;
import com.example.platform.dto.GitHubTrendingStatusDto;
import com.example.platform.dto.GitHubTrendingUpdateRequest;
import com.example.platform.entity.GitHubTrendingConfig;
import com.example.platform.entity.GitHubTrendingEntry;
import com.example.platform.repository.GitHubTrendingConfigRepository;
import com.example.platform.repository.GitHubTrendingEntryRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;

@Service
public class GitHubTrendingService {

    private final GitHubTrendingEntryRepository entryRepository;
    private final GitHubTrendingConfigRepository configRepository;
    private final GitHubTrendingScraperService scraperService;
    private final GitHubTrendingSummaryService summaryService;
    private final TransactionTemplate transactionTemplate;

    public GitHubTrendingService(GitHubTrendingEntryRepository entryRepository,
                                 GitHubTrendingConfigRepository configRepository,
                                 GitHubTrendingScraperService scraperService,
                                 GitHubTrendingSummaryService summaryService,
                                 TransactionTemplate transactionTemplate) {
        this.entryRepository = entryRepository;
        this.configRepository = configRepository;
        this.scraperService = scraperService;
        this.summaryService = summaryService;
        this.transactionTemplate = transactionTemplate;
    }

    @Transactional(readOnly = true)
    public List<GitHubTrendingDto> listHome(GitHubTrendingEntry.Period period) {
        GitHubTrendingConfig config = findConfigOrDefault();
        Instant batch = period == GitHubTrendingEntry.Period.MONTHLY
                ? config.getLatestMonthlyBatch()
                : config.getLatestWeeklyBatch();
        if (batch == null) {
            return List.of();
        }
        return entryRepository.findLatestByPeriod(period, batch, PageRequest.of(0, config.getHomeDisplayCount()))
                .stream()
                .map(GitHubTrendingDto::fromEntity)
                .toList();
    }

    @Transactional
    public GitHubTrendingConfigDto getConfig() {
        return GitHubTrendingConfigDto.fromEntity(getOrCreateConfig());
    }

    @Transactional
    public GitHubTrendingConfigDto updateConfig(GitHubTrendingConfigRequest request) {
        GitHubTrendingConfig config = getOrCreateConfig();
        config.setLanguageFilter(blankToNull(request.getLanguageFilter()));
        config.setKeywordFilter(blankToNull(request.getKeywordFilter()));
        if (request.getHomeDisplayCount() != null) {
            config.setHomeDisplayCount(Math.min(30, Math.max(1, request.getHomeDisplayCount())));
        }
        if (request.getRefreshCron() != null && !request.getRefreshCron().isBlank()) {
            config.setRefreshCron(request.getRefreshCron().trim());
        }
        return GitHubTrendingConfigDto.fromEntity(configRepository.save(config));
    }

    @Transactional
    public GitHubTrendingDto updateEntry(Long id, GitHubTrendingUpdateRequest request) {
        GitHubTrendingEntry entry = entryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("GitHub trending entry not found: " + id));
        entry.setEffectCn(request.getEffectCn());
        entry.setScenarioCn(request.getScenarioCn());
        entry.setSummaryStatus(request.getSummaryStatus() == null
                ? GitHubTrendingEntry.SummaryStatus.MANUAL
                : request.getSummaryStatus());
        return GitHubTrendingDto.fromEntity(entryRepository.save(entry));
    }

    public GitHubTrendingStatusDto syncNow() {
        GitHubTrendingConfig config = markSyncStarted();
        Instant batch = Instant.now();
        try {
            syncPeriodFetchedRows(
                    GitHubTrendingEntry.Period.WEEKLY,
                    scraperService.fetch(GitHubTrendingEntry.Period.WEEKLY, config.getLanguageFilter()),
                    batch
            );
            syncPeriodFetchedRows(
                    GitHubTrendingEntry.Period.MONTHLY,
                    scraperService.fetch(GitHubTrendingEntry.Period.MONTHLY, config.getLanguageFilter()),
                    batch
            );
            GitHubTrendingConfig completed = markSyncCompleted(batch);
            return GitHubTrendingStatusDto.fromConfig(completed);
        } catch (Exception e) {
            markSyncFailed(e);
            throw new IllegalStateException("Failed to sync GitHub trending data: " + e.getMessage(), e);
        }
    }

    private GitHubTrendingConfig markSyncStarted() {
        return transactionTemplate.execute(status -> {
            GitHubTrendingConfig config = getOrCreateConfig();
            config.setLastSyncStatus(GitHubTrendingConfig.SyncStatus.RUNNING);
            config.setLastSyncError(null);
            config.setLastSyncStartedAt(Instant.now());
            config.setLastSyncFinishedAt(null);
            return configRepository.save(config);
        });
    }

    private GitHubTrendingConfig markSyncCompleted(Instant batch) {
        return transactionTemplate.execute(status -> {
            GitHubTrendingConfig config = getOrCreateConfig();
            config.setLastSyncStatus(GitHubTrendingConfig.SyncStatus.COMPLETED);
            config.setLastSyncError(null);
            config.setLastSyncFinishedAt(Instant.now());
            config.setLatestWeeklyBatch(batch);
            config.setLatestMonthlyBatch(batch);
            return configRepository.save(config);
        });
    }

    private void markSyncFailed(Exception e) {
        transactionTemplate.executeWithoutResult(status -> {
            GitHubTrendingConfig config = getOrCreateConfig();
            config.setLastSyncStatus(GitHubTrendingConfig.SyncStatus.FAILED);
            config.setLastSyncError(limit(e.getMessage(), 1000));
            config.setLastSyncFinishedAt(Instant.now());
            configRepository.save(config);
        });
    }

    private void syncPeriodFetchedRows(
            GitHubTrendingEntry.Period period,
            List<GitHubTrendingScraperService.TrendingRow> rows,
            Instant batch
    ) {
        transactionTemplate.executeWithoutResult(status -> {
            Instant fetchedAt = Instant.now();
            for (GitHubTrendingScraperService.TrendingRow row : rows) {
                GitHubTrendingEntry entry = entryRepository
                        .findByPeriodAndRepoFullName(period, row.repoFullName())
                        .orElseGet(GitHubTrendingEntry::new);
                boolean isNew = entry.getId() == null;
                applyRow(entry, row, fetchedAt, batch);
                if (isNew) {
                    applyGeneratedSummary(entry);
                }
                entryRepository.save(entry);
            }
        });
    }

    private void applyRow(GitHubTrendingEntry entry,
                          GitHubTrendingScraperService.TrendingRow row,
                          Instant fetchedAt,
                          Instant batch) {
        entry.setPeriod(row.period());
        entry.setRank(row.rank());
        entry.setRepoFullName(row.repoFullName());
        entry.setRepoUrl(row.repoUrl());
        entry.setDescription(row.description());
        entry.setLanguage(row.language());
        entry.setStars(row.stars());
        entry.setForks(row.forks());
        entry.setStarsGained(row.starsGained());
        entry.setSourceFetchedAt(fetchedAt);
        entry.setLastSeenBatch(batch);
    }

    private void applyGeneratedSummary(GitHubTrendingEntry entry) {
        GitHubTrendingSummaryService.SummaryResult result = summaryService.generate(entry);
        entry.setEffectCn(result.effectCn());
        entry.setScenarioCn(result.scenarioCn());
        entry.setSummaryStatus(switch (result.status()) {
            case GENERATED -> GitHubTrendingEntry.SummaryStatus.GENERATED;
            case FAILED -> GitHubTrendingEntry.SummaryStatus.FAILED;
            case NEEDS_REVIEW -> GitHubTrendingEntry.SummaryStatus.NEEDS_REVIEW;
        });
    }

    private GitHubTrendingConfig getOrCreateConfig() {
        return configRepository.findById(GitHubTrendingConfig.SINGLETON_ID)
                .orElseGet(() -> configRepository.save(GitHubTrendingConfig.defaultConfig()));
    }

    private GitHubTrendingConfig findConfigOrDefault() {
        return configRepository.findById(GitHubTrendingConfig.SINGLETON_ID)
                .orElseGet(GitHubTrendingConfig::defaultConfig);
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String limit(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
