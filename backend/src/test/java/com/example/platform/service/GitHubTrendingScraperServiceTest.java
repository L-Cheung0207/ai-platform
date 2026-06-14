package com.example.platform.service;

import com.example.platform.entity.GitHubTrendingEntry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GitHubTrendingScraperServiceTest {

    private final GitHubTrendingScraperService service = new GitHubTrendingScraperService();

    @Test
    void buildsWeeklyAndMonthlyUrlsWithLanguageFilter() {
        assertThat(service.buildTrendingUrl(GitHubTrendingEntry.Period.WEEKLY, "TypeScript"))
                .isEqualTo("https://github.com/trending/typescript?since=weekly");
        assertThat(service.buildTrendingUrl(GitHubTrendingEntry.Period.MONTHLY, "C++"))
                .isEqualTo("https://github.com/trending/c%2B%2B?since=monthly");
        assertThat(service.buildTrendingUrl(GitHubTrendingEntry.Period.WEEKLY, null))
                .isEqualTo("https://github.com/trending?since=weekly");
    }

    @Test
    void parsesTrendingArticleRowsIntoEntries() {
        String html = """
                <html><body>
                  <article class="Box-row">
                    <h2>
                      <a href="/modelcontextprotocol/servers">
                        <span>modelcontextprotocol</span> / servers
                      </a>
                    </h2>
                    <p class="col-9 color-fg-muted my-1 pr-4">Model Context Protocol Servers</p>
                    <span itemprop="programmingLanguage">TypeScript</span>
                    <a href="/modelcontextprotocol/servers/stargazers"> 61,234 </a>
                    <a href="/modelcontextprotocol/servers/forks"> 7,890 </a>
                    <span class="d-inline-block float-sm-right">1,234 stars this week</span>
                  </article>
                  <article class="Box-row">
                    <h2>
                      <a href="/openai/codex"> openai / codex </a>
                    </h2>
                    <p>A cloud coding agent.</p>
                    <span itemprop="programmingLanguage">Go</span>
                    <a href="/openai/codex/stargazers"> 9.8k </a>
                    <a href="/openai/codex/forks"> 1,002 </a>
                    <span>456 stars this month</span>
                  </article>
                </body></html>
                """;

        List<GitHubTrendingEntry> entries = service.parseTrendingHtml(html, GitHubTrendingEntry.Period.WEEKLY);

        assertThat(entries).hasSize(2);
        GitHubTrendingEntry first = entries.get(0);
        assertThat(first.getPeriod()).isEqualTo(GitHubTrendingEntry.Period.WEEKLY);
        assertThat(first.getRank()).isEqualTo(1);
        assertThat(first.getRepoFullName()).isEqualTo("modelcontextprotocol/servers");
        assertThat(first.getRepoUrl()).isEqualTo("https://github.com/modelcontextprotocol/servers");
        assertThat(first.getDescription()).isEqualTo("Model Context Protocol Servers");
        assertThat(first.getLanguage()).isEqualTo("TypeScript");
        assertThat(first.getStars()).isEqualTo(61234);
        assertThat(first.getForks()).isEqualTo(7890);
        assertThat(first.getStarsGained()).isEqualTo(1234);

        GitHubTrendingEntry second = entries.get(1);
        assertThat(second.getRank()).isEqualTo(2);
        assertThat(second.getRepoFullName()).isEqualTo("openai/codex");
        assertThat(second.getStars()).isEqualTo(9800);
        assertThat(second.getForks()).isEqualTo(1002);
        assertThat(second.getStarsGained()).isEqualTo(456);
    }
}
