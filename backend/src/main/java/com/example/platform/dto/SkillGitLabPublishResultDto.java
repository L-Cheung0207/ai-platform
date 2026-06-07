package com.example.platform.dto;

public record SkillGitLabPublishResultDto(
        String status,
        String message,
        String repositoryUrl,
        String branchName,
        String targetBranch,
        String mergeRequestUrl,
        String skillPath,
        String commitId
) {

    public static SkillGitLabPublishResultDto disabled(String message) {
        return new SkillGitLabPublishResultDto(
                "DISABLED",
                message,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    public static SkillGitLabPublishResultDto published(String repositoryUrl,
                                                        String branchName,
                                                        String targetBranch,
                                                        String mergeRequestUrl,
                                                        String skillPath,
                                                        String commitId) {
        return new SkillGitLabPublishResultDto(
                "PUBLISHED",
                "已发布到 GitLab，并创建待审核 Merge Request",
                repositoryUrl,
                branchName,
                targetBranch,
                mergeRequestUrl,
                skillPath,
                commitId
        );
    }

    public static SkillGitLabPublishResultDto deleted(String repositoryUrl,
                                                      String branchName,
                                                      String targetBranch,
                                                      String mergeRequestUrl,
                                                      String skillPath,
                                                      String commitId) {
        return new SkillGitLabPublishResultDto(
                "DELETE_MR_CREATED",
                "已创建 GitLab 删除 Merge Request",
                repositoryUrl,
                branchName,
                targetBranch,
                mergeRequestUrl,
                skillPath,
                commitId
        );
    }
}
