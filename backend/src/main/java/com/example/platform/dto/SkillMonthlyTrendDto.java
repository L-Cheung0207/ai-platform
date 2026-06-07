package com.example.platform.dto;

public record SkillMonthlyTrendDto(
        String month,
        long usageCount,
        long feedbackCount,
        double savedHours
) {}
