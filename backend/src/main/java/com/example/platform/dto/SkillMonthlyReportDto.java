package com.example.platform.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record SkillMonthlyReportDto(
        Long reportId,
        String month,
        Instant generatedAt,
        SkillAssetMetricsDto metricsSnapshot,
        long monthlyUsageCount,
        long monthlyFeedbackCount,
        long monthlyReviewCount,
        double monthlySavedHours,
        double monthlyNewcomerOnboardingSavedHours,
        double monthlyReviewIssueReductionRate,
        double monthlyTestCoverageIncreasePoints,
        double monthlyFeedbackClosedRate,
        double monthlyReviewPassRate,
        List<SkillLeaderboardItemDto> topSkills,
        List<SkillMonthlyAwardCandidateDto> monthlyAwardCandidates,
        List<SkillPilotMilestoneDto> pilotMilestones,
        Map<String, Long> monthlyReviewRoleCounts,
        List<String> highlights,
        List<String> risks,
        List<String> recommendations,
        String markdown
) {}
