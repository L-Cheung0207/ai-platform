package com.example.platform.dto;

import com.example.platform.entity.SkillReview;
import com.example.platform.entity.User;
import jakarta.validation.constraints.NotNull;

public record AdminUserUpdateRequest(
        @NotNull(message = "系统角色不能为空")
        User.Role role,

        @NotNull(message = "治理角色不能为空")
        SkillReview.ReviewerRole skillGovernanceRole
) {}
