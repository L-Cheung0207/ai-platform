package com.example.platform.dto;

import com.example.platform.entity.SkillFeedback;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SkillFeedbackRequest(
        @Size(max = 100)
        String submitterName,

        SkillFeedback.FeedbackType feedbackType,

        @NotBlank(message = "反馈内容不能为空")
        @Size(max = 20_000)
        String content,

        @Min(0)
        @Max(100_000)
        Integer estimatedSavedMinutes,

        @Min(1)
        @Max(5)
        Integer rating
) {}
