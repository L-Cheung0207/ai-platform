package com.example.platform.dto;

import com.example.platform.entity.News;

import java.util.List;
import java.time.Instant;

public class HomeDto {

    private List<SkillDto> latestSkills;
    private List<ArticleDto> latestArticles;
    private List<News> latestNews;
    private List<LlmLeaderboardEntryDto> latestLlmLeaderboard;
    private List<GitHubTrendingDto> githubTrendingWeekly;
    private List<GitHubTrendingDto> githubTrendingMonthly;
    private Instant githubTrendingUpdatedAt;

    public List<SkillDto> getLatestSkills() { return latestSkills; }
    public void setLatestSkills(List<SkillDto> latestSkills) { this.latestSkills = latestSkills; }
    public List<ArticleDto> getLatestArticles() { return latestArticles; }
    public void setLatestArticles(List<ArticleDto> latestArticles) { this.latestArticles = latestArticles; }
    public List<News> getLatestNews() { return latestNews; }
    public void setLatestNews(List<News> latestNews) { this.latestNews = latestNews; }
    public List<LlmLeaderboardEntryDto> getLatestLlmLeaderboard() { return latestLlmLeaderboard; }
    public void setLatestLlmLeaderboard(List<LlmLeaderboardEntryDto> latestLlmLeaderboard) { this.latestLlmLeaderboard = latestLlmLeaderboard; }
    public List<GitHubTrendingDto> getGithubTrendingWeekly() { return githubTrendingWeekly; }
    public void setGithubTrendingWeekly(List<GitHubTrendingDto> githubTrendingWeekly) { this.githubTrendingWeekly = githubTrendingWeekly; }
    public List<GitHubTrendingDto> getGithubTrendingMonthly() { return githubTrendingMonthly; }
    public void setGithubTrendingMonthly(List<GitHubTrendingDto> githubTrendingMonthly) { this.githubTrendingMonthly = githubTrendingMonthly; }
    public Instant getGithubTrendingUpdatedAt() { return githubTrendingUpdatedAt; }
    public void setGithubTrendingUpdatedAt(Instant githubTrendingUpdatedAt) { this.githubTrendingUpdatedAt = githubTrendingUpdatedAt; }
}
