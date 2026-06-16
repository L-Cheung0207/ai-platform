package com.example.platform.service;

import com.example.platform.entity.GitHubTrendingEntry;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
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
    void fetchUsesBuiltUrlTimeoutAndUserAgentWithoutRealNetwork() throws IOException {
        CapturingFetcher fetcher = new CapturingFetcher("""
                <html><body>
                  <article class="Box-row">
                    <h2><a href="/openai/codex"> openai / codex </a></h2>
                    <p>A cloud coding agent.</p>
                    <span itemprop="programmingLanguage">Go</span>
                    <a href="/openai/codex/stargazers"> 9.8k </a>
                    <a href="/openai/codex/forks"> 1,002 </a>
                    <span>456 stars this month</span>
                  </article>
                </body></html>
                """);
        GitHubTrendingScraperService scraper = new GitHubTrendingScraperService(
                fetcher,
                new GitHubTrendingScraperProperties(30_000, 2, "127.0.0.1", 7890)
        );

        List<GitHubTrendingScraperService.TrendingRow> rows = scraper.fetch(
                GitHubTrendingEntry.Period.MONTHLY,
                "C++"
        );

        assertThat(fetcher.url).isEqualTo("https://github.com/trending/c%2B%2B?since=monthly");
        assertThat(fetcher.options.timeoutMillis()).isEqualTo(30_000);
        assertThat(fetcher.options.userAgent()).isEqualTo("ai-guide-style-website/1.0 (+https://github.com/trending)");
        assertThat(fetcher.options.proxyAddress()).contains(new InetSocketAddress("127.0.0.1", 7890));
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).period()).isEqualTo(GitHubTrendingEntry.Period.MONTHLY);
        assertThat(rows.get(0).repoFullName()).isEqualTo("openai/codex");
    }

    @Test
    void fetchRetriesTransientIoFailureBeforeParsingRows() throws IOException {
        FlakyFetcher fetcher = new FlakyFetcher("""
                <html><body>
                  <article class="Box-row">
                    <h2><a href="/openai/codex"> openai / codex </a></h2>
                  </article>
                </body></html>
                """);
        GitHubTrendingScraperService scraper = new GitHubTrendingScraperService(
                fetcher,
                new GitHubTrendingScraperProperties(30_000, 2, null, null)
        );

        List<GitHubTrendingScraperService.TrendingRow> rows = scraper.fetch(
                GitHubTrendingEntry.Period.WEEKLY,
                null
        );

        assertThat(fetcher.calls).isEqualTo(2);
        assertThat(rows).extracting(GitHubTrendingScraperService.TrendingRow::repoFullName)
                .containsExactly("openai/codex");
    }

    @Test
    void parsesTrendingArticleRowsIntoRowsWithoutEntities() {
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

        List<GitHubTrendingScraperService.TrendingRow> rows = service.parseTrendingHtml(
                html,
                GitHubTrendingEntry.Period.WEEKLY
        );

        assertThat(rows).hasSize(2);
        GitHubTrendingScraperService.TrendingRow first = rows.get(0);
        assertThat(first.period()).isEqualTo(GitHubTrendingEntry.Period.WEEKLY);
        assertThat(first.rank()).isEqualTo(1);
        assertThat(first.repoFullName()).isEqualTo("modelcontextprotocol/servers");
        assertThat(first.repoUrl()).isEqualTo("https://github.com/modelcontextprotocol/servers");
        assertThat(first.description()).isEqualTo("Model Context Protocol Servers");
        assertThat(first.language()).isEqualTo("TypeScript");
        assertThat(first.stars()).isEqualTo(61234);
        assertThat(first.forks()).isEqualTo(7890);
        assertThat(first.starsGained()).isEqualTo(1234);

        GitHubTrendingScraperService.TrendingRow second = rows.get(1);
        assertThat(second.rank()).isEqualTo(2);
        assertThat(second.repoFullName()).isEqualTo("openai/codex");
        assertThat(second.stars()).isEqualTo(9800);
        assertThat(second.forks()).isEqualTo(1002);
        assertThat(second.starsGained()).isEqualTo(456);
    }

    private static class CapturingFetcher implements GitHubTrendingScraperService.DocumentFetcher {
        private final String html;
        private String url;
        private GitHubTrendingScraperService.FetchOptions options;

        private CapturingFetcher(String html) {
            this.html = html;
        }

        @Override
        public org.jsoup.nodes.Document fetch(String url, GitHubTrendingScraperService.FetchOptions options) {
            this.url = url;
            this.options = options;
            return Jsoup.parse(html, url);
        }
    }

    private static class FlakyFetcher implements GitHubTrendingScraperService.DocumentFetcher {
        private final String html;
        private int calls;

        private FlakyFetcher(String html) {
            this.html = html;
        }

        @Override
        public org.jsoup.nodes.Document fetch(String url, GitHubTrendingScraperService.FetchOptions options) throws IOException {
            calls++;
            if (calls == 1) {
                throw new IOException("Connect timed out");
            }
            return Jsoup.parse(html, url);
        }
    }
}
