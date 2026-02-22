package com.example.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public class ExternalSkillWriteRequest {

    @NotBlank(message = "名称不能为空")
    @Size(max = 200)
    private String name;

    @Size(max = 5000)
    private String description;

    @Size(max = 50000)
    private String content;

    @NotBlank(message = "安装命令不能为空")
    @Size(max = 500)
    private String installCommand;

    @Size(max = 500)
    private String sourceUrl;

    private List<String> tags;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getInstallCommand() { return installCommand; }
    public void setInstallCommand(String installCommand) { this.installCommand = installCommand; }
    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
}
