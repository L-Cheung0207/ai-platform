package com.example.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ArticleWriteRequest {

    @NotBlank(message = "标题不能为空")
    @Size(max = 300)
    private String title;

    @Size(max = 5000)
    private String summary;

    @Size(max = 50000)
    private String content;

    private String status; // DRAFT, PUBLISHED

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
