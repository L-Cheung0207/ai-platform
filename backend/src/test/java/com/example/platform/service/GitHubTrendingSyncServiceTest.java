package com.example.platform.service;

import com.example.platform.dto.GitHubTrendingConfigDto;
import com.example.platform.dto.GitHubTrendingStatusDto;
import com.example.platform.entity.GitHubTrendingConfig;
import com.example.platform.entity.GitHubTrendingEntry;
import com.example.platform.repository.GitHubTrendingConfigRepository;
import com.example.platform.repository.GitHubTrendingEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class GitHubTrendingSyncServiceTest {

    @Autowired
    private GitHubTrendingService trendingService;

    @Autowired
    private GitHubTrendingEntryRepository entryRepository;

    @Autowired
    private GitHubTrendingConfigRepository configRepository;

    @Autowired
    private StubGitHubTrendingScraperService scraperService;

    @Autowired
    private StubGitHubTrendingSummaryService summaryService;

    @BeforeEach
    void setUp() {
        entryRepository.deleteAll();
        configRepository.deleteAll();
        scraperService.reset();
        summaryService.reset();
    }

    @Test
    void syncNowFetchesWeeklyAndMonthlyUpsertsRowsAndGeneratesSummariesForNewEntries() throws Exception {
        GitHubTrendingConfig config = GitHubTrendingConfig.defaultConfig();
        config.setLanguageFilter("TypeScript");
        configRepository.save(config);
        GitHubTrendingEntry manual = entry(
                GitHubTrendingEntry.Period.WEEKLY,
                "owner/manual",
                "Old description",
                GitHubTrendingEntry.SummaryStatus.MANUAL,
                "人工作用",
                "人工场景",
                Instant.parse("2026-06-13T00:00:00Z")
        );
        entryRepository.save(manual);
        scraperService.rows.put(GitHubTrendingEntry.Period.WEEKLY, List.of(
                row(GitHubTrendingEntry.Period.WEEKLY, 1, "owner/manual", "New description"),
                row(GitHubTrendingEntry.Period.WEEKLY, 2, "owner/new-weekly", "Weekly description")
        ));
        scraperService.rows.put(GitHubTrendingEntry.Period.MONTHLY, List.of(
                row(GitHubTrendingEntry.Period.MONTHLY, 1, "owner/new-monthly", "Monthly description")
        ));

        GitHubTrendingStatusDto status = trendingService.syncNow();

        assertThat(status.status()).isEqualTo(GitHubTrendingConfig.SyncStatus.COMPLETED);
        GitHubTrendingConfig savedConfig = configRepository.findById(GitHubTrendingConfig.SINGLETON_ID).orElseThrow();
        assertThat(savedConfig.getLatestWeeklyBatch()).isNotNull();
        assertThat(savedConfig.getLatestMonthlyBatch()).isEqualTo(savedConfig.getLatestWeeklyBatch());
        assertThat(savedConfig.getLastSyncError()).isNull();
        GitHubTrendingEntry savedManual = entryRepository
                .findByPeriodAndRepoFullName(GitHubTrendingEntry.Period.WEEKLY, "owner/manual")
                .orElseThrow();
        assertThat(savedManual.getRank()).isEqualTo(1);
        assertThat(savedManual.getDescription()).isEqualTo("New description");
        assertThat(savedManual.getSummaryStatus()).isEqualTo(GitHubTrendingEntry.SummaryStatus.MANUAL);
        assertThat(savedManual.getEffectCn()).isEqualTo("人工作用");
        assertThat(savedManual.getScenarioCn()).isEqualTo("人工场景");
        GitHubTrendingEntry newWeekly = entryRepository
                .findByPeriodAndRepoFullName(GitHubTrendingEntry.Period.WEEKLY, "owner/new-weekly")
                .orElseThrow();
        assertThat(newWeekly.getSummaryStatus()).isEqualTo(GitHubTrendingEntry.SummaryStatus.GENERATED);
        assertThat(newWeekly.getEffectCn()).isEqualTo("作用：owner/new-weekly");
        assertThat(entryRepository.findLatestByPeriod(
                GitHubTrendingEntry.Period.WEEKLY,
                savedConfig.getLatestWeeklyBatch(),
                PageRequest.of(0, 10)
        )).extracting(GitHubTrendingEntry::getRepoFullName)
                .containsExactly("owner/manual", "owner/new-weekly");
        assertThat(scraperService.languageFilters).containsExactly("TypeScript", "TypeScript");
        assertThat(summaryService.generatedRepoNames).doesNotContain("owner/manual");
    }

    @Test
    void getConfigCreatesDefaultWhenMissing() {
        GitHubTrendingConfigDto dto = trendingService.getConfig();

        assertThat(dto.id()).isEqualTo(GitHubTrendingConfig.SINGLETON_ID);
        assertThat(dto.homeDisplayCount()).isEqualTo(10);
        assertThat(dto.refreshCron()).isEqualTo("0 0 8 * * *");
        assertThat(dto.lastSyncStatus()).isEqualTo(GitHubTrendingConfig.SyncStatus.IDLE);
        assertThat(configRepository.findById(GitHubTrendingConfig.SINGLETON_ID)).isPresent();
    }

    @Test
    void syncFailureKeepsPreviousBatchAndStoresFailureStatusAndError() throws Exception {
        Instant oldBatch = Instant.parse("2026-06-12T00:00:00Z");
        GitHubTrendingConfig config = GitHubTrendingConfig.defaultConfig();
        config.setLatestWeeklyBatch(oldBatch);
        config.setLatestMonthlyBatch(oldBatch);
        config.setLastSyncStatus(GitHubTrendingConfig.SyncStatus.COMPLETED);
        configRepository.save(config);
        entryRepository.save(entry(
                GitHubTrendingEntry.Period.WEEKLY,
                "owner/old",
                "Old",
                GitHubTrendingEntry.SummaryStatus.GENERATED,
                "旧作用",
                "旧场景",
                oldBatch
        ));
        scraperService.failure = new IOException("github unavailable");

        assertThatThrownBy(() -> trendingService.syncNow())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("github unavailable");

        GitHubTrendingConfig savedConfig = configRepository.findById(GitHubTrendingConfig.SINGLETON_ID).orElseThrow();
        assertThat(savedConfig.getLastSyncStatus()).isEqualTo(GitHubTrendingConfig.SyncStatus.FAILED);
        assertThat(savedConfig.getLastSyncError()).contains("github unavailable");
        assertThat(savedConfig.getLatestWeeklyBatch()).isEqualTo(oldBatch);
        assertThat(savedConfig.getLatestMonthlyBatch()).isEqualTo(oldBatch);
        assertThat(entryRepository.countByPeriodAndLastSeenBatch(GitHubTrendingEntry.Period.WEEKLY, oldBatch)).isEqualTo(1);
    }

    private GitHubTrendingScraperService.TrendingRow row(
            GitHubTrendingEntry.Period period,
            int rank,
            String repoFullName,
            String description
    ) {
        return new GitHubTrendingScraperService.TrendingRow(
                period,
                rank,
                repoFullName,
                "https://github.com/" + repoFullName,
                description,
                "TypeScript",
                1000 + rank,
                100 + rank,
                10 + rank
        );
    }

    private GitHubTrendingEntry entry(
            GitHubTrendingEntry.Period period,
            String repoFullName,
            String description,
            GitHubTrendingEntry.SummaryStatus summaryStatus,
            String effectCn,
            String scenarioCn,
            Instant batch
    ) {
        GitHubTrendingEntry entry = new GitHubTrendingEntry();
        entry.setPeriod(period);
        entry.setRank(9);
        entry.setRepoFullName(repoFullName);
        entry.setRepoUrl("https://github.com/" + repoFullName);
        entry.setDescription(description);
        entry.setLanguage("TypeScript");
        entry.setStars(100);
        entry.setForks(10);
        entry.setStarsGained(5);
        entry.setSummaryStatus(summaryStatus);
        entry.setEffectCn(effectCn);
        entry.setScenarioCn(scenarioCn);
        entry.setSourceFetchedAt(batch);
        entry.setLastSeenBatch(batch);
        return entry;
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        StubGitHubTrendingScraperService scraperService() {
            return new StubGitHubTrendingScraperService();
        }

        @Bean
        StubGitHubTrendingSummaryService summaryService() {
            return new StubGitHubTrendingSummaryService();
        }

        @Bean
        GitHubTrendingService gitHubTrendingService(
                GitHubTrendingEntryRepository entryRepository,
                GitHubTrendingConfigRepository configRepository,
                StubGitHubTrendingScraperService scraperService,
                StubGitHubTrendingSummaryService summaryService
        ) {
            return new GitHubTrendingService(entryRepository, configRepository, scraperService, summaryService);
        }
    }

    static class StubGitHubTrendingScraperService extends GitHubTrendingScraperService {
        private final Map<GitHubTrendingEntry.Period, List<TrendingRow>> rows = new HashMap<>();
        private final List<String> languageFilters = new ArrayList<>();
        private IOException failure;

        @Override
        public List<TrendingRow> fetch(GitHubTrendingEntry.Period period, String languageFilter) throws IOException {
            languageFilters.add(languageFilter);
            if (failure != null) {
                throw failure;
            }
            return rows.getOrDefault(period, List.of());
        }

        void reset() {
            rows.clear();
            languageFilters.clear();
            failure = null;
        }
    }

    static class StubGitHubTrendingSummaryService extends GitHubTrendingSummaryService {
        private final List<String> generatedRepoNames = new ArrayList<>();

        @Override
        public SummaryResult generate(GitHubTrendingEntry entry) {
            generatedRepoNames.add(entry.getRepoFullName());
            return new SummaryResult(
                    "作用：" + entry.getRepoFullName(),
                    "场景：" + entry.getRepoFullName(),
                    SummaryResultStatus.GENERATED
            );
        }

        void reset() {
            generatedRepoNames.clear();
        }
    }
}
