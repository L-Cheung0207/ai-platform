package com.example.platform.dto;

import com.example.platform.entity.SkillOperationReport;

import java.time.Instant;

public record SkillOperationReportDto(
        Long reportId,
        String month,
        Instant generatedAt,
        long monthlyUsageCount,
        long monthlyFeedbackCount,
        long monthlyReviewCount,
        double monthlySavedHours,
        double monthlyFeedbackClosedRate,
        double monthlyReviewPassRate,
        String markdown
) {
    public static SkillOperationReportDto fromEntity(SkillOperationReport report) {
        return new SkillOperationReportDto(
                report.getId(),
                report.getReportMonth(),
                report.getGeneratedAt(),
                report.getMonthlyUsageCount(),
                report.getMonthlyFeedbackCount(),
                report.getMonthlyReviewCount(),
                report.getMonthlySavedHours(),
                report.getMonthlyFeedbackClosedRate(),
                report.getMonthlyReviewPassRate(),
                report.getMarkdown()
        );
    }
}
