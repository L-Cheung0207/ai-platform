package com.example.platform.service;

import com.example.platform.entity.GitHubTrendingEntry;
import org.springframework.stereotype.Service;

@Service
public class GitHubTrendingSummaryService {

    private static final int DESCRIPTION_LIMIT = 360;

    public void applyFallbackSummary(GitHubTrendingEntry entry) {
        String description = normalize(entry.getDescription());
        if (description == null) {
            entry.setEffectCn("GitHub Trending 未提供项目描述，暂无法可靠判断项目作用。");
            entry.setScenarioCn("请管理员复核项目 README、仓库活跃度和适用场景后补充中文摘要。");
        } else {
            entry.setEffectCn("根据 GitHub 描述，该项目可能用于：" + trimDescription(description) + "。该摘要为保守兜底生成，需人工确认。");
            entry.setScenarioCn("建议管理员结合 README、示例代码和 issue 活跃度复核后，再确认适用场景。");
        }
        entry.setSummaryStatus(GitHubTrendingEntry.SummaryStatus.NEEDS_REVIEW);
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
}
