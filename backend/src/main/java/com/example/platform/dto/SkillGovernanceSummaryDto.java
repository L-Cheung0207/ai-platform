package com.example.platform.dto;

import java.time.LocalDate;

public record SkillGovernanceSummaryDto(
        Long skillId,
        long usageCount,
        double estimatedSavedHours,
        double newcomerOnboardingSavedHours,
        double reviewIssueReductionRate,
        double testCoverageIncreasePoints,
        long feedbackCount,
        long openFeedbackCount,
        long reviewCount,
        String latestReviewResult,
        LocalDate latestReviewedAt
) {}
