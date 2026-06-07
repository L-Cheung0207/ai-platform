package com.example.platform.dto;

import com.example.platform.entity.SkillUsageEvent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record SkillUsageRequest(
        @Size(max = 100)
        String userName,

        @Size(max = 20_000)
        String scenario,

        @Min(0)
        @Max(100_000)
        Integer savedMinutes,

        @Min(0)
        @Max(100_000)
        Integer newcomerOnboardingSavedMinutes,

        @Min(0)
        @Max(100_000)
        Integer reviewIssuesBefore,

        @Min(0)
        @Max(100_000)
        Integer reviewIssuesAfter,

        @DecimalMin("0.0")
        @DecimalMax("100.0")
        Double testCoverageBefore,

        @DecimalMin("0.0")
        @DecimalMax("100.0")
        Double testCoverageAfter,

        SkillUsageEvent.ToolchainSource toolchainSource,

        @Size(max = 255)
        String externalEventId,

        @Size(max = 500)
        String repository,

        @Size(max = 255)
        String branchName,

        @Size(max = 100)
        String commitSha,

        @Size(max = 40)
        String ciStatus
) {}
