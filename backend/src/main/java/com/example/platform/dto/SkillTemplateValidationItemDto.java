package com.example.platform.dto;

public record SkillTemplateValidationItemDto(
        String key,
        String label,
        boolean required,
        boolean passed,
        String message
) {}
