package com.example.platform.dto;

import com.example.platform.entity.SkillFeedback;
import jakarta.validation.constraints.NotNull;

public record SkillFeedbackStatusRequest(
        @NotNull(message = "反馈状态不能为空")
        SkillFeedback.FeedbackStatus status
) {}
