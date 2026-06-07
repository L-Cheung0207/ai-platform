package com.example.platform.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminSkillApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void skillOperationsRequiresAdminAuthentication() throws Exception {
        mockMvc.perform(get("/api/admin/skill-operations"))
                .andExpect(status().isForbidden());
    }

    @Test
    void skillOperationsReturnsGovernanceCollections() throws Exception {
        mockMvc.perform(get("/api/admin/skill-operations")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.metrics.totalSkills").exists())
                .andExpect(jsonPath("$.data.reviewQueue").isArray())
                .andExpect(jsonPath("$.data.monthlyAwardCandidates").isArray())
                .andExpect(jsonPath("$.data.pilotMilestones").isArray())
                .andExpect(jsonPath("$.data.archiveCandidates").isArray());
    }

    @Test
    void quarterlyReportReturnsGovernanceContract() throws Exception {
        mockMvc.perform(get("/api/admin/skill-operations/quarterly-report")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.quarter").exists())
                .andExpect(jsonPath("$.data.quarterlyUsageCount").exists())
                .andExpect(jsonPath("$.data.governanceFindings").isArray())
                .andExpect(jsonPath("$.data.archiveCandidates").isArray())
                .andExpect(jsonPath("$.data.markdown", containsString("季度治理报告")));
    }

    @Test
    void quarterlyReportRejectsInvalidQuarter() throws Exception {
        mockMvc.perform(get("/api/admin/skill-operations/quarterly-report")
                        .param("quarter", "2026-Q9")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message", containsString("季度格式")));
    }

    @Test
    void createSkillDefaultsCreationSourceToManualThroughApi() throws Exception {
        mockMvc.perform(post("/api/admin/skills")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(skillRequest("API Manual Skill", "CANDIDATE"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.lifecycleStatus").value("CANDIDATE"))
                .andExpect(jsonPath("$.data.creationSource").value("MANUAL"));
    }

    @Test
    void createSkillRejectsManualApprovedLifecycleThroughApi() throws Exception {
        mockMvc.perform(post("/api/admin/skills")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(skillRequest("API Bypass Skill", "APPROVED"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message", containsString("模板校验后的评审通过")));
    }

    @Test
    void publicUsageEndpointAcceptsIdempotentToolchainTelemetry() throws Exception {
        String token = adminToken();
        MvcResult createResult = mockMvc.perform(post("/api/admin/skills")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(skillRequest("Telemetry Skill", "CANDIDATE"))))
                .andExpect(status().isCreated())
                .andReturn();
        long skillId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data")
                .path("id")
                .asLong();

        Map<String, Object> usagePayload = new LinkedHashMap<>();
        usagePayload.put("userName", "ci-bot");
        usagePayload.put("scenario", "PR #128 code review");
        usagePayload.put("savedMinutes", 30);
        usagePayload.put("toolchainSource", "CODE_REVIEW");
        usagePayload.put("externalEventId", "github-pr-128-review-1");
        usagePayload.put("repository", "teleone/checkout");
        usagePayload.put("branchName", "feature/refactor-payment");
        usagePayload.put("commitSha", "abc1234");
        usagePayload.put("ciStatus", "PASSED");
        usagePayload.put("reviewIssuesBefore", 8);
        usagePayload.put("reviewIssuesAfter", 3);
        usagePayload.put("testCoverageBefore", 62.5);
        usagePayload.put("testCoverageAfter", 70.2);

        mockMvc.perform(post("/api/skills/{id}/usage", skillId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(usagePayload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.usageCount").value(1));

        mockMvc.perform(post("/api/skills/{id}/usage", skillId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(usagePayload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.usageCount").value(1));

        mockMvc.perform(get("/api/admin/skill-metrics")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.toolchainUsageCount").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.codeReviewSignalCount").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.toolchainSourceCounts.CODE_REVIEW").value(greaterThanOrEqualTo(1)));
    }

    @Test
    void createReviewUsesAuthenticatedUserGovernanceRole() throws Exception {
        String token = adminToken();
        MvcResult createResult = mockMvc.perform(post("/api/admin/skills")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(skillRequest("Review Role Binding Skill", "CANDIDATE"))))
                .andExpect(status().isCreated())
                .andReturn();
        long skillId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data")
                .path("id")
                .asLong();

        Map<String, Object> reviewPayload = new LinkedHashMap<>();
        reviewPayload.put("reviewerName", "spoofed-reviewer");
        reviewPayload.put("reviewerRole", "TECH_LEAD");
        reviewPayload.put("reviewStage", "TEAM_REVIEW");
        reviewPayload.put("result", "NEEDS_CHANGES");
        reviewPayload.put("truthful", true);
        reviewPayload.put("accurate", true);
        reviewPayload.put("reusable", true);
        reviewPayload.put("executable", true);
        reviewPayload.put("secure", true);
        reviewPayload.put("verifiable", true);
        reviewPayload.put("maintainable", true);
        reviewPayload.put("notes", "server should bind reviewer identity");

        mockMvc.perform(post("/api/admin/skills/{id}/reviews", skillId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reviewPayload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.reviewerName").value("admin"))
                .andExpect(jsonPath("$.data.reviewerRole").value("SECURITY_QUALITY"))
                .andExpect(jsonPath("$.data.result").value("NEEDS_CHANGES"));
    }

    @Test
    void adminCanListAndUpdateUserRoles() throws Exception {
        String token = adminToken();
        String username = "managed-user-api-" + System.nanoTime();
        MvcResult createResult = mockMvc.perform(post("/api/admin/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", username,
                                "password", "user123456"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.username").value(username))
                .andExpect(jsonPath("$.data.role").value("NORMAL"))
                .andExpect(jsonPath("$.data.skillGovernanceRole").value("CONTRIBUTOR"))
                .andReturn();
        long userId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data")
                .path("id")
                .asLong();

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isArray());

        mockMvc.perform(put("/api/admin/users/{id}", userId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"role":"ADMIN","skillGovernanceRole":"PLATFORM_ENGINEERING"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(userId))
                .andExpect(jsonPath("$.data.role").value("ADMIN"))
                .andExpect(jsonPath("$.data.skillGovernanceRole").value("PLATFORM_ENGINEERING"));

        mockMvc.perform(put("/api/admin/users/{id}/role", userId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"role":"ADMIN"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(userId))
                .andExpect(jsonPath("$.data.username").value(username))
                .andExpect(jsonPath("$.data.role").value("ADMIN"));

        mockMvc.perform(put("/api/admin/users/{id}/role", userId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"role":"NORMAL"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(userId))
                .andExpect(jsonPath("$.data.role").value("NORMAL"));

        mockMvc.perform(put("/api/admin/users/{id}/skill-governance-role", userId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"skillGovernanceRole":"PLATFORM_ENGINEERING"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(userId))
                .andExpect(jsonPath("$.data.username").value(username))
                .andExpect(jsonPath("$.data.skillGovernanceRole").value("PLATFORM_ENGINEERING"));

        mockMvc.perform(delete("/api/admin/users/{id}", userId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/admin/users/{id}/role", 1L)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"role":"NORMAL"}
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("至少需要保留一个管理员")));

        mockMvc.perform(delete("/api/admin/users/{id}", 1L)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("不能删除当前登录用户")));
    }

    @Test
    void toolchainTelemetryEndpointRequiresSharedToken() throws Exception {
        mockMvc.perform(post("/api/integrations/skill-telemetry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "skillDirectory", "java-code-review",
                                "toolchainSource", "CI",
                                "externalEventId", "missing-token-build"
                        ))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void toolchainTelemetryEndpointRecordsBySkillDirectoryIdempotently() throws Exception {
        String token = adminToken();
        MvcResult createResult = mockMvc.perform(post("/api/admin/skills")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(skillRequest("Webhook Telemetry Skill", "CANDIDATE"))))
                .andExpect(status().isCreated())
                .andReturn();
        String skillDirectory = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data")
                .path("skillDirectory")
                .asText();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("skillDirectory", skillDirectory);
        payload.put("userName", "jenkins");
        payload.put("scenario", "Jenkins build #44");
        payload.put("savedMinutes", 18);
        payload.put("toolchainSource", "CI");
        payload.put("externalEventId", "jenkins-build-44");
        payload.put("repository", "teleone/platform");
        payload.put("branchName", "main");
        payload.put("commitSha", "def5678");
        payload.put("ciStatus", "PASSED");

        mockMvc.perform(post("/api/integrations/skill-telemetry")
                        .header("X-Skill-Telemetry-Token", "test-skill-telemetry-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.usageCount").value(1));

        mockMvc.perform(post("/api/integrations/skill-telemetry")
                        .header("X-Skill-Telemetry-Token", "test-skill-telemetry-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.usageCount").value(1));
    }

    @Test
    void gitlabTelemetryEndpointRequiresGitlabToken() throws Exception {
        mockMvc.perform(post("/api/integrations/skill-telemetry/gitlab")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "object_kind", "pipeline",
                                "skillDirectory", "java-code-review"
                        ))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void gitlabTelemetryEndpointMapsPipelinePayloadIdempotently() throws Exception {
        String token = adminToken();
        MvcResult createResult = mockMvc.perform(post("/api/admin/skills")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(skillRequest("GitLab Pipeline Skill", "CANDIDATE"))))
                .andExpect(status().isCreated())
                .andReturn();
        String skillDirectory = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data")
                .path("skillDirectory")
                .asText();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("object_kind", "pipeline");
        payload.put("user", Map.of("username", "gitlab-ci"));
        payload.put("project", Map.of(
                "path_with_namespace", "teleone/platform",
                "web_url", "https://gitlab.example.com/teleone/platform"
        ));
        payload.put("object_attributes", Map.of(
                "id", 2201,
                "status", "success",
                "ref", "main",
                "sha", "abc2201"
        ));
        payload.put("savedMinutes", 25);

        mockMvc.perform(post("/api/integrations/skill-telemetry/gitlab")
                        .header("X-Gitlab-Token", "test-skill-gitlab-token")
                        .header("X-Skill-Directory", skillDirectory)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.usageCount").value(1))
                .andExpect(jsonPath("$.data.estimatedSavedHours").value(0.4));

        mockMvc.perform(post("/api/integrations/skill-telemetry/gitlab")
                        .header("X-Gitlab-Token", "test-skill-gitlab-token")
                        .header("X-Skill-Directory", skillDirectory)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.usageCount").value(1));
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

    private Map<String, Object> skillRequest(String name, String lifecycleStatus) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("name", name);
        request.put("description", "API regression skill");
        request.put("tags", List.of());
        request.put("cloneCommand", "git clone https://example.com/skills.git");
        request.put("contentMd", "# " + name);
        request.put("sourceRepositoryUrl", "https://example.com/skills");
        request.put("skillDirectory", name.toLowerCase().replace(" ", "-"));
        request.put("assetLevel", "TEAM");
        request.put("lifecycleStatus", lifecycleStatus);
        request.put("maintainer", "platform");
        request.put("teamName", "Platform");
        request.put("version", "1.0.0");
        request.put("applicableScenarios", "API regression");
        request.put("nonApplicableScenarios", "Manual approval bypass");
        request.put("inputRequirements", "Request body");
        request.put("executionSteps", "1. Submit through API");
        request.put("outputFormat", "JSON");
        request.put("validationMethod", "MockMvc");
        request.put("qualityStandard", "Must enforce governance gates");
        request.put("referenceMaterials", "docs/ai-skill-assetization-coverage.md");
        request.put("riskLevel", "LOW");
        return request;
    }
}
