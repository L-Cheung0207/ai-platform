package com.example.platform.dto;

import com.example.platform.entity.Rule;
import com.example.platform.entity.Tag;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

public class RuleDto {

    private Long id;
    private String name;
    private String content;
    private Long uploaderId;
    private String uploaderName;
    private String visibility;
    private List<String> tagNames;
    private Instant createdAt;
    private Instant updatedAt;

    public static RuleDto fromEntity(Rule r) {
        RuleDto dto = new RuleDto();
        dto.setId(r.getId());
        dto.setName(r.getName());
        dto.setContent(r.getContent());
        if (r.getUploader() != null) {
            dto.setUploaderId(r.getUploader().getId());
            dto.setUploaderName(r.getUploader().getUsername());
        }
        if (r.getTags() != null) {
            dto.setTagNames(r.getTags().stream().map(Tag::getName).collect(Collectors.toList()));
        }
        if (r.getVisibility() != null) {
            dto.setVisibility(r.getVisibility().name());
        }
        dto.setCreatedAt(r.getCreatedAt());
        dto.setUpdatedAt(r.getUpdatedAt());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
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
