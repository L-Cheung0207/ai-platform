package com.example.platform.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record SkillArchiveCandidateDto(
        Long skillId,
        String skillName,
        String assetLevel,
        String lifecycleStatus,
        String riskLevel,
        String maintainer,
        String teamName,
        long usageCount,
        Instant lastUsageAt,
        long daysSinceLastUsage,
        long feedbackCount,
        long openFeedbackCount,
        long failureCaseCount,
        LocalDate nextReviewAt,
        long riskScore,
        String recommendedAction,
        List<String> reasons
) {}
