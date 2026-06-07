package com.example.platform.dto;

import java.util.Map;

public record SkillAssetMetricsDto(
        long totalSkills,
        long teamAssetCount,
        long companyAssetCount,
        long templateValidatedCount,
        long templateValidationFailedCount,
        long approvedCount,
        long needsReviewCount,
        long overdueSkillCount,
        double overdueSkillRate,
        long highRiskCount,
        long monthlyUsageCount,
        long totalUsageCount,
        long manualUsageCount,
        long toolchainUsageCount,
        long ciSignalCount,
        long codeReviewSignalCount,
        long testCoverageSignalCount,
        double estimatedSavedHours,
        double newcomerOnboardingSavedHours,
        double reviewIssueReductionRate,
        double testCoverageIncreasePoints,
        long qualitySignalCount,
        long feedbackCount,
        long openFeedbackCount,
        double feedbackClosedRate,
        long reviewCount,
        double reviewPassRate,
        Map<String, Long> assetLevelCounts,
        Map<String, Long> lifecycleStatusCounts,
        Map<String, Long> skillCategoryCounts,
        Map<String, Long> buildPriorityCounts,
        Map<String, Long> toolchainSourceCounts
) {}
