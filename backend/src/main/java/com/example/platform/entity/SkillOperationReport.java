package com.example.platform.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "skill_operation_reports")
public class SkillOperationReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_month", nullable = false, length = 7)
    private String reportMonth;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt = Instant.now();

    @Column(name = "monthly_usage_count", nullable = false)
    private long monthlyUsageCount;

    @Column(name = "monthly_feedback_count", nullable = false)
    private long monthlyFeedbackCount;

    @Column(name = "monthly_review_count", nullable = false)
    private long monthlyReviewCount;

    @Column(name = "monthly_saved_hours", nullable = false)
    private double monthlySavedHours;

    @Column(name = "monthly_feedback_closed_rate", nullable = false)
    private double monthlyFeedbackClosedRate;

    @Column(name = "monthly_review_pass_rate", nullable = false)
    private double monthlyReviewPassRate;

    @Column(columnDefinition = "MEDIUMTEXT")
    private String markdown;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @PrePersist
    public void prePersist() {
        if (generatedAt == null) generatedAt = Instant.now();
        if (createdAt == null) createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getReportMonth() { return reportMonth; }
    public void setReportMonth(String reportMonth) { this.reportMonth = reportMonth; }
    public Instant getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(Instant generatedAt) { this.generatedAt = generatedAt; }
    public long getMonthlyUsageCount() { return monthlyUsageCount; }
    public void setMonthlyUsageCount(long monthlyUsageCount) { this.monthlyUsageCount = monthlyUsageCount; }
    public long getMonthlyFeedbackCount() { return monthlyFeedbackCount; }
    public void setMonthlyFeedbackCount(long monthlyFeedbackCount) { this.monthlyFeedbackCount = monthlyFeedbackCount; }
    public long getMonthlyReviewCount() { return monthlyReviewCount; }
    public void setMonthlyReviewCount(long monthlyReviewCount) { this.monthlyReviewCount = monthlyReviewCount; }
    public double getMonthlySavedHours() { return monthlySavedHours; }
    public void setMonthlySavedHours(double monthlySavedHours) { this.monthlySavedHours = monthlySavedHours; }
    public double getMonthlyFeedbackClosedRate() { return monthlyFeedbackClosedRate; }
    public void setMonthlyFeedbackClosedRate(double monthlyFeedbackClosedRate) { this.monthlyFeedbackClosedRate = monthlyFeedbackClosedRate; }
    public double getMonthlyReviewPassRate() { return monthlyReviewPassRate; }
    public void setMonthlyReviewPassRate(double monthlyReviewPassRate) { this.monthlyReviewPassRate = monthlyReviewPassRate; }
    public String getMarkdown() { return markdown; }
    public void setMarkdown(String markdown) { this.markdown = markdown; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
