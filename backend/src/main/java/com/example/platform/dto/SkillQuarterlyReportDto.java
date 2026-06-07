package com.example.platform.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record SkillQuarterlyReportDto(
        String quarter,
        String startMonth,
        String endMonth,
        Instant generatedAt,
        SkillAssetMetricsDto metricsSnapshot,
        long quarterlyUsageCount,
        long quarterlyFeedbackCount,
        long quarterlyReviewCount,
        double quarterlySavedHours,
        double quarterlyNewcomerOnboardingSavedHours,
        double quarterlyReviewIssueReductionRate,
        double quarterlyTestCoverageIncreasePoints,
        double quarterlyFeedbackClosedRate,
        double quarterlyReviewPassRate,
        List<SkillLeaderboardItemDto> topSkills,
        List<SkillMonthlyAwardCandidateDto> quarterlyAwardCandidates,
        List<SkillArchiveCandidateDto> archiveCandidates,
        Map<String, Long> quarterlyReviewRoleCounts,
        List<String> governanceFindings,
        List<String> risks,
        List<String> recommendations,
        String markdown
) {}
