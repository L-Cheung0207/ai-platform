package com.example.platform.dto;

import com.example.platform.entity.McpServer;

import java.time.Instant;

public class McpServerDto {

    private Long id;
    private String name;
    private String summary;
    private String content;
    private String description;
    private String url;
    private String logoUrl;
    private String tagNames;
    private Instant createdAt;

    public static McpServerDto fromEntity(McpServer t) {
        McpServerDto dto = new McpServerDto();
        dto.setId(t.getId());
        dto.setName(t.getName());
        dto.setSummary(t.getSummary());
        dto.setContent(t.getContent());
        dto.setDescription(t.getDescription());
        dto.setUrl(t.getUrl());
        dto.setLogoUrl(t.getLogoUrl());
        dto.setTagNames(t.getTagNames());
        dto.setCreatedAt(t.getCreatedAt());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }
    public String getTagNames() { return tagNames; }
    public void setTagNames(String tagNames) { this.tagNames = tagNames; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
