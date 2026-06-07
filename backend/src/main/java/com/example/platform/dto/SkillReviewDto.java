package com.example.platform.dto;

import com.example.platform.entity.SkillReview;

import java.time.Instant;
import java.time.LocalDate;

public record SkillReviewDto(
        Long id,
        Long skillId,
        String skillName,
        String reviewerName,
        String reviewerRole,
        String reviewStage,
        String result,
        boolean truthful,
        boolean accurate,
        boolean reusable,
        boolean executable,
        boolean secure,
        boolean verifiable,
        boolean maintainable,
        String notes,
        LocalDate reviewedAt,
        LocalDate nextReviewAt,
        Instant createdAt
) {
    public static SkillReviewDto fromEntity(SkillReview review) {
        return new SkillReviewDto(
                review.getId(),
                review.getSkill() != null ? review.getSkill().getId() : null,
                review.getSkill() != null ? review.getSkill().getName() : null,
                review.getReviewerName(),
                review.getReviewerRole() != null ? review.getReviewerRole().name() : null,
                review.getReviewStage() != null ? review.getReviewStage().name() : null,
                review.getResult() != null ? review.getResult().name() : null,
                review.isTruthful(),
                review.isAccurate(),
                review.isReusable(),
                review.isExecutable(),
                review.isSecure(),
                review.isVerifiable(),
                review.isMaintainable(),
                review.getNotes(),
                review.getReviewedAt(),
                review.getNextReviewAt(),
                review.getCreatedAt()
        );
    }
}
