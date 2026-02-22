package com.example.platform.dto;

import com.example.platform.entity.ExternalSkill;
import com.example.platform.entity.Tag;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

public class ExternalSkillDto {

    private Long id;
    private String name;
    private String description;
    private String content;
    private String installCommand;
    private String sourceUrl;
    private String visibility;
    private List<String> tagNames;
    private Instant createdAt;
    private Instant updatedAt;

    public static ExternalSkillDto fromEntity(ExternalSkill e) {
        ExternalSkillDto dto = new ExternalSkillDto();
        dto.setId(e.getId());
        dto.setName(e.getName());
        dto.setDescription(e.getDescription());
        dto.setContent(e.getContent());
        dto.setInstallCommand(e.getInstallCommand());
        dto.setSourceUrl(e.getSourceUrl());
        if (e.getTags() != null) {
            dto.setTagNames(e.getTags().stream().map(Tag::getName).collect(Collectors.toList()));
        }
        if (e.getVisibility() != null) {
            dto.setVisibility(e.getVisibility().name());
        }
        dto.setCreatedAt(e.getCreatedAt());
        dto.setUpdatedAt(e.getUpdatedAt());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getInstallCommand() { return installCommand; }
    public void setInstallCommand(String installCommand) { this.installCommand = installCommand; }
    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
    public String getVisibility() { return visibility; }
    public void setVisibility(String visibility) { this.visibility = visibility; }
    public List<String> getTagNames() { return tagNames; }
    public void setTagNames(List<String> tagNames) { this.tagNames = tagNames; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
