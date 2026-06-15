package com.example.platform.service;

import com.example.platform.entity.GitHubTrendingEntry;
import org.springframework.stereotype.Service;

@Service
public class GitHubTrendingSummaryService {

    private static final int DESCRIPTION_LIMIT = 360;

    public enum SummaryResultStatus { GENERATED, NEEDS_REVIEW, FAILED }

    public record SummaryResult(String effectCn, String scenarioCn, SummaryResultStatus status) {}

    public SummaryResult generate(GitHubTrendingEntry entry) {
        return generateFallback(entry);
    }

    public SummaryResult generateFallback(GitHubTrendingEntry entry) {
        String description = normalize(entry.getDescription());
        if (description == null) {
            return new SummaryResult(
                    "GitHub Trending 未提供项目描述，暂无法可靠判断项目 " + repoName(entry) + " 的作用。",
                    "请管理员复核 " + repoName(entry) + " 的 README、仓库活跃度和适用场景后补充中文摘要。",
                    SummaryResultStatus.NEEDS_REVIEW
            );
        }
        return new SummaryResult(
                "根据 GitHub 描述，该项目可能用于：" + trimDescription(description) + "。该摘要为保守兜底生成，需人工确认。",
                "建议管理员结合 " + repoName(entry) + " 的 README、示例代码和 issue 活跃度复核后，再确认适用场景。",
                SummaryResultStatus.NEEDS_REVIEW
        );
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().replaceAll("\\s+", " ");
    }

    private String trimDescription(String description) {
        if (description.length() <= DESCRIPTION_LIMIT) {
            return description;
        }
        return description.substring(0, DESCRIPTION_LIMIT).trim() + "...";
    }

    private String repoName(GitHubTrendingEntry entry) {
        String repoFullName = normalize(entry.getRepoFullName());
        return repoFullName == null ? "该仓库" : repoFullName;
    }
}
