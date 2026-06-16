package com.example.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

public class ForumPostWriteRequest {

    @NotBlank(message = "标题不能为空")
    @Size(max = 300, message = "标题最多 300 字符")
    private String title;

    @NotBlank(message = "正文不能为空")
    private String content;

    @NotNull(message = "帖子类型不能为空")
    private String postType;

    @NotNull(message = "分类不能为空")
    private Long categoryId;

    private List<Long> tagIds = new ArrayList<>();

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getPostType() { return postType; }
    public void setPostType(String postType) { this.postType = postType; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public List<Long> getTagIds() { return tagIds; }
    public void setTagIds(List<Long> tagIds) { this.tagIds = tagIds != null ? tagIds : new ArrayList<>(); }
}
