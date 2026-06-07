package com.example.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.example.platform.entity.Skill.AssetLevel;
import com.example.platform.entity.Skill.BuildPriority;
import com.example.platform.entity.Skill.LifecycleStatus;
import com.example.platform.entity.Skill.RiskLevel;
import com.example.platform.entity.Skill.SkillCategory;

import java.time.LocalDate;
import java.util.List;

public class CreateSkillRequest {

    @NotBlank(message = "名称不能为空")
    @Size(max = 200)
    private String name;

    @Size(max = 5000)
    private String description;

    private List<String> tags;

    @NotBlank(message = "clone 命令不能为空")
    @Size(max = 2000)
    private String cloneCommand;

    @Size(max = 500_000)
    private String contentMd;

    @Size(max = 1000)
    private String sourceRepositoryUrl;

    @Size(max = 255)
    private String skillDirectory;

    private AssetLevel assetLevel;

    private LifecycleStatus lifecycleStatus;

    private SkillCategory skillCategory;

    private BuildPriority buildPriority;

    @Size(max = 100)
    private String maintainer;

    @Size(max = 120)
    private String teamName;

    @Size(max = 50)
    private String version;

    @Size(max = 20_000)
    private String applicableScenarios;

    @Size(max = 20_000)
    private String nonApplicableScenarios;

    @Size(max = 20_000)
    private String inputRequirements;

    @Size(max = 100_000)
    private String executionSteps;

    @Size(max = 20_000)
    private String outputFormat;

    @Size(max = 20_000)
    private String validationMethod;

    @Size(max = 20_000)
    private String qualityStandard;

    @Size(max = 20_000)
    private String referenceMaterials;

    private RiskLevel riskLevel;

    @Size(max = 20_000)
    private String reviewNotes;

    private LocalDate trialStartedAt;

    private LocalDate trialEndsAt;

    private LocalDate lastReviewedAt;

    private LocalDate nextReviewAt;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
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
}
