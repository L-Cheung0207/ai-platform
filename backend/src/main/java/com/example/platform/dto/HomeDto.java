package com.example.platform.dto;

import com.example.platform.entity.News;

import java.util.List;
import java.util.Map;

public class HomeDto {

    private List<SkillDto> latestSkills;
    private List<RuleDto> latestRules;
    private List<ExternalSkillDto> latestExternalSkills;
    private List<ArticleDto> latestArticles;
    private List<News> latestNews;
    private List<Map<String, Object>> latestAiTools;
    private List<LlmLeaderboardEntryDto> latestLlmLeaderboard;

    public List<SkillDto> getLatestSkills() { return latestSkills; }
    public void setLatestSkills(List<SkillDto> latestSkills) { this.latestSkills = latestSkills; }
    public List<RuleDto> getLatestRules() { return latestRules; }
    public void setLatestRules(List<RuleDto> latestRules) { this.latestRules = latestRules; }
    public List<ExternalSkillDto> getLatestExternalSkills() { return latestExternalSkills; }
    public void setLatestExternalSkills(List<ExternalSkillDto> latestExternalSkills) { this.latestExternalSkills = latestExternalSkills; }
    public List<ArticleDto> getLatestArticles() { return latestArticles; }
    public void setLatestArticles(List<ArticleDto> latestArticles) { this.latestArticles = latestArticles; }
    public List<News> getLatestNews() { return latestNews; }
    public void setLatestNews(List<News> latestNews) { this.latestNews = latestNews; }
    public List<Map<String, Object>> getLatestAiTools() { return latestAiTools; }
    public void setLatestAiTools(List<Map<String, Object>> latestAiTools) { this.latestAiTools = latestAiTools; }
    public List<LlmLeaderboardEntryDto> getLatestLlmLeaderboard() { return latestLlmLeaderboard; }
    public void setLatestLlmLeaderboard(List<LlmLeaderboardEntryDto> latestLlmLeaderboard) { this.latestLlmLeaderboard = latestLlmLeaderboard; }
}
