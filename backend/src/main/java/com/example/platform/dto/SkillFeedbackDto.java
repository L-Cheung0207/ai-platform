package com.example.platform.dto;

import com.example.platform.entity.SkillFeedback;

import java.time.Instant;

public record SkillFeedbackDto(
        Long id,
        Long skillId,
        String skillName,
        String submitterName,
        String feedbackType,
        String content,
        int estimatedSavedMinutes,
        Integer rating,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
    public static SkillFeedbackDto fromEntity(SkillFeedback feedback) {
        return new SkillFeedbackDto(
                feedback.getId(),
                feedback.getSkill() != null ? feedback.getSkill().getId() : null,
                feedback.getSkill() != null ? feedback.getSkill().getName() : null,
                feedback.getSubmitterName(),
                feedback.getFeedbackType() != null ? feedback.getFeedbackType().name() : null,
                feedback.getContent(),
                feedback.getEstimatedSavedMinutes(),
                feedback.getRating(),
                feedback.getStatus() != null ? feedback.getStatus().name() : null,
                feedback.getCreatedAt(),
                feedback.getUpdatedAt()
        );
    }
}
