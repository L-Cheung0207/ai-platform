# GitHub Trending Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a GitHub Trending home-page module with weekly/monthly rankings, Chinese "作用 / 场景" summaries, daily/manual refresh, and admin editing.

**Architecture:** Add a focused backend domain for GitHub Trending records, config/status, parsing, sync orchestration, and admin APIs. Extend the existing `/api/home` aggregate endpoint for public display, then add Vue home/sidebar UI and an admin management page following the current LLM leaderboard patterns.

**Tech Stack:** Spring Boot 3, Java 17, JPA, Flyway, Jsoup, Spring Security, MockMvc, Vue 3, Vite, Element Plus, existing axios `api` helper.

---

## File Structure

Create backend files:

- `backend/src/main/resources/db/migration/V31__create_github_trending.sql`: Flyway tables and indexes.
- `backend/src/main/java/com/example/platform/entity/GitHubTrendingRepository.java`: Trending repository record entity.
- `backend/src/main/java/com/example/platform/entity/GitHubTrendingConfig.java`: Single-row config/status entity.
- `backend/src/main/java/com/example/platform/repository/GitHubTrendingRepositoryRepository.java`: JPA queries for records.
- `backend/src/main/java/com/example/platform/repository/GitHubTrendingConfigRepository.java`: JPA access for config/status.
- `backend/src/main/java/com/example/platform/dto/GitHubTrendingDto.java`: Public/admin record DTO.
- `backend/src/main/java/com/example/platform/dto/GitHubTrendingConfigDto.java`: Config DTO.
- `backend/src/main/java/com/example/platform/dto/GitHubTrendingStatusDto.java`: Status DTO.
- `backend/src/main/java/com/example/platform/dto/GitHubTrendingUpdateRequest.java`: Admin summary edit request.
- `backend/src/main/java/com/example/platform/dto/GitHubTrendingConfigRequest.java`: Admin config update request.
- `backend/src/main/java/com/example/platform/service/GitHubTrendingScraperService.java`: URL building, HTML fetch, HTML parsing.
- `backend/src/main/java/com/example/platform/service/GitHubTrendingSummaryService.java`: Chinese summary fallback generation.
- `backend/src/main/java/com/example/platform/service/GitHubTrendingService.java`: Sync, upsert, config, status, home/admin queries.
- `backend/src/main/java/com/example/platform/service/GitHubTrendingScheduler.java`: Daily scheduled refresh.
- `backend/src/main/java/com/example/platform/controller/AdminGitHubTrendingController.java`: Admin endpoints.

Modify backend files:

- `backend/src/main/java/com/example/platform/dto/HomeDto.java`: Add GitHub Trending fields.
- `backend/src/main/java/com/example/platform/service/HomeService.java`: Populate home trending data.
- `backend/src/main/java/com/example/platform/config/AsyncConfig.java`: Reuse existing executor if needed; avoid changes unless scheduler/sync needs an async executor already exposed.
- `PROJECT_CONTEXT.md`: Document new route, entity, APIs, and behavior.

Scheduling is already enabled in `backend/src/main/java/com/example/platform/PlatformApplication.java`, so the plan does not need to modify the application entry point.

Create backend tests:

- `backend/src/test/java/com/example/platform/service/GitHubTrendingScraperServiceTest.java`: HTML parser and URL/config behavior.
- `backend/src/test/java/com/example/platform/service/GitHubTrendingSummaryServiceTest.java`: fallback Chinese summaries and review status.
- `backend/src/test/java/com/example/platform/service/GitHubTrendingServiceTest.java`: upsert, manual preservation, status, home result selection.
- `backend/src/test/java/com/example/platform/controller/AdminGitHubTrendingApiIntegrationTest.java`: admin auth and core endpoints.

Create/modify frontend files:

- Create `frontend/src/views/admin/GitHubTrendingManage.vue`: Admin management page.
- Modify `frontend/src/views/Home.vue`: Add right-sidebar tab module using `/api/home`.
- Modify `frontend/src/router/index.js`: Add `/admin/github-trending`.
- Modify `frontend/src/views/admin/AdminLayout.vue`: Add menu item.
- Modify `frontend/src/assets/styles.css` only if shared styles are needed; prefer scoped styles in new/modified views.

---

### Task 1: Database And Entity Model

**Files:**
- Create: `backend/src/main/resources/db/migration/V31__create_github_trending.sql`
- Create: `backend/src/main/java/com/example/platform/entity/GitHubTrendingRepository.java`
- Create: `backend/src/main/java/com/example/platform/entity/GitHubTrendingConfig.java`
- Create: `backend/src/main/java/com/example/platform/repository/GitHubTrendingRepositoryRepository.java`
- Create: `backend/src/main/java/com/example/platform/repository/GitHubTrendingConfigRepository.java`
- Test via: `backend/src/test/java/com/example/platform/service/GitHubTrendingServiceTest.java`

- [ ] **Step 1: Write a failing repository/model test**

Create `backend/src/test/java/com/example/platform/service/GitHubTrendingServiceTest.java` with this initial test skeleton:

```java
package com.example.platform.service;

import com.example.platform.entity.GitHubTrendingConfig;
import com.example.platform.entity.GitHubTrendingRepository;
import com.example.platform.repository.GitHubTrendingConfigRepository;
import com.example.platform.repository.GitHubTrendingRepositoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
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
```

- [ ] **Step 2: Run the test and verify it fails**

Run:

```bash
cd backend
rtk mvn -Dtest=GitHubTrendingServiceTest test
```

Expected: compile fails because the GitHub Trending entities and repositories do not exist.

- [ ] **Step 3: Add Flyway migration**

Create `backend/src/main/resources/db/migration/V31__create_github_trending.sql`:

```sql
CREATE TABLE github_trending_repositories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    period VARCHAR(20) NOT NULL,
    rank_no INT NOT NULL,
    repo_full_name VARCHAR(255) NOT NULL,
    repo_url VARCHAR(500) NOT NULL,
    description VARCHAR(1000),
    language VARCHAR(100),
    stars INT,
    forks INT,
    stars_gained INT,
    effect_cn VARCHAR(1000),
    scenario_cn VARCHAR(1000),
    summary_status VARCHAR(30) NOT NULL,
    source_fetched_at TIMESTAMP,
    last_seen_batch TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_github_trending_period_repo UNIQUE (period, repo_full_name)
);

CREATE INDEX idx_github_trending_period_batch_rank
    ON github_trending_repositories (period, last_seen_batch, rank_no);

CREATE TABLE github_trending_config (
    id BIGINT PRIMARY KEY,
    language_filter VARCHAR(100),
    keyword_filter VARCHAR(500),
    home_display_count INT NOT NULL DEFAULT 10,
    refresh_cron VARCHAR(100) NOT NULL DEFAULT '0 0 8 * * *',
    last_sync_status VARCHAR(30) NOT NULL DEFAULT 'IDLE',
    last_sync_error VARCHAR(1000),
    last_sync_started_at TIMESTAMP,
    last_sync_finished_at TIMESTAMP,
    latest_weekly_batch TIMESTAMP,
    latest_monthly_batch TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

- [ ] **Step 4: Add entities**

Create `backend/src/main/java/com/example/platform/entity/GitHubTrendingRepository.java`:

```java
package com.example.platform.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "github_trending_repositories",
        uniqueConstraints = @UniqueConstraint(name = "uk_github_trending_period_repo", columnNames = {"period", "repo_full_name"}))
public class GitHubTrendingRepository {

    public enum Period { WEEKLY, MONTHLY }
    public enum SummaryStatus { GENERATED, MANUAL, NEEDS_REVIEW, FAILED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Period period;

    @Column(name = "rank_no", nullable = false)
    private int rank;

    @Column(name = "repo_full_name", nullable = false, length = 255)
    private String repoFullName;

    @Column(name = "repo_url", nullable = false, length = 500)
    private String repoUrl;

    @Column(length = 1000)
    private String description;

    @Column(length = 100)
    private String language;

    private Integer stars;
    private Integer forks;

    @Column(name = "stars_gained")
    private Integer starsGained;

    @Column(name = "effect_cn", length = 1000)
    private String effectCn;

    @Column(name = "scenario_cn", length = 1000)
    private String scenarioCn;

    @Enumerated(EnumType.STRING)
    @Column(name = "summary_status", nullable = false, length = 30)
    private SummaryStatus summaryStatus = SummaryStatus.NEEDS_REVIEW;

    @Column(name = "source_fetched_at")
    private Instant sourceFetchedAt;

    @Column(name = "last_seen_batch")
    private Instant lastSeenBatch;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Period getPeriod() { return period; }
    public void setPeriod(Period period) { this.period = period; }
    public int getRank() { return rank; }
    public void setRank(int rank) { this.rank = rank; }
    public String getRepoFullName() { return repoFullName; }
    public void setRepoFullName(String repoFullName) { this.repoFullName = repoFullName; }
    public String getRepoUrl() { return repoUrl; }
    public void setRepoUrl(String repoUrl) { this.repoUrl = repoUrl; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public Integer getStars() { return stars; }
    public void setStars(Integer stars) { this.stars = stars; }
    public Integer getForks() { return forks; }
    public void setForks(Integer forks) { this.forks = forks; }
    public Integer getStarsGained() { return starsGained; }
    public void setStarsGained(Integer starsGained) { this.starsGained = starsGained; }
    public String getEffectCn() { return effectCn; }
    public void setEffectCn(String effectCn) { this.effectCn = effectCn; }
    public String getScenarioCn() { return scenarioCn; }
    public void setScenarioCn(String scenarioCn) { this.scenarioCn = scenarioCn; }
    public SummaryStatus getSummaryStatus() { return summaryStatus; }
    public void setSummaryStatus(SummaryStatus summaryStatus) { this.summaryStatus = summaryStatus; }
    public Instant getSourceFetchedAt() { return sourceFetchedAt; }
    public void setSourceFetchedAt(Instant sourceFetchedAt) { this.sourceFetchedAt = sourceFetchedAt; }
    public Instant getLastSeenBatch() { return lastSeenBatch; }
    public void setLastSeenBatch(Instant lastSeenBatch) { this.lastSeenBatch = lastSeenBatch; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
```

Create `backend/src/main/java/com/example/platform/entity/GitHubTrendingConfig.java`:

```java
package com.example.platform.entity;

import jakarta.persistence.*;
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
```

- [ ] **Step 5: Add repositories**

Create `backend/src/main/java/com/example/platform/repository/GitHubTrendingRepositoryRepository.java`:

```java
package com.example.platform.repository;

import com.example.platform.entity.GitHubTrendingRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface GitHubTrendingRepositoryRepository extends JpaRepository<GitHubTrendingRepository, Long> {

    Optional<GitHubTrendingRepository> findByPeriodAndRepoFullName(
            GitHubTrendingRepository.Period period,
            String repoFullName
    );

    @Query("SELECT r FROM GitHubTrendingRepository r WHERE r.period = :period AND r.lastSeenBatch = :batch ORDER BY r.rank ASC")
    List<GitHubTrendingRepository> findLatestByPeriod(
            GitHubTrendingRepository.Period period,
            Instant batch,
            Pageable pageable
    );

    long countByPeriodAndLastSeenBatch(GitHubTrendingRepository.Period period, Instant batch);
}
```

Create `backend/src/main/java/com/example/platform/repository/GitHubTrendingConfigRepository.java`:

```java
package com.example.platform.repository;

import com.example.platform.entity.GitHubTrendingConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GitHubTrendingConfigRepository extends JpaRepository<GitHubTrendingConfig, Long> {
}
```

- [ ] **Step 6: Run the model test**

Run:

```bash
cd backend
rtk mvn -Dtest=GitHubTrendingServiceTest test
```

Expected: PASS for the two repository/config tests.

- [ ] **Step 7: Commit**

```bash
rtk git add backend/src/main/resources/db/migration/V31__create_github_trending.sql \
  backend/src/main/java/com/example/platform/entity/GitHubTrendingRepository.java \
  backend/src/main/java/com/example/platform/entity/GitHubTrendingConfig.java \
  backend/src/main/java/com/example/platform/repository/GitHubTrendingRepositoryRepository.java \
  backend/src/main/java/com/example/platform/repository/GitHubTrendingConfigRepository.java \
  backend/src/test/java/com/example/platform/service/GitHubTrendingServiceTest.java
rtk git commit -m "feat: add github trending model"
```

---

### Task 2: Scraper Parser And Summary Fallback

**Files:**
- Create: `backend/src/main/java/com/example/platform/service/GitHubTrendingScraperService.java`
- Create: `backend/src/main/java/com/example/platform/service/GitHubTrendingSummaryService.java`
- Create: `backend/src/test/java/com/example/platform/service/GitHubTrendingScraperServiceTest.java`
- Create: `backend/src/test/java/com/example/platform/service/GitHubTrendingSummaryServiceTest.java`

- [ ] **Step 1: Write scraper parser tests**

Create `backend/src/test/java/com/example/platform/service/GitHubTrendingScraperServiceTest.java`:

```java
package com.example.platform.service;

import com.example.platform.entity.GitHubTrendingRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GitHubTrendingScraperServiceTest {

    private final GitHubTrendingScraperService service = new GitHubTrendingScraperService();

    @Test
    void parseTrendingHtmlExtractsRepositoryRows() {
        String html = """
                <html><body>
                  <article class="Box-row">
                    <h2><a href="/openai/codex">openai / codex</a></h2>
                    <p>Cloud coding agent.</p>
                    <span itemprop="programmingLanguage">TypeScript</span>
                    <a href="/openai/codex/stargazers">12,345</a>
                    <a href="/openai/codex/forks">678</a>
                    <span class="d-inline-block float-sm-right">1,234 stars this week</span>
                  </article>
                </body></html>
                """;

        var rows = service.parseTrendingHtml(html, GitHubTrendingRepository.Period.WEEKLY);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).repoFullName()).isEqualTo("openai/codex");
        assertThat(rows.get(0).repoUrl()).isEqualTo("https://github.com/openai/codex");
        assertThat(rows.get(0).description()).isEqualTo("Cloud coding agent.");
        assertThat(rows.get(0).language()).isEqualTo("TypeScript");
        assertThat(rows.get(0).stars()).isEqualTo(12345);
        assertThat(rows.get(0).forks()).isEqualTo(678);
        assertThat(rows.get(0).starsGained()).isEqualTo(1234);
    }

    @Test
    void buildTrendingUrlSupportsLanguageAndPeriod() {
        assertThat(service.buildTrendingUrl(GitHubTrendingRepository.Period.MONTHLY, "typescript"))
                .isEqualTo("https://github.com/trending/typescript?since=monthly");
        assertThat(service.buildTrendingUrl(GitHubTrendingRepository.Period.WEEKLY, " "))
                .isEqualTo("https://github.com/trending?since=weekly");
    }
}
```

- [ ] **Step 2: Write summary fallback tests**

Create `backend/src/test/java/com/example/platform/service/GitHubTrendingSummaryServiceTest.java`:

```java
package com.example.platform.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GitHubTrendingSummaryServiceTest {

    private final GitHubTrendingSummaryService service = new GitHubTrendingSummaryService();

    @Test
    void fallbackSummaryUsesDescriptionWhenPresent() {
        var result = service.generateFallback("openai/codex", "Cloud coding agent.", "TypeScript");

        assertThat(result.effectCn()).contains("Cloud coding agent");
        assertThat(result.scenarioCn()).contains("openai/codex");
        assertThat(result.status()).isEqualTo(GitHubTrendingSummaryService.SummaryResultStatus.NEEDS_REVIEW);
    }

    @Test
    void fallbackSummaryHandlesBlankDescriptionConservatively() {
        var result = service.generateFallback("owner/repo", " ", null);

        assertThat(result.effectCn()).contains("owner/repo");
        assertThat(result.scenarioCn()).contains("复核");
        assertThat(result.status()).isEqualTo(GitHubTrendingSummaryService.SummaryResultStatus.NEEDS_REVIEW);
    }
}
```

- [ ] **Step 3: Run tests and verify they fail**

Run:

```bash
cd backend
rtk mvn -Dtest=GitHubTrendingScraperServiceTest,GitHubTrendingSummaryServiceTest test
```

Expected: compile fails because services do not exist.

- [ ] **Step 4: Implement scraper service**

Create `backend/src/main/java/com/example/platform/service/GitHubTrendingScraperService.java`:

```java
package com.example.platform.service;

import com.example.platform.entity.GitHubTrendingRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class GitHubTrendingScraperService {

    private static final String BASE_URL = "https://github.com";

    public record TrendingRow(
            GitHubTrendingRepository.Period period,
            int rank,
            String repoFullName,
            String repoUrl,
            String description,
            String language,
            Integer stars,
            Integer forks,
            Integer starsGained
    ) {}

    public List<TrendingRow> fetch(GitHubTrendingRepository.Period period, String languageFilter) throws IOException {
        Document doc = Jsoup.connect(buildTrendingUrl(period, languageFilter))
                .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 Chrome/120 Safari/537.36")
                .timeout(30000)
                .get();
        return parseTrendingHtml(doc.html(), period);
    }

    public String buildTrendingUrl(GitHubTrendingRepository.Period period, String languageFilter) {
        String since = period == GitHubTrendingRepository.Period.MONTHLY ? "monthly" : "weekly";
        String lang = languageFilter == null ? "" : languageFilter.trim().toLowerCase(Locale.ROOT);
        if (lang.isBlank()) {
            return BASE_URL + "/trending?since=" + since;
        }
        return BASE_URL + "/trending/" + lang + "?since=" + since;
    }

    public List<TrendingRow> parseTrendingHtml(String html, GitHubTrendingRepository.Period period) {
        Document doc = Jsoup.parse(html, BASE_URL + "/trending");
        List<TrendingRow> rows = new ArrayList<>();
        int rank = 1;
        for (Element article : doc.select("article.Box-row")) {
            Element link = article.selectFirst("h2 a[href]");
            if (link == null) continue;
            String repoFullName = link.text().replaceAll("\\s+", "").trim();
            if (!repoFullName.contains("/")) continue;
            String repoUrl = link.attr("abs:href");
            String description = textOrNull(article.selectFirst("p"));
            String language = textOrNull(article.selectFirst("[itemprop=programmingLanguage]"));
            Integer stars = parseNumberFromText(textOrNull(article.selectFirst("a[href$=/stargazers]")));
            Integer forks = parseNumberFromText(textOrNull(article.selectFirst("a[href$=/forks]")));
            Integer starsGained = parseNumberFromText(textOrNull(article.selectFirst(".float-sm-right")));
            rows.add(new TrendingRow(period, rank++, repoFullName, repoUrl, description, language, stars, forks, starsGained));
        }
        return rows;
    }

    private String textOrNull(Element element) {
        if (element == null) return null;
        String text = element.text().trim();
        return text.isBlank() ? null : text;
    }

    private Integer parseNumberFromText(String text) {
        if (text == null || text.isBlank()) return null;
        String digits = text.replaceAll("[^0-9]", "");
        if (digits.isBlank()) return null;
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
```

- [ ] **Step 5: Implement summary service**

Create `backend/src/main/java/com/example/platform/service/GitHubTrendingSummaryService.java`:

```java
package com.example.platform.service;

import org.springframework.stereotype.Service;

@Service
public class GitHubTrendingSummaryService {

    public enum SummaryResultStatus { GENERATED, NEEDS_REVIEW, FAILED }

    public record SummaryResult(String effectCn, String scenarioCn, SummaryResultStatus status) {}

    public SummaryResult generate(String repoFullName, String description, String language) {
        return generateFallback(repoFullName, description, language);
    }

    public SummaryResult generateFallback(String repoFullName, String description, String language) {
        String repo = clean(repoFullName, "该仓库");
        String desc = clean(description, "");
        String lang = clean(language, "");
        if (!desc.isBlank()) {
            String effect = truncate(desc, 180);
            String scenario = lang.isBlank()
                    ? "适合评估 " + repo + " 是否能用于当前项目或团队工具链。"
                    : "适合在 " + lang + " 技术栈中评估 " + repo + " 的落地价值。";
            return new SummaryResult(effect, scenario, SummaryResultStatus.NEEDS_REVIEW);
        }
        return new SummaryResult(
                repo + " 是 GitHub Trending 仓库，具体作用需要结合 README 复核。",
                "适合先由管理员复核用途，再决定是否推荐给团队使用。",
                SummaryResultStatus.NEEDS_REVIEW
        );
    }

    private String clean(String value, String fallback) {
        if (value == null || value.isBlank()) return fallback;
        return value.trim();
    }

    private String truncate(String value, int max) {
        if (value.length() <= max) return value;
        return value.substring(0, max);
    }
}
```

- [ ] **Step 6: Run parser and summary tests**

Run:

```bash
cd backend
rtk mvn -Dtest=GitHubTrendingScraperServiceTest,GitHubTrendingSummaryServiceTest test
```

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
rtk git add backend/src/main/java/com/example/platform/service/GitHubTrendingScraperService.java \
  backend/src/main/java/com/example/platform/service/GitHubTrendingSummaryService.java \
  backend/src/test/java/com/example/platform/service/GitHubTrendingScraperServiceTest.java \
  backend/src/test/java/com/example/platform/service/GitHubTrendingSummaryServiceTest.java
rtk git commit -m "feat: parse github trending rows"
```

---

### Task 3: Sync Service, DTOs, And Home Data

**Files:**
- Create: `backend/src/main/java/com/example/platform/dto/GitHubTrendingDto.java`
- Create: `backend/src/main/java/com/example/platform/dto/GitHubTrendingConfigDto.java`
- Create: `backend/src/main/java/com/example/platform/dto/GitHubTrendingStatusDto.java`
- Create: `backend/src/main/java/com/example/platform/dto/GitHubTrendingUpdateRequest.java`
- Create: `backend/src/main/java/com/example/platform/dto/GitHubTrendingConfigRequest.java`
- Create: `backend/src/main/java/com/example/platform/service/GitHubTrendingService.java`
- Modify: `backend/src/main/java/com/example/platform/dto/HomeDto.java`
- Modify: `backend/src/main/java/com/example/platform/service/HomeService.java`
- Modify test: `backend/src/test/java/com/example/platform/service/GitHubTrendingServiceTest.java`

- [ ] **Step 1: Replace service test with sync behavior tests**

Extend `backend/src/test/java/com/example/platform/service/GitHubTrendingServiceTest.java` with service-level Mockito tests in a second class if keeping the `@DataJpaTest` class is awkward. Use this class name for service behavior: `GitHubTrendingSyncServiceTest`.

Create `backend/src/test/java/com/example/platform/service/GitHubTrendingSyncServiceTest.java`:

```java
package com.example.platform.service;

import com.example.platform.entity.GitHubTrendingConfig;
import com.example.platform.entity.GitHubTrendingRepository;
import com.example.platform.repository.GitHubTrendingConfigRepository;
import com.example.platform.repository.GitHubTrendingRepositoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GitHubTrendingSyncServiceTest {

    @Mock private GitHubTrendingRepositoryRepository trendingRepository;
    @Mock private GitHubTrendingConfigRepository configRepository;
    @Mock private GitHubTrendingScraperService scraperService;
    @Mock private GitHubTrendingSummaryService summaryService;

    private GitHubTrendingService service;

    @BeforeEach
    void setUp() {
        service = new GitHubTrendingService(trendingRepository, configRepository, scraperService, summaryService);
    }

    @Test
    void syncCreatesNewRecordWithGeneratedSummary() throws Exception {
        GitHubTrendingConfig config = GitHubTrendingConfig.defaultConfig();
        when(configRepository.findById(GitHubTrendingConfig.SINGLETON_ID)).thenReturn(Optional.of(config));
        when(scraperService.fetch(GitHubTrendingRepository.Period.WEEKLY, null)).thenReturn(List.of(
                new GitHubTrendingScraperService.TrendingRow(GitHubTrendingRepository.Period.WEEKLY, 1, "owner/repo", "https://github.com/owner/repo", "AI tool", "Java", 10, 2, 5)
        ));
        when(scraperService.fetch(GitHubTrendingRepository.Period.MONTHLY, null)).thenReturn(List.of());
        when(trendingRepository.findByPeriodAndRepoFullName(GitHubTrendingRepository.Period.WEEKLY, "owner/repo")).thenReturn(Optional.empty());
        when(summaryService.generate("owner/repo", "AI tool", "Java")).thenReturn(
                new GitHubTrendingSummaryService.SummaryResult("用于 AI 工具。", "适合 Java 团队。", GitHubTrendingSummaryService.SummaryResultStatus.GENERATED)
        );
        when(trendingRepository.save(any(GitHubTrendingRepository.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(configRepository.save(any(GitHubTrendingConfig.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.syncNow();

        ArgumentCaptor<GitHubTrendingRepository> captor = ArgumentCaptor.forClass(GitHubTrendingRepository.class);
        verify(trendingRepository).save(captor.capture());
        GitHubTrendingRepository saved = captor.getValue();
        assertThat(saved.getRepoFullName()).isEqualTo("owner/repo");
        assertThat(saved.getEffectCn()).isEqualTo("用于 AI 工具。");
        assertThat(saved.getSummaryStatus()).isEqualTo(GitHubTrendingRepository.SummaryStatus.GENERATED);
        assertThat(config.getLastSyncStatus()).isEqualTo(GitHubTrendingConfig.SyncStatus.COMPLETED);
        assertThat(config.getLatestWeeklyBatch()).isNotNull();
    }

    @Test
    void syncPreservesManualSummaryOnExistingRecord() throws Exception {
        GitHubTrendingConfig config = GitHubTrendingConfig.defaultConfig();
        GitHubTrendingRepository existing = new GitHubTrendingRepository();
        existing.setPeriod(GitHubTrendingRepository.Period.WEEKLY);
        existing.setRepoFullName("owner/repo");
        existing.setEffectCn("人工作用");
        existing.setScenarioCn("人工场景");
        existing.setSummaryStatus(GitHubTrendingRepository.SummaryStatus.MANUAL);

        when(configRepository.findById(GitHubTrendingConfig.SINGLETON_ID)).thenReturn(Optional.of(config));
        when(scraperService.fetch(GitHubTrendingRepository.Period.WEEKLY, null)).thenReturn(List.of(
                new GitHubTrendingScraperService.TrendingRow(GitHubTrendingRepository.Period.WEEKLY, 2, "owner/repo", "https://github.com/owner/repo", "New desc", "Java", 20, 3, 6)
        ));
        when(scraperService.fetch(GitHubTrendingRepository.Period.MONTHLY, null)).thenReturn(List.of());
        when(trendingRepository.findByPeriodAndRepoFullName(GitHubTrendingRepository.Period.WEEKLY, "owner/repo")).thenReturn(Optional.of(existing));
        when(trendingRepository.save(any(GitHubTrendingRepository.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(configRepository.save(any(GitHubTrendingConfig.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.syncNow();

        assertThat(existing.getRank()).isEqualTo(2);
        assertThat(existing.getDescription()).isEqualTo("New desc");
        assertThat(existing.getEffectCn()).isEqualTo("人工作用");
        assertThat(existing.getScenarioCn()).isEqualTo("人工场景");
        verify(summaryService, never()).generate(any(), any(), any());
    }

    @Test
    void homeResultsUseLatestConfiguredBatchAndLimit() {
        GitHubTrendingConfig config = GitHubTrendingConfig.defaultConfig();
        Instant weeklyBatch = Instant.parse("2026-06-14T01:00:00Z");
        config.setLatestWeeklyBatch(weeklyBatch);
        config.setHomeDisplayCount(5);
        when(configRepository.findById(GitHubTrendingConfig.SINGLETON_ID)).thenReturn(Optional.of(config));
        when(trendingRepository.findLatestByPeriod(GitHubTrendingRepository.Period.WEEKLY, weeklyBatch, PageRequest.of(0, 5))).thenReturn(List.of());

        assertThat(service.listHome(GitHubTrendingRepository.Period.WEEKLY)).isEmpty();

        verify(trendingRepository).findLatestByPeriod(GitHubTrendingRepository.Period.WEEKLY, weeklyBatch, PageRequest.of(0, 5));
    }
}
```

- [ ] **Step 2: Run sync service test and verify it fails**

Run:

```bash
cd backend
rtk mvn -Dtest=GitHubTrendingSyncServiceTest test
```

Expected: compile fails because DTO/service methods are missing.

- [ ] **Step 3: Add DTOs**

Create `backend/src/main/java/com/example/platform/dto/GitHubTrendingDto.java`:

```java
package com.example.platform.dto;

import com.example.platform.entity.GitHubTrendingRepository;
import java.time.Instant;

public class GitHubTrendingDto {
    private Long id;
    private String period;
    private int rank;
    private String repoFullName;
    private String repoUrl;
    private String description;
    private String language;
    private Integer stars;
    private Integer forks;
    private Integer starsGained;
    private String effectCn;
    private String scenarioCn;
    private String summaryStatus;
    private Instant sourceFetchedAt;
    private Instant updatedAt;

    public static GitHubTrendingDto fromEntity(GitHubTrendingRepository entity) {
        GitHubTrendingDto dto = new GitHubTrendingDto();
        dto.setId(entity.getId());
        dto.setPeriod(entity.getPeriod() != null ? entity.getPeriod().name() : null);
        dto.setRank(entity.getRank());
        dto.setRepoFullName(entity.getRepoFullName());
        dto.setRepoUrl(entity.getRepoUrl());
        dto.setDescription(entity.getDescription());
        dto.setLanguage(entity.getLanguage());
        dto.setStars(entity.getStars());
        dto.setForks(entity.getForks());
        dto.setStarsGained(entity.getStarsGained());
        dto.setEffectCn(entity.getEffectCn());
        dto.setScenarioCn(entity.getScenarioCn());
        dto.setSummaryStatus(entity.getSummaryStatus() != null ? entity.getSummaryStatus().name() : null);
        dto.setSourceFetchedAt(entity.getSourceFetchedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }
    public int getRank() { return rank; }
    public void setRank(int rank) { this.rank = rank; }
    public String getRepoFullName() { return repoFullName; }
    public void setRepoFullName(String repoFullName) { this.repoFullName = repoFullName; }
    public String getRepoUrl() { return repoUrl; }
    public void setRepoUrl(String repoUrl) { this.repoUrl = repoUrl; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public Integer getStars() { return stars; }
    public void setStars(Integer stars) { this.stars = stars; }
    public Integer getForks() { return forks; }
    public void setForks(Integer forks) { this.forks = forks; }
    public Integer getStarsGained() { return starsGained; }
    public void setStarsGained(Integer starsGained) { this.starsGained = starsGained; }
    public String getEffectCn() { return effectCn; }
    public void setEffectCn(String effectCn) { this.effectCn = effectCn; }
    public String getScenarioCn() { return scenarioCn; }
    public void setScenarioCn(String scenarioCn) { this.scenarioCn = scenarioCn; }
    public String getSummaryStatus() { return summaryStatus; }
    public void setSummaryStatus(String summaryStatus) { this.summaryStatus = summaryStatus; }
    public Instant getSourceFetchedAt() { return sourceFetchedAt; }
    public void setSourceFetchedAt(Instant sourceFetchedAt) { this.sourceFetchedAt = sourceFetchedAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
```

Create config/status/request DTOs with simple fields and validation:

```java
// backend/src/main/java/com/example/platform/dto/GitHubTrendingUpdateRequest.java
package com.example.platform.dto;

import jakarta.validation.constraints.Size;

public class GitHubTrendingUpdateRequest {
    @Size(max = 1000)
    private String effectCn;
    @Size(max = 1000)
    private String scenarioCn;
    public String getEffectCn() { return effectCn; }
    public void setEffectCn(String effectCn) { this.effectCn = effectCn; }
    public String getScenarioCn() { return scenarioCn; }
    public void setScenarioCn(String scenarioCn) { this.scenarioCn = scenarioCn; }
}
```

```java
// backend/src/main/java/com/example/platform/dto/GitHubTrendingConfigRequest.java
package com.example.platform.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public class GitHubTrendingConfigRequest {
    @Size(max = 100)
    private String languageFilter;
    @Size(max = 500)
    private String keywordFilter;
    @Min(1)
    @Max(30)
    private Integer homeDisplayCount;
    @Size(max = 100)
    private String refreshCron;
    public String getLanguageFilter() { return languageFilter; }
    public void setLanguageFilter(String languageFilter) { this.languageFilter = languageFilter; }
    public String getKeywordFilter() { return keywordFilter; }
    public void setKeywordFilter(String keywordFilter) { this.keywordFilter = keywordFilter; }
    public Integer getHomeDisplayCount() { return homeDisplayCount; }
    public void setHomeDisplayCount(Integer homeDisplayCount) { this.homeDisplayCount = homeDisplayCount; }
    public String getRefreshCron() { return refreshCron; }
    public void setRefreshCron(String refreshCron) { this.refreshCron = refreshCron; }
}
```

```java
// backend/src/main/java/com/example/platform/dto/GitHubTrendingConfigDto.java
package com.example.platform.dto;

import com.example.platform.entity.GitHubTrendingConfig;

public class GitHubTrendingConfigDto {
    private String languageFilter;
    private String keywordFilter;
    private int homeDisplayCount;
    private String refreshCron;

    public static GitHubTrendingConfigDto fromEntity(GitHubTrendingConfig config) {
        GitHubTrendingConfigDto dto = new GitHubTrendingConfigDto();
        dto.setLanguageFilter(config.getLanguageFilter());
        dto.setKeywordFilter(config.getKeywordFilter());
        dto.setHomeDisplayCount(config.getHomeDisplayCount());
        dto.setRefreshCron(config.getRefreshCron());
        return dto;
    }

    public String getLanguageFilter() { return languageFilter; }
    public void setLanguageFilter(String languageFilter) { this.languageFilter = languageFilter; }
    public String getKeywordFilter() { return keywordFilter; }
    public void setKeywordFilter(String keywordFilter) { this.keywordFilter = keywordFilter; }
    public int getHomeDisplayCount() { return homeDisplayCount; }
    public void setHomeDisplayCount(int homeDisplayCount) { this.homeDisplayCount = homeDisplayCount; }
    public String getRefreshCron() { return refreshCron; }
    public void setRefreshCron(String refreshCron) { this.refreshCron = refreshCron; }
}
```

```java
// backend/src/main/java/com/example/platform/dto/GitHubTrendingStatusDto.java
package com.example.platform.dto;

import com.example.platform.entity.GitHubTrendingConfig;
import java.time.Instant;

public class GitHubTrendingStatusDto {
    private String status;
    private String error;
    private Instant startedAt;
    private Instant finishedAt;
    private Instant latestWeeklyBatch;
    private Instant latestMonthlyBatch;
    private long weeklyCount;
    private long monthlyCount;

    public static GitHubTrendingStatusDto fromEntity(GitHubTrendingConfig config, long weeklyCount, long monthlyCount) {
        GitHubTrendingStatusDto dto = new GitHubTrendingStatusDto();
        dto.setStatus(config.getLastSyncStatus().name());
        dto.setError(config.getLastSyncError());
        dto.setStartedAt(config.getLastSyncStartedAt());
        dto.setFinishedAt(config.getLastSyncFinishedAt());
        dto.setLatestWeeklyBatch(config.getLatestWeeklyBatch());
        dto.setLatestMonthlyBatch(config.getLatestMonthlyBatch());
        dto.setWeeklyCount(weeklyCount);
        dto.setMonthlyCount(monthlyCount);
        return dto;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
    public void setFinishedAt(Instant finishedAt) { this.finishedAt = finishedAt; }
    public Instant getLatestWeeklyBatch() { return latestWeeklyBatch; }
    public void setLatestWeeklyBatch(Instant latestWeeklyBatch) { this.latestWeeklyBatch = latestWeeklyBatch; }
    public Instant getLatestMonthlyBatch() { return latestMonthlyBatch; }
    public void setLatestMonthlyBatch(Instant latestMonthlyBatch) { this.latestMonthlyBatch = latestMonthlyBatch; }
    public long getWeeklyCount() { return weeklyCount; }
    public void setWeeklyCount(long weeklyCount) { this.weeklyCount = weeklyCount; }
    public long getMonthlyCount() { return monthlyCount; }
    public void setMonthlyCount(long monthlyCount) { this.monthlyCount = monthlyCount; }
}
```

- [ ] **Step 4: Implement GitHubTrendingService**

Create `backend/src/main/java/com/example/platform/service/GitHubTrendingService.java` with methods used by tests and controllers:

```java
package com.example.platform.service;

import com.example.platform.config.ResourceNotFoundException;
import com.example.platform.dto.*;
import com.example.platform.entity.GitHubTrendingConfig;
import com.example.platform.entity.GitHubTrendingRepository;
import com.example.platform.repository.GitHubTrendingConfigRepository;
import com.example.platform.repository.GitHubTrendingRepositoryRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class GitHubTrendingService {

    private final GitHubTrendingRepositoryRepository trendingRepository;
    private final GitHubTrendingConfigRepository configRepository;
    private final GitHubTrendingScraperService scraperService;
    private final GitHubTrendingSummaryService summaryService;

    public GitHubTrendingService(GitHubTrendingRepositoryRepository trendingRepository,
                                 GitHubTrendingConfigRepository configRepository,
                                 GitHubTrendingScraperService scraperService,
                                 GitHubTrendingSummaryService summaryService) {
        this.trendingRepository = trendingRepository;
        this.configRepository = configRepository;
        this.scraperService = scraperService;
        this.summaryService = summaryService;
    }

    @Async("scraperTaskExecutor")
    public void syncAsync() {
        syncNow();
    }

    @Transactional
    public void syncNow() {
        GitHubTrendingConfig config = getOrCreateConfig();
        if (config.getLastSyncStatus() == GitHubTrendingConfig.SyncStatus.RUNNING) {
            throw new IllegalStateException("GitHub Trending 同步任务正在运行");
        }
        Instant started = Instant.now();
        config.setLastSyncStatus(GitHubTrendingConfig.SyncStatus.RUNNING);
        config.setLastSyncStartedAt(started);
        config.setLastSyncError(null);
        configRepository.save(config);
        try {
            Instant weeklyBatch = syncPeriod(GitHubTrendingRepository.Period.WEEKLY, config);
            Instant monthlyBatch = syncPeriod(GitHubTrendingRepository.Period.MONTHLY, config);
            config.setLatestWeeklyBatch(weeklyBatch);
            config.setLatestMonthlyBatch(monthlyBatch);
            config.setLastSyncStatus(GitHubTrendingConfig.SyncStatus.COMPLETED);
            config.setLastSyncFinishedAt(Instant.now());
            configRepository.save(config);
        } catch (Exception e) {
            config.setLastSyncStatus(GitHubTrendingConfig.SyncStatus.FAILED);
            config.setLastSyncError(e.getMessage());
            config.setLastSyncFinishedAt(Instant.now());
            configRepository.save(config);
            throw new IllegalStateException("GitHub Trending 同步失败：" + e.getMessage(), e);
        }
    }

    private Instant syncPeriod(GitHubTrendingRepository.Period period, GitHubTrendingConfig config) throws Exception {
        Instant batch = Instant.now();
        List<GitHubTrendingScraperService.TrendingRow> rows = scraperService.fetch(period, blankToNull(config.getLanguageFilter()));
        for (GitHubTrendingScraperService.TrendingRow row : filterRows(rows, config.getKeywordFilter())) {
            GitHubTrendingRepository entity = trendingRepository.findByPeriodAndRepoFullName(period, row.repoFullName())
                    .orElseGet(GitHubTrendingRepository::new);
            boolean isNew = entity.getId() == null && entity.getRepoFullName() == null;
            entity.setPeriod(period);
            entity.setRank(row.rank());
            entity.setRepoFullName(row.repoFullName());
            entity.setRepoUrl(row.repoUrl());
            entity.setDescription(row.description());
            entity.setLanguage(row.language());
            entity.setStars(row.stars());
            entity.setForks(row.forks());
            entity.setStarsGained(row.starsGained());
            entity.setSourceFetchedAt(batch);
            entity.setLastSeenBatch(batch);
            if (isNew || entity.getSummaryStatus() != GitHubTrendingRepository.SummaryStatus.MANUAL && isBlank(entity.getEffectCn())) {
                applySummary(entity);
            }
            trendingRepository.save(entity);
        }
        return batch;
    }

    private void applySummary(GitHubTrendingRepository entity) {
        var summary = summaryService.generate(entity.getRepoFullName(), entity.getDescription(), entity.getLanguage());
        entity.setEffectCn(summary.effectCn());
        entity.setScenarioCn(summary.scenarioCn());
        entity.setSummaryStatus(mapSummaryStatus(summary.status()));
    }

    private GitHubTrendingRepository.SummaryStatus mapSummaryStatus(GitHubTrendingSummaryService.SummaryResultStatus status) {
        if (status == GitHubTrendingSummaryService.SummaryResultStatus.GENERATED) return GitHubTrendingRepository.SummaryStatus.GENERATED;
        if (status == GitHubTrendingSummaryService.SummaryResultStatus.FAILED) return GitHubTrendingRepository.SummaryStatus.FAILED;
        return GitHubTrendingRepository.SummaryStatus.NEEDS_REVIEW;
    }

    private List<GitHubTrendingScraperService.TrendingRow> filterRows(List<GitHubTrendingScraperService.TrendingRow> rows, String keywordFilter) {
        String kw = blankToNull(keywordFilter);
        if (kw == null) return rows;
        String[] keywords = kw.toLowerCase().split(",");
        return rows.stream().filter(row -> {
            String haystack = ((row.repoFullName() == null ? "" : row.repoFullName()) + " " +
                    (row.description() == null ? "" : row.description()) + " " +
                    (row.language() == null ? "" : row.language())).toLowerCase();
            for (String keyword : keywords) {
                String trimmed = keyword.trim();
                if (!trimmed.isBlank() && haystack.contains(trimmed)) return true;
            }
            return false;
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<GitHubTrendingDto> listHome(GitHubTrendingRepository.Period period) {
        GitHubTrendingConfig config = getOrCreateConfig();
        Instant batch = period == GitHubTrendingRepository.Period.WEEKLY ? config.getLatestWeeklyBatch() : config.getLatestMonthlyBatch();
        if (batch == null) return List.of();
        return trendingRepository.findLatestByPeriod(period, batch, PageRequest.of(0, config.getHomeDisplayCount()))
                .stream().map(GitHubTrendingDto::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public List<GitHubTrendingDto> listAdmin(GitHubTrendingRepository.Period period) {
        GitHubTrendingConfig config = getOrCreateConfig();
        Instant batch = period == GitHubTrendingRepository.Period.WEEKLY ? config.getLatestWeeklyBatch() : config.getLatestMonthlyBatch();
        if (batch == null) return List.of();
        return trendingRepository.findLatestByPeriod(period, batch, PageRequest.of(0, 100))
                .stream().map(GitHubTrendingDto::fromEntity).toList();
    }

    @Transactional
    public GitHubTrendingDto updateSummary(Long id, GitHubTrendingUpdateRequest request) {
        GitHubTrendingRepository entity = trendingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("GitHub Trending 记录不存在"));
        entity.setEffectCn(trimToNull(request.getEffectCn()));
        entity.setScenarioCn(trimToNull(request.getScenarioCn()));
        entity.setSummaryStatus(GitHubTrendingRepository.SummaryStatus.MANUAL);
        return GitHubTrendingDto.fromEntity(trendingRepository.save(entity));
    }

    @Transactional
    public GitHubTrendingDto regenerateSummary(Long id) {
        GitHubTrendingRepository entity = trendingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("GitHub Trending 记录不存在"));
        applySummary(entity);
        return GitHubTrendingDto.fromEntity(trendingRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public GitHubTrendingConfigDto getConfigDto() {
        return GitHubTrendingConfigDto.fromEntity(getOrCreateConfig());
    }

    @Transactional
    public GitHubTrendingConfigDto updateConfig(GitHubTrendingConfigRequest request) {
        GitHubTrendingConfig config = getOrCreateConfig();
        config.setLanguageFilter(trimToNull(request.getLanguageFilter()));
        config.setKeywordFilter(trimToNull(request.getKeywordFilter()));
        if (request.getHomeDisplayCount() != null) config.setHomeDisplayCount(request.getHomeDisplayCount());
        if (!isBlank(request.getRefreshCron())) config.setRefreshCron(request.getRefreshCron().trim());
        return GitHubTrendingConfigDto.fromEntity(configRepository.save(config));
    }

    @Transactional(readOnly = true)
    public GitHubTrendingStatusDto getStatus() {
        GitHubTrendingConfig config = getOrCreateConfig();
        long weekly = config.getLatestWeeklyBatch() == null ? 0 : trendingRepository.countByPeriodAndLastSeenBatch(GitHubTrendingRepository.Period.WEEKLY, config.getLatestWeeklyBatch());
        long monthly = config.getLatestMonthlyBatch() == null ? 0 : trendingRepository.countByPeriodAndLastSeenBatch(GitHubTrendingRepository.Period.MONTHLY, config.getLatestMonthlyBatch());
        return GitHubTrendingStatusDto.fromEntity(config, weekly, monthly);
    }

    @Transactional
    public GitHubTrendingConfig getOrCreateConfig() {
        return configRepository.findById(GitHubTrendingConfig.SINGLETON_ID)
                .orElseGet(() -> configRepository.save(GitHubTrendingConfig.defaultConfig()));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private String trimToNull(String value) {
        return blankToNull(value);
    }
}
```

- [ ] **Step 5: Extend HomeDto and HomeService**

Modify `backend/src/main/java/com/example/platform/dto/HomeDto.java`:

```java
// Add imports/fields/getters/setters beside existing leaderboard fields.
private List<GitHubTrendingDto> githubTrendingWeekly;
private List<GitHubTrendingDto> githubTrendingMonthly;
private java.time.Instant githubTrendingUpdatedAt;

public List<GitHubTrendingDto> getGithubTrendingWeekly() { return githubTrendingWeekly; }
public void setGithubTrendingWeekly(List<GitHubTrendingDto> githubTrendingWeekly) { this.githubTrendingWeekly = githubTrendingWeekly; }
public List<GitHubTrendingDto> getGithubTrendingMonthly() { return githubTrendingMonthly; }
public void setGithubTrendingMonthly(List<GitHubTrendingDto> githubTrendingMonthly) { this.githubTrendingMonthly = githubTrendingMonthly; }
public java.time.Instant getGithubTrendingUpdatedAt() { return githubTrendingUpdatedAt; }
public void setGithubTrendingUpdatedAt(java.time.Instant githubTrendingUpdatedAt) { this.githubTrendingUpdatedAt = githubTrendingUpdatedAt; }
```

Modify `backend/src/main/java/com/example/platform/service/HomeService.java` constructor to inject `GitHubTrendingService`, then in `getHome()` add:

```java
dto.setGithubTrendingWeekly(gitHubTrendingService.listHome(GitHubTrendingRepository.Period.WEEKLY));
dto.setGithubTrendingMonthly(gitHubTrendingService.listHome(GitHubTrendingRepository.Period.MONTHLY));
var githubStatus = gitHubTrendingService.getStatus();
dto.setGithubTrendingUpdatedAt(githubStatus.getFinishedAt());
```

Add import:

```java
import com.example.platform.entity.GitHubTrendingRepository;
```

- [ ] **Step 6: Run backend service tests**

Run:

```bash
cd backend
rtk mvn -Dtest=GitHubTrendingServiceTest,GitHubTrendingSyncServiceTest test
```

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
rtk git add backend/src/main/java/com/example/platform/dto/GitHubTrendingDto.java \
  backend/src/main/java/com/example/platform/dto/GitHubTrendingConfigDto.java \
  backend/src/main/java/com/example/platform/dto/GitHubTrendingStatusDto.java \
  backend/src/main/java/com/example/platform/dto/GitHubTrendingUpdateRequest.java \
  backend/src/main/java/com/example/platform/dto/GitHubTrendingConfigRequest.java \
  backend/src/main/java/com/example/platform/service/GitHubTrendingService.java \
  backend/src/main/java/com/example/platform/dto/HomeDto.java \
  backend/src/main/java/com/example/platform/service/HomeService.java \
  backend/src/test/java/com/example/platform/service/GitHubTrendingSyncServiceTest.java
rtk git commit -m "feat: sync github trending data"
```

---

### Task 4: Admin API And Scheduler

**Files:**
- Create: `backend/src/main/java/com/example/platform/controller/AdminGitHubTrendingController.java`
- Create: `backend/src/main/java/com/example/platform/service/GitHubTrendingScheduler.java`
- Create: `backend/src/test/java/com/example/platform/controller/AdminGitHubTrendingApiIntegrationTest.java`

- [ ] **Step 1: Write admin API integration tests**

Create `backend/src/test/java/com/example/platform/controller/AdminGitHubTrendingApiIntegrationTest.java`:

```java
package com.example.platform.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminGitHubTrendingApiIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void adminEndpointsRequireAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/github-trending/status"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanReadAndUpdateConfig() throws Exception {
        String token = adminToken();

        mockMvc.perform(get("/api/admin/github-trending/config")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.homeDisplayCount").value(10));

        mockMvc.perform(put("/api/admin/github-trending/config")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "languageFilter", "typescript",
                                "keywordFilter", "ai,llm",
                                "homeDisplayCount", 8,
                                "refreshCron", "0 0 8 * * *"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.languageFilter").value("typescript"))
                .andExpect(jsonPath("$.data.homeDisplayCount").value(8));
    }

    private String adminToken() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "admin",
                                "password", "admin123"
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("token").asText();
    }
}
```

- [ ] **Step 2: Run controller test and verify it fails**

Run:

```bash
cd backend
rtk mvn -Dtest=AdminGitHubTrendingApiIntegrationTest test
```

Expected: FAIL with 404 or compile error because controller does not exist.

- [ ] **Step 3: Implement admin controller**

Create `backend/src/main/java/com/example/platform/controller/AdminGitHubTrendingController.java`:

```java
package com.example.platform.controller;

import com.example.platform.dto.*;
import com.example.platform.entity.GitHubTrendingRepository;
import com.example.platform.service.GitHubTrendingService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/github-trending")
public class AdminGitHubTrendingController {

    private final GitHubTrendingService service;

    public AdminGitHubTrendingController(GitHubTrendingService service) {
        this.service = service;
    }

    @GetMapping("/status")
    public ApiResponse<GitHubTrendingStatusDto> status() {
        return ApiResponse.ok(service.getStatus());
    }

    @GetMapping
    public ApiResponse<List<GitHubTrendingDto>> list(@RequestParam(defaultValue = "WEEKLY") GitHubTrendingRepository.Period period) {
        return ApiResponse.ok(service.listAdmin(period));
    }

    @PutMapping("/{id}")
    public ApiResponse<GitHubTrendingDto> update(@PathVariable Long id, @Valid @RequestBody GitHubTrendingUpdateRequest request) {
        return ApiResponse.ok(service.updateSummary(id, request));
    }

    @PostMapping("/sync")
    public ApiResponse<String> sync() {
        service.syncAsync();
        return ApiResponse.ok("GitHub Trending 同步任务已启动");
    }

    @PostMapping("/{id}/regenerate-summary")
    public ApiResponse<GitHubTrendingDto> regenerate(@PathVariable Long id) {
        return ApiResponse.ok(service.regenerateSummary(id));
    }

    @GetMapping("/config")
    public ApiResponse<GitHubTrendingConfigDto> getConfig() {
        return ApiResponse.ok(service.getConfigDto());
    }

    @PutMapping("/config")
    public ApiResponse<GitHubTrendingConfigDto> updateConfig(@Valid @RequestBody GitHubTrendingConfigRequest request) {
        return ApiResponse.ok(service.updateConfig(request));
    }
}
```

- [ ] **Step 4: Implement scheduler**

Create `backend/src/main/java/com/example/platform/service/GitHubTrendingScheduler.java`:

```java
package com.example.platform.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class GitHubTrendingScheduler {

    private static final Logger log = LoggerFactory.getLogger(GitHubTrendingScheduler.class);
    private final GitHubTrendingService service;

    public GitHubTrendingScheduler(GitHubTrendingService service) {
        this.service = service;
    }

    @Scheduled(cron = "0 0 8 * * *", zone = "Asia/Shanghai")
    public void refreshDaily() {
        try {
            service.syncNow();
        } catch (Exception e) {
            log.warn("Scheduled GitHub Trending sync failed: {}", e.getMessage());
        }
    }
}
```

- [ ] **Step 5: Run controller tests**

Run:

```bash
cd backend
rtk mvn -Dtest=AdminGitHubTrendingApiIntegrationTest test
```

Expected: PASS.

- [ ] **Step 6: Run all GitHub Trending backend tests**

Run:

```bash
cd backend
rtk mvn -Dtest=GitHubTrendingServiceTest,GitHubTrendingSyncServiceTest,GitHubTrendingScraperServiceTest,GitHubTrendingSummaryServiceTest,AdminGitHubTrendingApiIntegrationTest test
```

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
rtk git add backend/src/main/java/com/example/platform/controller/AdminGitHubTrendingController.java \
  backend/src/main/java/com/example/platform/service/GitHubTrendingScheduler.java \
  backend/src/test/java/com/example/platform/controller/AdminGitHubTrendingApiIntegrationTest.java
rtk git commit -m "feat: add github trending admin api"
```

---

### Task 5: Home Page Trending Module

**Files:**
- Modify: `frontend/src/views/Home.vue`

- [ ] **Step 1: Identify current home state and add data refs**

Modify `frontend/src/views/Home.vue` script section to add:

```js
const githubTrendingWeekly = ref([])
const githubTrendingMonthly = ref([])
const githubTrendingUpdatedAt = ref('')
const githubTrendingPeriod = ref('weekly')
```

In the existing `/home` load block, add:

```js
githubTrendingWeekly.value = data.githubTrendingWeekly || []
githubTrendingMonthly.value = data.githubTrendingMonthly || []
githubTrendingUpdatedAt.value = data.githubTrendingUpdatedAt || ''
```

Add helpers:

```js
function githubTrendingItems() {
  return githubTrendingPeriod.value === 'weekly'
    ? githubTrendingWeekly.value
    : githubTrendingMonthly.value
}

function formatTrendingTime(value) {
  if (!value) return '暂无同步记录'
  try {
    return new Date(value).toLocaleString('zh-CN', { hour12: false })
  } catch (e) {
    return '暂无同步记录'
  }
}
```

- [ ] **Step 2: Add template section**

Inside the home sidebar, add this section near the LLM leaderboard module:

```vue
<section
  v-if="githubTrendingWeekly?.length || githubTrendingMonthly?.length"
  class="home-section home-section--github home-section-news home-section-anim"
>
  <div class="home-section-header">
    <div>
      <p class="home-section-kicker">Open Source</p>
      <h2 class="home-section-title">
        <span class="home-section-icon">
          <Icons name="link" :size="20" />
        </span>
        GitHub Trending
      </h2>
      <p class="home-trending-time">{{ formatTrendingTime(githubTrendingUpdatedAt) }}</p>
    </div>
    <div class="home-trending-tabs" role="tablist" aria-label="GitHub Trending 周期">
      <button
        type="button"
        class="home-trending-tab"
        :class="{ 'home-trending-tab--active': githubTrendingPeriod === 'weekly' }"
        @click="githubTrendingPeriod = 'weekly'"
      >
        周榜
      </button>
      <button
        type="button"
        class="home-trending-tab"
        :class="{ 'home-trending-tab--active': githubTrendingPeriod === 'monthly' }"
        @click="githubTrendingPeriod = 'monthly'"
      >
        月榜
      </button>
    </div>
  </div>
  <ul class="home-news-list">
    <li v-for="repo in githubTrendingItems()" :key="repo.period + repo.repoFullName" class="home-news-item">
      <a :href="repo.repoUrl" target="_blank" rel="noopener" class="home-news-link home-trending-link">
        <span class="home-news-num" :class="repo.rank <= 3 ? 'home-news-num-hot' : 'home-news-num-normal'">{{ repo.rank }}</span>
        <span class="home-news-copy">
          <span class="home-news-title">{{ repo.repoFullName }}</span>
          <span v-if="repo.effectCn" class="home-news-meta">{{ repo.effectCn }}</span>
          <span v-if="repo.scenarioCn" class="home-trending-scenario">{{ repo.scenarioCn }}</span>
        </span>
      </a>
    </li>
  </ul>
</section>
```

- [ ] **Step 3: Add scoped styles**

In the same file's style block, add:

```css
.home-trending-time {
  margin-top: 4px;
  font-size: 12px;
  color: var(--text-muted);
}

.home-trending-tabs {
  display: inline-flex;
  align-items: center;
  padding: 2px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--bg-body);
}

.home-trending-tab {
  min-height: 32px;
  padding: 0 10px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: var(--text-secondary);
  font-size: 13px;
  cursor: pointer;
}

.home-trending-tab--active {
  background: var(--bg-card);
  color: var(--text-primary);
  box-shadow: var(--shadow-card);
}

.home-trending-link {
  align-items: flex-start;
}

.home-trending-scenario {
  display: block;
  margin-top: 3px;
  color: var(--text-muted);
  font-size: 12px;
  line-height: 1.5;
}
```

- [ ] **Step 4: Run frontend build**

Run:

```bash
cd frontend
rtk npm run build
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
rtk git add frontend/src/views/Home.vue
rtk git commit -m "feat: show github trending on home"
```

---

### Task 6: Admin Frontend

**Files:**
- Create: `frontend/src/views/admin/GitHubTrendingManage.vue`
- Modify: `frontend/src/router/index.js`
- Modify: `frontend/src/views/admin/AdminLayout.vue`

- [ ] **Step 1: Add admin route**

Modify `frontend/src/router/index.js` admin children:

```js
{ path: 'github-trending', name: 'AdminGitHubTrending', component: () => import('../views/admin/GitHubTrendingManage.vue'), meta: { title: 'GitHub Trending' } },
```

- [ ] **Step 2: Add admin menu item**

In `frontend/src/views/admin/AdminLayout.vue`, add an item under `内容与资讯`:

```js
{ path: '/admin/github-trending', label: 'GitHub Trending', icon: IconTrophy },
```

Use existing `IconTrophy` to avoid adding another icon unless the UI needs one.

- [ ] **Step 3: Create admin page**

Create `frontend/src/views/admin/GitHubTrendingManage.vue`:

```vue
<template>
  <div class="github-admin">
    <header class="github-admin__header">
      <div>
        <h1>GitHub Trending</h1>
        <p>同步周榜/月榜，并维护中文“作用 / 场景”摘要</p>
      </div>
      <div class="github-admin__actions">
        <el-button :loading="loading" @click="loadAll">刷新状态</el-button>
        <el-button type="primary" :loading="syncing" @click="syncNow">手动同步</el-button>
      </div>
    </header>

    <div class="github-admin__stats">
      <article class="stat-card">
        <span class="stat-card__label">同步状态</span>
        <strong class="stat-card__value stat-card__value--sm">{{ status.status || 'IDLE' }}</strong>
        <span class="stat-card__hint">{{ status.error || '最近无错误' }}</span>
      </article>
      <article class="stat-card">
        <span class="stat-card__label">周榜数量</span>
        <strong class="stat-card__value">{{ status.weeklyCount || 0 }}</strong>
        <span class="stat-card__hint">{{ formatDate(status.latestWeeklyBatch) }}</span>
      </article>
      <article class="stat-card">
        <span class="stat-card__label">月榜数量</span>
        <strong class="stat-card__value">{{ status.monthlyCount || 0 }}</strong>
        <span class="stat-card__hint">{{ formatDate(status.latestMonthlyBatch) }}</span>
      </article>
    </div>

    <section class="github-admin__panel">
      <div class="panel-head">
        <div>
          <h2>同步配置</h2>
          <p>默认全站 Trending，可按语言或关键词过滤</p>
        </div>
        <el-button :loading="savingConfig" @click="saveConfig">保存配置</el-button>
      </div>
      <el-form :model="config" label-width="110px" class="github-admin__form">
        <el-form-item label="语言过滤">
          <el-input v-model="config.languageFilter" placeholder="例如 typescript，可留空" />
        </el-form-item>
        <el-form-item label="关键词过滤">
          <el-input v-model="config.keywordFilter" placeholder="例如 ai,llm，可留空" />
        </el-form-item>
        <el-form-item label="首页数量">
          <el-input-number v-model="config.homeDisplayCount" :min="1" :max="30" />
        </el-form-item>
        <el-form-item label="刷新 Cron">
          <el-input v-model="config.refreshCron" placeholder="0 0 8 * * *" />
        </el-form-item>
      </el-form>
    </section>

    <section class="github-admin__panel">
      <div class="panel-head">
        <div>
          <h2>榜单编辑</h2>
          <p>人工编辑后的摘要不会被后续自动同步覆盖</p>
        </div>
        <el-radio-group v-model="period" @change="loadItems">
          <el-radio-button label="WEEKLY">周榜</el-radio-button>
          <el-radio-button label="MONTHLY">月榜</el-radio-button>
        </el-radio-group>
      </div>

      <el-table :data="items" v-loading="itemsLoading" row-key="id">
        <el-table-column prop="rank" label="排名" width="80" />
        <el-table-column label="仓库" min-width="190">
          <template #default="{ row }">
            <a :href="row.repoUrl" target="_blank" rel="noopener" class="repo-link">{{ row.repoFullName }}</a>
            <small v-if="row.language">{{ row.language }}</small>
          </template>
        </el-table-column>
        <el-table-column label="作用" min-width="260">
          <template #default="{ row }">
            <el-input v-model="row.effectCn" type="textarea" :rows="2" />
          </template>
        </el-table-column>
        <el-table-column label="场景" min-width="260">
          <template #default="{ row }">
            <el-input v-model="row.scenarioCn" type="textarea" :rows="2" />
          </template>
        </el-table-column>
        <el-table-column prop="summaryStatus" label="状态" width="130" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="saveRow(row)">保存</el-button>
            <el-button size="small" @click="regenerate(row)">重生成</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-alert v-if="error" type="error" :title="error" show-icon />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../../services/api'

const loading = ref(false)
const syncing = ref(false)
const savingConfig = ref(false)
const itemsLoading = ref(false)
const error = ref('')
const status = ref({})
const config = ref({ homeDisplayCount: 10, refreshCron: '0 0 8 * * *' })
const period = ref('WEEKLY')
const items = ref([])

onMounted(loadAll)

async function loadAll() {
  loading.value = true
  error.value = ''
  try {
    await Promise.all([loadStatus(), loadConfig(), loadItems()])
  } catch (e) {
    error.value = e.message || '加载失败'
  } finally {
    loading.value = false
  }
}

async function loadStatus() {
  status.value = await api.get('/admin/github-trending/status')
}

async function loadConfig() {
  config.value = await api.get('/admin/github-trending/config')
}

async function loadItems() {
  itemsLoading.value = true
  try {
    items.value = await api.get('/admin/github-trending', { params: { period: period.value } })
  } finally {
    itemsLoading.value = false
  }
}

async function saveConfig() {
  savingConfig.value = true
  try {
    config.value = await api.put('/admin/github-trending/config', config.value)
    ElMessage.success('配置已保存')
  } finally {
    savingConfig.value = false
  }
}

async function syncNow() {
  syncing.value = true
  try {
    await api.post('/admin/github-trending/sync')
    ElMessage.success('同步任务已启动')
    await loadStatus()
  } finally {
    syncing.value = false
  }
}

async function saveRow(row) {
  const saved = await api.put(`/admin/github-trending/${row.id}`, {
    effectCn: row.effectCn,
    scenarioCn: row.scenarioCn,
  })
  Object.assign(row, saved)
  ElMessage.success('已保存')
}

async function regenerate(row) {
  const saved = await api.post(`/admin/github-trending/${row.id}/regenerate-summary`)
  Object.assign(row, saved)
  ElMessage.success('已重新生成')
}

function formatDate(value) {
  if (!value) return '暂无数据'
  return new Date(value).toLocaleString('zh-CN', { hour12: false })
}
</script>

<style scoped>
@reference "../../assets/styles.css";

.github-admin {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.github-admin__header,
.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.github-admin__header h1,
.panel-head h2 {
  margin: 0;
  color: var(--text-primary);
}

.github-admin__header p,
.panel-head p,
.repo-link + small {
  margin: 6px 0 0;
  color: var(--text-secondary);
}

.github-admin__actions {
  display: flex;
  gap: 10px;
}

.github-admin__stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.github-admin__panel,
.stat-card {
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--bg-card);
  padding: 18px;
  box-shadow: var(--shadow-card);
}

.stat-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.stat-card__label,
.stat-card__hint {
  color: var(--text-secondary);
  font-size: 13px;
}

.stat-card__value {
  color: var(--text-primary);
  font-size: 28px;
}

.stat-card__value--sm {
  font-size: 20px;
}

.github-admin__form {
  margin-top: 18px;
  max-width: 760px;
}

.repo-link {
  display: block;
  color: var(--primary-color);
  font-weight: 600;
}

@media (max-width: 900px) {
  .github-admin__stats {
    grid-template-columns: 1fr;
  }

  .github-admin__header,
  .panel-head {
    flex-direction: column;
  }
}
</style>
```

- [ ] **Step 4: Run frontend build**

Run:

```bash
cd frontend
rtk npm run build
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
rtk git add frontend/src/views/admin/GitHubTrendingManage.vue \
  frontend/src/router/index.js \
  frontend/src/views/admin/AdminLayout.vue
rtk git commit -m "feat: manage github trending"
```

---

### Task 7: Project Context And End-To-End Verification

**Files:**
- Modify: `PROJECT_CONTEXT.md`

- [ ] **Step 1: Update project context**

Modify `PROJECT_CONTEXT.md`:

Add public behavior under product surface:

```markdown
- 首页右侧展示 GitHub Trending 周榜/月榜模块，数据来自后端定时同步。
```

Add admin route:

```markdown
- `/admin/github-trending`
```

Add core domain:

```markdown
- `GitHubTrendingRepository`：GitHub Trending 周榜/月榜记录，包含排名、仓库、作用、场景、摘要状态和抓取批次。
```

Add development facts:

```markdown
- GitHub Trending 由后端每天早上自动同步，也可在后台手动刷新；首页读取 `/api/home` 聚合数据，不单独请求公开 Trending 接口。
```

- [ ] **Step 2: Run backend GitHub Trending tests**

Run:

```bash
cd backend
rtk mvn -Dtest=GitHubTrendingServiceTest,GitHubTrendingSyncServiceTest,GitHubTrendingScraperServiceTest,GitHubTrendingSummaryServiceTest,AdminGitHubTrendingApiIntegrationTest test
```

Expected: PASS.

- [ ] **Step 3: Run frontend build**

Run:

```bash
cd frontend
rtk npm run build
```

Expected: PASS.

- [ ] **Step 4: Optionally run full backend tests if time allows**

Run:

```bash
cd backend
rtk mvn test
```

Expected: PASS. If unrelated existing tests fail, capture exact failing test names and errors in the final implementation report.

- [ ] **Step 5: Start local app for manual check**

Run backend:

```bash
cd backend
SPRING_PROFILES_ACTIVE=dev rtk mvn spring-boot:run
```

Run frontend in another terminal:

```bash
cd frontend
rtk npm run dev
```

Open `http://127.0.0.1:8888`.

Manual checks:

- Home page loads.
- If trending data exists, right sidebar shows `GitHub Trending` with `周榜 / 月榜` tabs.
- Admin login `admin / admin123` can open `/admin/github-trending`.
- Config loads and can be saved.
- Manual sync button starts a sync and shows status.

- [ ] **Step 6: Commit context and final verification changes**

```bash
rtk git add PROJECT_CONTEXT.md
rtk git commit -m "docs: update context for github trending"
```

---

## Self-Review Notes

- Spec coverage: database storage, weekly/monthly fetching, Chinese summaries, manual preservation, home display, admin config/editing, sync failure behavior, and tests are covered by Tasks 1-7.
- Red-flag scan: no unfinished markers, copy-forward shortcuts, or undefined tasks remain.
- Type consistency: planned names use `GitHubTrendingRepository` for the entity and `GitHubTrendingRepositoryRepository` for the JPA repository to avoid collision with the entity name. Period/status names match across entity, DTO, service, and controller tasks.
