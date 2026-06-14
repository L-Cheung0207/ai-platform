package com.example.platform.controller;

import com.example.platform.entity.GitHubTrendingConfig;
import com.example.platform.entity.GitHubTrendingEntry;
import com.example.platform.repository.GitHubTrendingConfigRepository;
import com.example.platform.repository.GitHubTrendingEntryRepository;
import com.example.platform.service.GitHubTrendingScraperService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminGitHubTrendingApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private GitHubTrendingEntryRepository entryRepository;

    @Autowired
    private GitHubTrendingConfigRepository configRepository;

    @BeforeEach
    void setUp() {
        entryRepository.deleteAll();
        configRepository.deleteAll();
    }

    @Test
    void adminEndpointsRequireAdminAuthentication() throws Exception {
        mockMvc.perform(get("/api/admin/github-trending/status"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanManageGitHubTrendingDataAndConfig() throws Exception {
        String token = adminToken();

        mockMvc.perform(get("/api/admin/github-trending/status")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("IDLE"));

        mockMvc.perform(put("/api/admin/github-trending/config")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "languageFilter", " Java ",
                                "keywordFilter", "agent",
                                "homeDisplayCount", 12,
                                "refreshCron", "0 0 8 * * *"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.languageFilter").value("Java"))
                .andExpect(jsonPath("$.data.keywordFilter").value("agent"))
                .andExpect(jsonPath("$.data.homeDisplayCount").value(12));

        mockMvc.perform(get("/api/admin/github-trending/config")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.languageFilter").value("Java"))
                .andExpect(jsonPath("$.data.refreshCron").value("0 0 8 * * *"));

        mockMvc.perform(post("/api/admin/github-trending/sync")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.latestWeeklyBatch").exists())
                .andExpect(jsonPath("$.data.latestMonthlyBatch").exists());

        mockMvc.perform(get("/api/admin/github-trending")
                        .param("period", "WEEKLY")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].repoFullName").value("octo/weekly-agent"))
                .andExpect(jsonPath("$.data[0].summaryStatus").value("NEEDS_REVIEW"));

        mockMvc.perform(get("/api/admin/github-trending")
                        .param("period", "MONTHLY")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].repoFullName").value("octo/monthly-agent"));

        GitHubTrendingEntry entry = entryRepository.findByPeriodAndRepoFullName(
                        GitHubTrendingEntry.Period.WEEKLY,
                        "octo/weekly-agent")
                .orElseThrow();

        mockMvc.perform(put("/api/admin/github-trending/{id}", entry.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "effectCn", "用于追踪周榜 AI 项目",
                                "scenarioCn", "适合管理员整理趋势内容"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.effectCn").value("用于追踪周榜 AI 项目"))
                .andExpect(jsonPath("$.data.scenarioCn").value("适合管理员整理趋势内容"))
                .andExpect(jsonPath("$.data.summaryStatus").value("MANUAL"));

        mockMvc.perform(put("/api/admin/github-trending/{id}", entry.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "effectCn", "x".repeat(1001)
                        ))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/admin/github-trending/{id}/regenerate-summary", entry.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.effectCn", containsString("根据 GitHub 描述")))
                .andExpect(jsonPath("$.data.summaryStatus").value("NEEDS_REVIEW"));
    }

    private String adminToken() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"admin123"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.path("data").path("token").asText();
    }

    @TestConfiguration
    static class GitHubTrendingScraperTestConfiguration {

        @Bean
        @Primary
        GitHubTrendingScraperService githubTrendingScraperService() {
            return new GitHubTrendingScraperService() {
                @Override
                public List<TrendingRow> fetch(GitHubTrendingEntry.Period period, String languageFilter) throws IOException {
                    if (period == GitHubTrendingEntry.Period.MONTHLY) {
                        return List.of(new TrendingRow(
                                period,
                                1,
                                "octo/monthly-agent",
                                "https://github.com/octo/monthly-agent",
                                "Monthly AI agent tooling",
                                "Java",
                                2000,
                                120,
                                300
                        ));
                    }
                    return List.of(new TrendingRow(
                            period,
                            1,
                            "octo/weekly-agent",
                            "https://github.com/octo/weekly-agent",
                            "Weekly AI agent tooling",
                            "Java",
                            1000,
                            80,
                            100
                    ));
                }
            };
        }
    }
}
