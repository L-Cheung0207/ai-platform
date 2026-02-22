package com.example.platform.dto;

import com.example.platform.entity.LearningArticle;

import java.time.Instant;

public class ArticleDto {

    private Long id;
    private String title;
    private String summary;
    private String content;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;

    public static ArticleDto fromEntity(LearningArticle a) {
        ArticleDto dto = new ArticleDto();
        dto.setId(a.getId());
        dto.setTitle(a.getTitle());
        dto.setSummary(a.getSummary());
        dto.setContent(a.getContent());
        dto.setStatus(a.getStatus() != null ? a.getStatus().name() : null);
        dto.setCreatedAt(a.getCreatedAt());
        dto.setUpdatedAt(a.getUpdatedAt());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
