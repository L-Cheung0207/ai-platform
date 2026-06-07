package com.example.platform.dto;

import java.util.List;

public record SkillOperationsDto(
        SkillAssetMetricsDto metrics,
        List<SkillMonthlyTrendDto> monthlyTrends,
        List<SkillLeaderboardItemDto> topSkills,
        List<SkillReviewQueueItemDto> reviewQueue,
        List<SkillMonthlyAwardCandidateDto> monthlyAwardCandidates,
        List<SkillPilotMilestoneDto> pilotMilestones,
        List<SkillArchiveCandidateDto> archiveCandidates
) {}
