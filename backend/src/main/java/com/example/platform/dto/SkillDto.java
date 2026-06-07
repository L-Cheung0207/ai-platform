package com.example.platform.dto;

import com.example.platform.entity.Skill;
import com.example.platform.entity.Tag;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class SkillDto {

    private Long id;
    private String name;
    private String description;
    private String cloneCommand;
    private String contentMd;
    private String sourceRepositoryUrl;
    private String skillDirectory;
    private String assetLevel;
    private String lifecycleStatus;
    private String skillCategory;
    private String buildPriority;
    private String creationSource;
    private String maintainer;
    private String teamName;
    private String version;
    private String applicableScenarios;
    private String nonApplicableScenarios;
    private String inputRequirements;
    private String executionSteps;
    private String outputFormat;
    private String validationMethod;
    private String qualityStandard;
    private String referenceMaterials;
    private String riskLevel;
    private String templateValidationStatus;
    private String templateValidationNotes;
    private Instant lastValidatedAt;
    private String reviewNotes;
    private LocalDate trialStartedAt;
    private LocalDate trialEndsAt;
    private LocalDate lastReviewedAt;
    private LocalDate nextReviewAt;
    private Long uploaderId;
    private String uploaderName;
    private String visibility;
    private List<String> tagNames;
    private Instant createdAt;
    private Instant updatedAt;

    public static SkillDto fromEntity(Skill s) {
        SkillDto dto = new SkillDto();
        dto.setId(s.getId());
        dto.setName(s.getName());
        dto.setDescription(s.getDescription());
        dto.setCloneCommand(s.getCloneCommand());
        dto.setContentMd(s.getContentMd());
        dto.setSourceRepositoryUrl(s.getSourceRepositoryUrl());
        dto.setSkillDirectory(s.getSkillDirectory());
        if (s.getAssetLevel() != null) {
            dto.setAssetLevel(s.getAssetLevel().name());
        }
        if (s.getLifecycleStatus() != null) {
            dto.setLifecycleStatus(s.getLifecycleStatus().name());
        }
        if (s.getSkillCategory() != null) {
            dto.setSkillCategory(s.getSkillCategory().name());
        }
        if (s.getBuildPriority() != null) {
            dto.setBuildPriority(s.getBuildPriority().name());
        }
        if (s.getCreationSource() != null) {
            dto.setCreationSource(s.getCreationSource().name());
        }
        dto.setMaintainer(s.getMaintainer());
        dto.setTeamName(s.getTeamName());
        dto.setVersion(s.getVersion());
        dto.setApplicableScenarios(s.getApplicableScenarios());
        dto.setNonApplicableScenarios(s.getNonApplicableScenarios());
        dto.setInputRequirements(s.getInputRequirements());
        dto.setExecutionSteps(s.getExecutionSteps());
        dto.setOutputFormat(s.getOutputFormat());
        dto.setValidationMethod(s.getValidationMethod());
        dto.setQualityStandard(s.getQualityStandard());
        dto.setReferenceMaterials(s.getReferenceMaterials());
        if (s.getRiskLevel() != null) {
            dto.setRiskLevel(s.getRiskLevel().name());
        }
        if (s.getTemplateValidationStatus() != null) {
            dto.setTemplateValidationStatus(s.getTemplateValidationStatus().name());
        }
        dto.setTemplateValidationNotes(s.getTemplateValidationNotes());
        dto.setLastValidatedAt(s.getLastValidatedAt());
        dto.setReviewNotes(s.getReviewNotes());
        dto.setTrialStartedAt(s.getTrialStartedAt());
        dto.setTrialEndsAt(s.getTrialEndsAt());
        dto.setLastReviewedAt(s.getLastReviewedAt());
        dto.setNextReviewAt(s.getNextReviewAt());
        if (s.getUploader() != null) {
            dto.setUploaderId(s.getUploader().getId());
            dto.setUploaderName(s.getUploader().getUsername());
        }
        if (s.getTags() != null) {
            dto.setTagNames(s.getTags().stream().map(Tag::getName).collect(Collectors.toList()));
        }
        if (s.getVisibility() != null) {
            dto.setVisibility(s.getVisibility().name());
        }
        dto.setCreatedAt(s.getCreatedAt());
        dto.setUpdatedAt(s.getUpdatedAt());
        return dto;
    }

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
    public String getAssetLevel() { return assetLevel; }
    public void setAssetLevel(String assetLevel) { this.assetLevel = assetLevel; }
    public String getLifecycleStatus() { return lifecycleStatus; }
    public void setLifecycleStatus(String lifecycleStatus) { this.lifecycleStatus = lifecycleStatus; }
    public String getSkillCategory() { return skillCategory; }
    public void setSkillCategory(String skillCategory) { this.skillCategory = skillCategory; }
    public String getBuildPriority() { return buildPriority; }
    public void setBuildPriority(String buildPriority) { this.buildPriority = buildPriority; }
    public String getCreationSource() { return creationSource; }
    public void setCreationSource(String creationSource) { this.creationSource = creationSource; }
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
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public String getTemplateValidationStatus() { return templateValidationStatus; }
    public void setTemplateValidationStatus(String templateValidationStatus) { this.templateValidationStatus = templateValidationStatus; }
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
    public Long getUploaderId() { return uploaderId; }
    public void setUploaderId(Long uploaderId) { this.uploaderId = uploaderId; }
    public String getUploaderName() { return uploaderName; }
    public void setUploaderName(String uploaderName) { this.uploaderName = uploaderName; }
    public String getVisibility() { return visibility; }
    public void setVisibility(String visibility) { this.visibility = visibility; }
    public List<String> getTagNames() { return tagNames; }
    public void setTagNames(List<String> tagNames) { this.tagNames = tagNames; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
