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

        GitHubTrendingSummaryService.SummaryResult result = service.generate(entry);

        assertThat(result.effectCn()).contains("根据 GitHub 描述");
        assertThat(result.effectCn()).contains("building AI workflow automations");
        assertThat(result.scenarioCn()).contains("owner/repo");
        assertThat(result.scenarioCn()).contains("README");
        assertThat(result.status()).isEqualTo(GitHubTrendingSummaryService.SummaryResultStatus.NEEDS_REVIEW);
        assertThat(entry.getEffectCn()).isNull();
        assertThat(entry.getScenarioCn()).isNull();
    }

    @Test
    void asksAdminToReviewWhenDescriptionIsMissing() {
        GitHubTrendingEntry entry = new GitHubTrendingEntry();
        entry.setRepoFullName("owner/repo");
        entry.setDescription("  ");

        GitHubTrendingSummaryService.SummaryResult result = service.generateFallback(entry);

        assertThat(result.effectCn()).contains("GitHub Trending 未提供项目描述");
        assertThat(result.effectCn()).contains("owner/repo");
        assertThat(result.scenarioCn()).contains("请管理员复核");
        assertThat(result.status()).isEqualTo(GitHubTrendingSummaryService.SummaryResultStatus.NEEDS_REVIEW);
        assertThat(entry.getEffectCn()).isNull();
        assertThat(entry.getScenarioCn()).isNull();
    }
}
