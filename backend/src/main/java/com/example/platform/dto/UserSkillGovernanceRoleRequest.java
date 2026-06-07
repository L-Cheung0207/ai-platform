package com.example.platform.dto;

import com.example.platform.entity.SkillReview;
import jakarta.validation.constraints.NotNull;

public record UserSkillGovernanceRoleRequest(
        @NotNull(message = "治理角色不能为空")
        SkillReview.ReviewerRole skillGovernanceRole
) {}
