package com.example.platform.dto;

import com.example.platform.entity.SkillReview;
import com.example.platform.entity.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminUserCreateRequest(
        @NotBlank(message = "用户名不能为空")
        @Size(min = 1, max = 100)
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(min = 6, message = "密码至少6位")
        String password,

        User.Role role,

        SkillReview.ReviewerRole skillGovernanceRole
) {}
