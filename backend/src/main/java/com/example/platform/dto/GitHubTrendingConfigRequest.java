package com.example.platform.dto;

public class GitHubTrendingConfigRequest {

    private String languageFilter;
    private String keywordFilter;
    private Integer homeDisplayCount;
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
