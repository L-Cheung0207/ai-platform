package com.example.platform.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(name = "github_trending_repositories",
        uniqueConstraints = @UniqueConstraint(name = "uk_github_trending_period_repo", columnNames = {"period", "repo_full_name"}))
public class GitHubTrendingEntry {

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
