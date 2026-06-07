package com.example.platform.dto;

import java.time.Instant;
import java.util.List;

public record SkillTemplateValidationDto(
        Long skillId,
        String status,
        boolean passed,
        int passedCount,
        int totalCount,
        Instant validatedAt,
        String notes,
        List<SkillTemplateValidationItemDto> items
) {}
