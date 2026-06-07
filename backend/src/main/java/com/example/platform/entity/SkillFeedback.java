package com.example.platform.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "skill_feedback")
public class SkillFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    @Column(name = "submitter_name", length = 100)
    private String submitterName;

    @Enumerated(EnumType.STRING)
    @Column(name = "feedback_type", nullable = false, length = 30)
    private FeedbackType feedbackType = FeedbackType.IMPROVEMENT;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "estimated_saved_minutes", nullable = false)
    private int estimatedSavedMinutes;

    @Column
    private Integer rating;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FeedbackStatus status = FeedbackStatus.OPEN;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public enum FeedbackType { SUCCESS_CASE, FAILURE_CASE, IMPROVEMENT, SCOPE_NOTE }
    public enum FeedbackStatus { OPEN, REVIEWED, RESOLVED }

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() { this.updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Skill getSkill() { return skill; }
    public void setSkill(Skill skill) { this.skill = skill; }
    public String getSubmitterName() { return submitterName; }
    public void setSubmitterName(String submitterName) { this.submitterName = submitterName; }
    public FeedbackType getFeedbackType() { return feedbackType; }
    public void setFeedbackType(FeedbackType feedbackType) { this.feedbackType = feedbackType; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public int getEstimatedSavedMinutes() { return estimatedSavedMinutes; }
    public void setEstimatedSavedMinutes(int estimatedSavedMinutes) { this.estimatedSavedMinutes = estimatedSavedMinutes; }
    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
    public FeedbackStatus getStatus() { return status; }
    public void setStatus(FeedbackStatus status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
