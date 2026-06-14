package com.example.platform.service;

import com.example.platform.entity.GitHubTrendingConfig;
import com.example.platform.entity.GitHubTrendingRepository;
import com.example.platform.repository.GitHubTrendingConfigRepository;
import com.example.platform.repository.GitHubTrendingRepositoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class GitHubTrendingServiceTest {

    @Autowired
    private GitHubTrendingRepositoryRepository trendingRepository;

    @Autowired
    private GitHubTrendingConfigRepository configRepository;

    @Test
    void repositoryFindsLatestVisibleRecordsByPeriod() {
        Instant batch = Instant.parse("2026-06-14T01:00:00Z");
        GitHubTrendingRepository item = new GitHubTrendingRepository();
        item.setPeriod(GitHubTrendingRepository.Period.WEEKLY);
        item.setRank(1);
        item.setRepoFullName("owner/repo");
        item.setRepoUrl("https://github.com/owner/repo");
        item.setDescription("A useful AI tool");
        item.setLanguage("TypeScript");
        item.setStars(1200);
        item.setForks(80);
        item.setStarsGained(240);
        item.setEffectCn("用于构建 AI 工具。");
        item.setScenarioCn("适合快速搭建内部原型。");
        item.setSummaryStatus(GitHubTrendingRepository.SummaryStatus.GENERATED);
        item.setSourceFetchedAt(batch);
        item.setLastSeenBatch(batch);
        trendingRepository.save(item);

        var page = trendingRepository.findLatestByPeriod(
                GitHubTrendingRepository.Period.WEEKLY,
                batch,
                PageRequest.of(0, 10)
        );

        assertThat(page).hasSize(1);
        assertThat(page.get(0).getRepoFullName()).isEqualTo("owner/repo");
    }

    @Test
    void configStoresDefaultsAndStatus() {
        GitHubTrendingConfig config = GitHubTrendingConfig.defaultConfig();
        config.setLastSyncStatus(GitHubTrendingConfig.SyncStatus.COMPLETED);
        config.setLastSyncError(null);
        configRepository.save(config);

        GitHubTrendingConfig saved = configRepository.findById(GitHubTrendingConfig.SINGLETON_ID).orElseThrow();

        assertThat(saved.getHomeDisplayCount()).isEqualTo(10);
        assertThat(saved.getRefreshCron()).isEqualTo("0 0 8 * * *");
        assertThat(saved.getLastSyncStatus()).isEqualTo(GitHubTrendingConfig.SyncStatus.COMPLETED);
    }
}
