package com.example.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public class CreateRuleRequest {

    @NotBlank(message = "名称不能为空")
    @Size(max = 200)
    private String name;

    private List<String> tags;

    @NotBlank(message = "正文不能为空")
    @Size(max = 100_000)
    private String content;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
