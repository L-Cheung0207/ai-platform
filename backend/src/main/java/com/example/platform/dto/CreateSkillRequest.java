package com.example.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public class CreateSkillRequest {

    @NotBlank(message = "名称不能为空")
    @Size(max = 200)
    private String name;

    @Size(max = 5000)
    private String description;

    private List<String> tags;

    @NotBlank(message = "clone 命令不能为空")
    @Size(max = 2000)
    private String cloneCommand;

    @Size(max = 500_000)
    private String contentMd;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public String getCloneCommand() { return cloneCommand; }
    public void setCloneCommand(String cloneCommand) { this.cloneCommand = cloneCommand; }
    public String getContentMd() { return contentMd; }
    public void setContentMd(String contentMd) { this.contentMd = contentMd; }
}
