package com.example.platform.dto;

import java.util.List;

public record SkillMonthlyAwardCandidateDto(
        Long skillId,
        String skillName,
        String assetLevel,
        String lifecycleStatus,
        long usageCount,
        long feedbackCount,
        long successCaseCount,
        long openFeedbackCount,
        double savedHours,
        long score,
        List<String> reasons
) {}
