package com.example.platform.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "skills")
public class Skill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "clone_command", nullable = false, length = 2000)
    private String cloneCommand;

    @Column(name = "content_md", columnDefinition = "MEDIUMTEXT")
    private String contentMd;

    @Column(name = "source_repository_url", length = 1000)
    private String sourceRepositoryUrl;

    @Column(name = "skill_directory", length = 255)
    private String skillDirectory;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_level", nullable = false, length = 20)
    private AssetLevel assetLevel = AssetLevel.TEAM;

    @Enumerated(EnumType.STRING)
    @Column(name = "lifecycle_status", nullable = false, length = 30)
    private LifecycleStatus lifecycleStatus = LifecycleStatus.CANDIDATE;

    @Enumerated(EnumType.STRING)
    @Column(name = "skill_category", nullable = false, length = 40)
    private SkillCategory skillCategory = SkillCategory.CODING_IMPLEMENTATION;

    @Enumerated(EnumType.STRING)
    @Column(name = "build_priority", nullable = false, length = 10)
    private BuildPriority buildPriority = BuildPriority.P2;

    @Enumerated(EnumType.STRING)
    @Column(name = "creation_source", nullable = false, length = 40)
    private CreationSource creationSource = CreationSource.MANUAL;

    @Column(length = 100)
    private String maintainer;

    @Column(name = "team_name", length = 120)
    private String teamName;

    @Column(length = 50)
    private String version = "1.0.0";

    @Column(name = "applicable_scenarios", columnDefinition = "TEXT")
    private String applicableScenarios;

    @Column(name = "non_applicable_scenarios", columnDefinition = "TEXT")
    private String nonApplicableScenarios;

    @Column(name = "input_requirements", columnDefinition = "TEXT")
    private String inputRequirements;

    @Column(name = "execution_steps", columnDefinition = "MEDIUMTEXT")
    private String executionSteps;

    @Column(name = "output_format", columnDefinition = "TEXT")
    private String outputFormat;

    @Column(name = "validation_method", columnDefinition = "TEXT")
    private String validationMethod;

    @Column(name = "quality_standard", columnDefinition = "TEXT")
    private String qualityStandard;

    @Column(name = "reference_materials", columnDefinition = "TEXT")
    private String referenceMaterials;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 20)
    private RiskLevel riskLevel = RiskLevel.LOW;

    @Enumerated(EnumType.STRING)
    @Column(name = "template_validation_status", nullable = false, length = 20)
    private TemplateValidationStatus templateValidationStatus = TemplateValidationStatus.UNVALIDATED;

    @Column(name = "template_validation_notes", columnDefinition = "TEXT")
    private String templateValidationNotes;

    @Column(name = "last_validated_at")
    private Instant lastValidatedAt;

    @Column(name = "review_notes", columnDefinition = "TEXT")
    private String reviewNotes;

    @Column(name = "trial_started_at")
    private LocalDate trialStartedAt;

    @Column(name = "trial_ends_at")
    private LocalDate trialEndsAt;

    @Column(name = "last_reviewed_at")
    private LocalDate lastReviewedAt;

    @Column(name = "next_review_at")
    private LocalDate nextReviewAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploader_id", nullable = false)
    private User uploader;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "skill_tags",
            joinColumns = @JoinColumn(name = "skill_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private List<Tag> tags = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Visibility visibility = Visibility.VISIBLE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public enum Visibility { VISIBLE, HIDDEN }

    @PreUpdate
    public void preUpdate() { this.updatedAt = Instant.now(); }

    public enum AssetLevel { TEAM, COMPANY }
    public enum LifecycleStatus { CANDIDATE, TRIAL, REVIEWING, APPROVED, NEEDS_REVIEW, ARCHIVED }
    public enum SkillCategory { REQUIREMENT_ANALYSIS, ARCHITECTURE_DESIGN, CODING_IMPLEMENTATION, TESTING_VALIDATION, CODE_REVIEW, OPS_TROUBLESHOOTING, DOCUMENTATION_KNOWLEDGE }
    public enum BuildPriority { P0, P1, P2 }
    public enum CreationSource { MANUAL, SKILL_CREATOR_PACKAGE, REPOSITORY_SYNC, SEED }
    public enum RiskLevel { LOW, MEDIUM, HIGH }
    public enum TemplateValidationStatus { UNVALIDATED, PASSED, FAILED }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCloneCommand() { return cloneCommand; }
    public void setCloneCommand(String cloneCommand) { this.cloneCommand = cloneCommand; }
    public String getContentMd() { return contentMd; }
    public void setContentMd(String contentMd) { this.contentMd = contentMd; }
    public String getSourceRepositoryUrl() { return sourceRepositoryUrl; }
    public void setSourceRepositoryUrl(String sourceRepositoryUrl) { this.sourceRepositoryUrl = sourceRepositoryUrl; }
    public String getSkillDirectory() { return skillDirectory; }
    public void setSkillDirectory(String skillDirectory) { this.skillDirectory = skillDirectory; }
    public AssetLevel getAssetLevel() { return assetLevel; }
    public void setAssetLevel(AssetLevel assetLevel) { this.assetLevel = assetLevel; }
    public LifecycleStatus getLifecycleStatus() { return lifecycleStatus; }
    public void setLifecycleStatus(LifecycleStatus lifecycleStatus) { this.lifecycleStatus = lifecycleStatus; }
    public SkillCategory getSkillCategory() { return skillCategory; }
    public void setSkillCategory(SkillCategory skillCategory) { this.skillCategory = skillCategory; }
    public BuildPriority getBuildPriority() { return buildPriority; }
    public void setBuildPriority(BuildPriority buildPriority) { this.buildPriority = buildPriority; }
    public CreationSource getCreationSource() { return creationSource; }
    public void setCreationSource(CreationSource creationSource) { this.creationSource = creationSource; }
    public String getMaintainer() { return maintainer; }
    public void setMaintainer(String maintainer) { this.maintainer = maintainer; }
    public String getTeamName() { return teamName; }
    public void setTeamName(String teamName) { this.teamName = teamName; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getApplicableScenarios() { return applicableScenarios; }
    public void setApplicableScenarios(String applicableScenarios) { this.applicableScenarios = applicableScenarios; }
    public String getNonApplicableScenarios() { return nonApplicableScenarios; }
    public void setNonApplicableScenarios(String nonApplicableScenarios) { this.nonApplicableScenarios = nonApplicableScenarios; }
    public String getInputRequirements() { return inputRequirements; }
    public void setInputRequirements(String inputRequirements) { this.inputRequirements = inputRequirements; }
    public String getExecutionSteps() { return executionSteps; }
    public void setExecutionSteps(String executionSteps) { this.executionSteps = executionSteps; }
    public String getOutputFormat() { return outputFormat; }
    public void setOutputFormat(String outputFormat) { this.outputFormat = outputFormat; }
    public String getValidationMethod() { return validationMethod; }
    public void setValidationMethod(String validationMethod) { this.validationMethod = validationMethod; }
    public String getQualityStandard() { return qualityStandard; }
    public void setQualityStandard(String qualityStandard) { this.qualityStandard = qualityStandard; }
    public String getReferenceMaterials() { return referenceMaterials; }
    public void setReferenceMaterials(String referenceMaterials) { this.referenceMaterials = referenceMaterials; }
    public RiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(RiskLevel riskLevel) { this.riskLevel = riskLevel; }
    public TemplateValidationStatus getTemplateValidationStatus() { return templateValidationStatus; }
    public void setTemplateValidationStatus(TemplateValidationStatus templateValidationStatus) { this.templateValidationStatus = templateValidationStatus; }
    public String getTemplateValidationNotes() { return templateValidationNotes; }
    public void setTemplateValidationNotes(String templateValidationNotes) { this.templateValidationNotes = templateValidationNotes; }
    public Instant getLastValidatedAt() { return lastValidatedAt; }
    public void setLastValidatedAt(Instant lastValidatedAt) { this.lastValidatedAt = lastValidatedAt; }
    public String getReviewNotes() { return reviewNotes; }
    public void setReviewNotes(String reviewNotes) { this.reviewNotes = reviewNotes; }
    public LocalDate getTrialStartedAt() { return trialStartedAt; }
    public void setTrialStartedAt(LocalDate trialStartedAt) { this.trialStartedAt = trialStartedAt; }
    public LocalDate getTrialEndsAt() { return trialEndsAt; }
    public void setTrialEndsAt(LocalDate trialEndsAt) { this.trialEndsAt = trialEndsAt; }
    public LocalDate getLastReviewedAt() { return lastReviewedAt; }
    public void setLastReviewedAt(LocalDate lastReviewedAt) { this.lastReviewedAt = lastReviewedAt; }
    public LocalDate getNextReviewAt() { return nextReviewAt; }
    public void setNextReviewAt(LocalDate nextReviewAt) { this.nextReviewAt = nextReviewAt; }
    public User getUploader() { return uploader; }
    public void setUploader(User uploader) { this.uploader = uploader; }
    public List<Tag> getTags() { return tags; }
    public void setTags(List<Tag> tags) { this.tags = tags != null ? tags : new ArrayList<>(); }
    public Visibility getVisibility() { return visibility; }
    public void setVisibility(Visibility visibility) { this.visibility = visibility; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
