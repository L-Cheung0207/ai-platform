package com.example.platform.dto;

import com.example.platform.entity.GitHubTrendingEntry;

public class GitHubTrendingUpdateRequest {

    private String effectCn;
    private String scenarioCn;
    private GitHubTrendingEntry.SummaryStatus summaryStatus;

    public String getEffectCn() { return effectCn; }
    public void setEffectCn(String effectCn) { this.effectCn = effectCn; }
    public String getScenarioCn() { return scenarioCn; }
    public void setScenarioCn(String scenarioCn) { this.scenarioCn = scenarioCn; }
    public GitHubTrendingEntry.SummaryStatus getSummaryStatus() { return summaryStatus; }
    public void setSummaryStatus(GitHubTrendingEntry.SummaryStatus summaryStatus) { this.summaryStatus = summaryStatus; }
}
