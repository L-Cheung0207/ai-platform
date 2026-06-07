package com.example.platform.dto;

import com.example.platform.entity.Skill;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public record SkillReviewQueueItemDto(
        Long skillId,
        String skillName,
        String assetLevel,
        String lifecycleStatus,
        String maintainer,
        String teamName,
        String riskLevel,
        String dueType,
        LocalDate dueAt,
        LocalDate trialEndsAt,
        LocalDate nextReviewAt,
        long daysUntilReview,
        boolean overdue
) {
    public static SkillReviewQueueItemDto fromSkill(Skill skill, LocalDate today) {
        LocalDate nextReviewAt = skill.getNextReviewAt();
        LocalDate trialEndsAt = skill.getTrialEndsAt();
        LocalDate dueAt = nextReviewAt;
        String dueType = "REVIEW";
        if (skill.getLifecycleStatus() == Skill.LifecycleStatus.TRIAL
                && trialEndsAt != null
                && (dueAt == null || trialEndsAt.isBefore(dueAt))) {
            dueAt = trialEndsAt;
            dueType = "TRIAL";
        }
        long daysUntilReview = dueAt != null ? ChronoUnit.DAYS.between(today, dueAt) : 0;
        return new SkillReviewQueueItemDto(
                skill.getId(),
                skill.getName(),
                skill.getAssetLevel() != null ? skill.getAssetLevel().name() : null,
                skill.getLifecycleStatus() != null ? skill.getLifecycleStatus().name() : null,
                skill.getMaintainer(),
                skill.getTeamName(),
                skill.getRiskLevel() != null ? skill.getRiskLevel().name() : null,
                dueType,
                dueAt,
                trialEndsAt,
                nextReviewAt,
                daysUntilReview,
                daysUntilReview < 0
        );
    }
}
