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
