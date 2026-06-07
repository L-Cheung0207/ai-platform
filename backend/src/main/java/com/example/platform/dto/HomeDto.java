package com.example.platform.dto;

import com.example.platform.entity.News;

import java.util.List;

public class HomeDto {

    private List<SkillDto> latestSkills;
    private List<ExternalSkillDto> latestExternalSkills;
    private List<ArticleDto> latestArticles;
    private List<News> latestNews;
    private List<LlmLeaderboardEntryDto> latestLlmLeaderboard;

    public List<SkillDto> getLatestSkills() { return latestSkills; }
    public void setLatestSkills(List<SkillDto> latestSkills) { this.latestSkills = latestSkills; }
    public List<ExternalSkillDto> getLatestExternalSkills() { return latestExternalSkills; }
    public void setLatestExternalSkills(List<ExternalSkillDto> latestExternalSkills) { this.latestExternalSkills = latestExternalSkills; }
    public List<ArticleDto> getLatestArticles() { return latestArticles; }
    public void setLatestArticles(List<ArticleDto> latestArticles) { this.latestArticles = latestArticles; }
    public List<News> getLatestNews() { return latestNews; }
    public void setLatestNews(List<News> latestNews) { this.latestNews = latestNews; }
    public List<LlmLeaderboardEntryDto> getLatestLlmLeaderboard() { return latestLlmLeaderboard; }
    public void setLatestLlmLeaderboard(List<LlmLeaderboardEntryDto> latestLlmLeaderboard) { this.latestLlmLeaderboard = latestLlmLeaderboard; }
}
