package com.example.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ForumCategoryWriteRequest {

    @NotBlank(message = "分类名称不能为空")
    @Size(max = 100, message = "分类名称最多 100 字符")
    private String name;

    @Size(max = 120, message = "Slug 最多 120 字符")
    private String slug;

    private String description;

    @NotNull(message = "排序值不能为空")
    private Integer sortOrder = 0;

    private Boolean enabled = true;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
}
