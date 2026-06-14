package com.example.platform.service;

import com.example.platform.entity.GitHubTrendingEntry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GitHubTrendingSummaryServiceTest {

    private final GitHubTrendingSummaryService service = new GitHubTrendingSummaryService();

    @Test
    void usesConservativeChineseFallbackWhenDescriptionExists() {
        GitHubTrendingEntry entry = new GitHubTrendingEntry();
        entry.setRepoFullName("owner/repo");
        entry.setDescription("A lightweight framework for building AI workflow automations.");

        service.applyFallbackSummary(entry);

        assertThat(entry.getEffectCn()).contains("根据 GitHub 描述");
        assertThat(entry.getEffectCn()).contains("building AI workflow automations");
        assertThat(entry.getScenarioCn()).contains("建议管理员结合 README");
        assertThat(entry.getSummaryStatus()).isEqualTo(GitHubTrendingEntry.SummaryStatus.NEEDS_REVIEW);
    }

    @Test
    void asksAdminToReviewWhenDescriptionIsMissing() {
        GitHubTrendingEntry entry = new GitHubTrendingEntry();
        entry.setRepoFullName("owner/repo");
        entry.setDescription("  ");

        service.applyFallbackSummary(entry);

        assertThat(entry.getEffectCn()).contains("GitHub Trending 未提供项目描述");
        assertThat(entry.getScenarioCn()).contains("请管理员复核");
        assertThat(entry.getSummaryStatus()).isEqualTo(GitHubTrendingEntry.SummaryStatus.NEEDS_REVIEW);
    }
}
