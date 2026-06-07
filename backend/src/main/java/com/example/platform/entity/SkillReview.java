package com.example.platform.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "skill_reviews")
public class SkillReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    @Column(name = "reviewer_name", nullable = false, length = 100)
    private String reviewerName;

    @Enumerated(EnumType.STRING)
    @Column(name = "reviewer_role", nullable = false, length = 40)
    private ReviewerRole reviewerRole = ReviewerRole.TECH_LEAD;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_stage", nullable = false, length = 30)
    private ReviewStage reviewStage = ReviewStage.TEAM_REVIEW;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReviewResult result;

    @Column(nullable = false)
    private boolean truthful;

    @Column(nullable = false)
    private boolean accurate;

    @Column(nullable = false)
    private boolean reusable;

    @Column(nullable = false)
    private boolean executable;

    @Column(nullable = false)
    private boolean secure;

    @Column(nullable = false)
    private boolean verifiable;

    @Column(nullable = false)
    private boolean maintainable;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "reviewed_at", nullable = false)
    private LocalDate reviewedAt = LocalDate.now();

    @Column(name = "next_review_at")
    private LocalDate nextReviewAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public enum ReviewStage { TEAM_REVIEW, COMPANY_REVIEW, PERIODIC_REVIEW }
    public enum ReviewResult { PASSED, NEEDS_CHANGES, REJECTED }
    public enum ReviewerRole { CONTRIBUTOR, TECH_LEAD, PLATFORM_ENGINEERING, TECHNICAL_COMMITTEE, SECURITY_QUALITY }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (reviewedAt == null) reviewedAt = LocalDate.now();
        if (reviewerRole == null) reviewerRole = ReviewerRole.CONTRIBUTOR;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Skill getSkill() { return skill; }
    public void setSkill(Skill skill) { this.skill = skill; }
    public String getReviewerName() { return reviewerName; }
    public void setReviewerName(String reviewerName) { this.reviewerName = reviewerName; }
    public ReviewerRole getReviewerRole() { return reviewerRole; }
    public void setReviewerRole(ReviewerRole reviewerRole) { this.reviewerRole = reviewerRole; }
    public ReviewStage getReviewStage() { return reviewStage; }
    public void setReviewStage(ReviewStage reviewStage) { this.reviewStage = reviewStage; }
    public ReviewResult getResult() { return result; }
    public void setResult(ReviewResult result) { this.result = result; }
    public boolean isTruthful() { return truthful; }
    public void setTruthful(boolean truthful) { this.truthful = truthful; }
    public boolean isAccurate() { return accurate; }
    public void setAccurate(boolean accurate) { this.accurate = accurate; }
    public boolean isReusable() { return reusable; }
    public void setReusable(boolean reusable) { this.reusable = reusable; }
    public boolean isExecutable() { return executable; }
    public void setExecutable(boolean executable) { this.executable = executable; }
    public boolean isSecure() { return secure; }
    public void setSecure(boolean secure) { this.secure = secure; }
    public boolean isVerifiable() { return verifiable; }
    public void setVerifiable(boolean verifiable) { this.verifiable = verifiable; }
    public boolean isMaintainable() { return maintainable; }
    public void setMaintainable(boolean maintainable) { this.maintainable = maintainable; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDate getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDate reviewedAt) { this.reviewedAt = reviewedAt; }
    public LocalDate getNextReviewAt() { return nextReviewAt; }
    public void setNextReviewAt(LocalDate nextReviewAt) { this.nextReviewAt = nextReviewAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
