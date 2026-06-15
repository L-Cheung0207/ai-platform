package com.example.platform.dto;

import com.example.platform.entity.GitHubTrendingEntry;

import java.time.Instant;

public class GitHubTrendingDto {

    private Long id;
    private GitHubTrendingEntry.Period period;
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
    private GitHubTrendingEntry.SummaryStatus summaryStatus;
    private Instant sourceFetchedAt;
    private Instant lastSeenBatch;
    private Instant createdAt;
    private Instant updatedAt;

    public static GitHubTrendingDto fromEntity(GitHubTrendingEntry entry) {
        GitHubTrendingDto dto = new GitHubTrendingDto();
        dto.setId(entry.getId());
        dto.setPeriod(entry.getPeriod());
        dto.setRank(entry.getRank());
        dto.setRepoFullName(entry.getRepoFullName());
        dto.setRepoUrl(entry.getRepoUrl());
        dto.setDescription(entry.getDescription());
        dto.setLanguage(entry.getLanguage());
        dto.setStars(entry.getStars());
        dto.setForks(entry.getForks());
        dto.setStarsGained(entry.getStarsGained());
        dto.setEffectCn(entry.getEffectCn());
        dto.setScenarioCn(entry.getScenarioCn());
        dto.setSummaryStatus(entry.getSummaryStatus());
        dto.setSourceFetchedAt(entry.getSourceFetchedAt());
        dto.setLastSeenBatch(entry.getLastSeenBatch());
        dto.setCreatedAt(entry.getCreatedAt());
        dto.setUpdatedAt(entry.getUpdatedAt());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public GitHubTrendingEntry.Period getPeriod() { return period; }
    public void setPeriod(GitHubTrendingEntry.Period period) { this.period = period; }
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
    public GitHubTrendingEntry.SummaryStatus getSummaryStatus() { return summaryStatus; }
    public void setSummaryStatus(GitHubTrendingEntry.SummaryStatus summaryStatus) { this.summaryStatus = summaryStatus; }
    public Instant getSourceFetchedAt() { return sourceFetchedAt; }
    public void setSourceFetchedAt(Instant sourceFetchedAt) { this.sourceFetchedAt = sourceFetchedAt; }
    public Instant getLastSeenBatch() { return lastSeenBatch; }
    public void setLastSeenBatch(Instant lastSeenBatch) { this.lastSeenBatch = lastSeenBatch; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
