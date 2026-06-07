package com.example.platform.dto;

import java.util.List;

public record SkillPilotMilestoneDto(
        String phase,
        String period,
        String keyAction,
        String deliverable,
        String status,
        int progressPercent,
        List<String> evidence,
        List<String> nextActions
) {}
