package com.example.platform.dto;

import com.example.platform.entity.SkillReview;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record SkillReviewRequest(
        @Size(max = 100)
        String reviewerName,

        SkillReview.ReviewerRole reviewerRole,

        SkillReview.ReviewStage reviewStage,

        @NotNull(message = "评审结果不能为空")
        SkillReview.ReviewResult result,

        Boolean truthful,
        Boolean accurate,
        Boolean reusable,
        Boolean executable,
        Boolean secure,
        Boolean verifiable,
        Boolean maintainable,

        @Size(max = 20_000)
        String notes,

        LocalDate reviewedAt,
        LocalDate nextReviewAt
) {}
