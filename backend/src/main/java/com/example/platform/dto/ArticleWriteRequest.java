package com.example.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ArticleWriteRequest {

    @NotBlank(message = "标题不能为空")
    @Size(max = 300)
    private String title;

    @Size(max = 50000)
    private String content;

    /** 内容类型：RICH_TEXT=富文本HTML，MARKDOWN=原始Markdown */
    private String contentType;

    private String status; // DRAFT, PUBLISHED

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
