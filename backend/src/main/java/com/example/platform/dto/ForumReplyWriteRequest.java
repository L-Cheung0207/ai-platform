package com.example.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ForumReplyWriteRequest {

    @NotBlank(message = "回复内容不能为空")
    @Size(max = 5000, message = "回复内容最多 5000 字符")
    private String content;

    private Long parentReplyId;

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Long getParentReplyId() { return parentReplyId; }
    public void setParentReplyId(Long parentReplyId) { this.parentReplyId = parentReplyId; }
}
