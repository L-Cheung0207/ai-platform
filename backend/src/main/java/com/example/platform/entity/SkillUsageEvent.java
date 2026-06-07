package com.example.platform.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "skill_usage_events")
public class SkillUsageEvent {

    public enum ToolchainSource {
        MANUAL,
        CI,
        CODE_REVIEW,
        TEST_COVERAGE,
        AI_TOOL,
        IDE_PLUGIN,
        REPOSITORY_SYNC
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    @Column(name = "user_name", length = 100)
    private String userName;

    @Column(columnDefinition = "TEXT")
    private String scenario;

    @Column(name = "saved_minutes", nullable = false)
    private int savedMinutes;

    @Column(name = "newcomer_onboarding_saved_minutes", nullable = false)
    private int newcomerOnboardingSavedMinutes;

    @Column(name = "review_issues_before")
    private Integer reviewIssuesBefore;

    @Column(name = "review_issues_after")
    private Integer reviewIssuesAfter;

    @Column(name = "test_coverage_before")
    private Double testCoverageBefore;

    @Column(name = "test_coverage_after")
    private Double testCoverageAfter;

    @Enumerated(EnumType.STRING)
    @Column(name = "toolchain_source", nullable = false, length = 40)
    private ToolchainSource toolchainSource = ToolchainSource.MANUAL;

    @Column(name = "external_event_id", length = 255)
    private String externalEventId;

    @Column(name = "repository", length = 500)
    private String repository;

    @Column(name = "branch_name", length = 255)
    private String branchName;

    @Column(name = "commit_sha", length = 100)
    private String commitSha;

    @Column(name = "ci_status", length = 40)
    private String ciStatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (toolchainSource == null) toolchainSource = ToolchainSource.MANUAL;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Skill getSkill() { return skill; }
    public void setSkill(Skill skill) { this.skill = skill; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getScenario() { return scenario; }
    public void setScenario(String scenario) { this.scenario = scenario; }
    public int getSavedMinutes() { return savedMinutes; }
    public void setSavedMinutes(int savedMinutes) { this.savedMinutes = savedMinutes; }
    public int getNewcomerOnboardingSavedMinutes() { return newcomerOnboardingSavedMinutes; }
    public void setNewcomerOnboardingSavedMinutes(int newcomerOnboardingSavedMinutes) { this.newcomerOnboardingSavedMinutes = newcomerOnboardingSavedMinutes; }
    public Integer getReviewIssuesBefore() { return reviewIssuesBefore; }
    public void setReviewIssuesBefore(Integer reviewIssuesBefore) { this.reviewIssuesBefore = reviewIssuesBefore; }
    public Integer getReviewIssuesAfter() { return reviewIssuesAfter; }
    public void setReviewIssuesAfter(Integer reviewIssuesAfter) { this.reviewIssuesAfter = reviewIssuesAfter; }
    public Double getTestCoverageBefore() { return testCoverageBefore; }
    public void setTestCoverageBefore(Double testCoverageBefore) { this.testCoverageBefore = testCoverageBefore; }
    public Double getTestCoverageAfter() { return testCoverageAfter; }
    public void setTestCoverageAfter(Double testCoverageAfter) { this.testCoverageAfter = testCoverageAfter; }
    public ToolchainSource getToolchainSource() { return toolchainSource; }
    public void setToolchainSource(ToolchainSource toolchainSource) { this.toolchainSource = toolchainSource; }
    public String getExternalEventId() { return externalEventId; }
    public void setExternalEventId(String externalEventId) { this.externalEventId = externalEventId; }
    public String getRepository() { return repository; }
    public void setRepository(String repository) { this.repository = repository; }
    public String getBranchName() { return branchName; }
    public void setBranchName(String branchName) { this.branchName = branchName; }
    public String getCommitSha() { return commitSha; }
    public void setCommitSha(String commitSha) { this.commitSha = commitSha; }
    public String getCiStatus() { return ciStatus; }
    public void setCiStatus(String ciStatus) { this.ciStatus = ciStatus; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
