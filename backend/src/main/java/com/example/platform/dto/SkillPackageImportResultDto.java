package com.example.platform.dto;

public record SkillPackageImportResultDto(
        SkillDto skill,
        SkillTemplateValidationDto packageValidation,
        SkillTemplateValidationDto assetValidation,
        SkillGitLabPublishResultDto gitLabPublication
) {}
