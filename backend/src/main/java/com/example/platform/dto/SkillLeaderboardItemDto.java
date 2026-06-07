package com.example.platform.dto;

public record SkillLeaderboardItemDto(
        Long skillId,
        String skillName,
        String assetLevel,
        String lifecycleStatus,
        long usageCount,
        long feedbackCount,
        long openFeedbackCount,
        double savedHours
) {}
