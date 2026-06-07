package com.example.platform.dto;

import com.example.platform.entity.User;
import jakarta.validation.constraints.NotNull;

public record UserRoleRequest(
        @NotNull(message = "系统角色不能为空")
        User.Role role
) {}
