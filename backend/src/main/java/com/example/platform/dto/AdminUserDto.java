package com.example.platform.dto;

import com.example.platform.entity.User;

import java.time.Instant;

public record AdminUserDto(
        Long id,
        String username,
        String role,
        String skillGovernanceRole,
        Instant createdAt,
        Instant updatedAt
) {
    public static AdminUserDto fromEntity(User user) {
        return new AdminUserDto(
                user.getId(),
                user.getUsername(),
                user.getRole() != null ? user.getRole().name() : null,
                user.getSkillGovernanceRole() != null ? user.getSkillGovernanceRole().name() : null,
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
