package com.example.platform.dto;

import com.example.platform.entity.Skill;
import com.example.platform.entity.Tag;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

public class SkillDto {

    private Long id;
    private String name;
    private String description;
    private String cloneCommand;
    private String contentMd;
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
