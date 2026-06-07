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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ContributorSkillApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void mySkillEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/skills/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    void contributorCanManageOnlyOwnSkillSubmissions() throws Exception {
        String token = contributorToken();
        String name = "Contributor Runtime Skill " + System.nanoTime();

        MvcResult createResult = mockMvc.perform(post("/api/skills/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(skillRequest(name, "CANDIDATE"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value(name))
                .andExpect(jsonPath("$.data.lifecycleStatus").value("CANDIDATE"))
                .andExpect(jsonPath("$.data.creationSource").value("MANUAL"))
                .andReturn();
        long skillId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data")
                .path("id")
                .asLong();

        mockMvc.perform(get("/api/skills/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].id").value(skillId))
                .andExpect(jsonPath("$.data.items[0].uploaderName").exists());

        Map<String, Object> update = skillRequest(name + " Updated", "REVIEWING");
        mockMvc.perform(put("/api/skills/me/{id}", skillId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(skillId))
                .andExpect(jsonPath("$.data.name").value(name + " Updated"))
                .andExpect(jsonPath("$.data.lifecycleStatus").value("REVIEWING"));

        mockMvc.perform(delete("/api/skills/me/{id}", skillId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void contributorCannotApproveOwnSkillDirectly() throws Exception {
        mockMvc.perform(post("/api/skills/me")
                        .header("Authorization", "Bearer " + contributorToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(skillRequest("Contributor Bypass Skill", "APPROVED"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("模板校验后的评审通过")));
    }

    private String contributorToken() throws Exception {
        String username = "contributor-" + System.nanoTime();
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", username,
                                "password", "user123456"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.user.role").value("NORMAL"))
                .andExpect(jsonPath("$.data.user.skillGovernanceRole").value("CONTRIBUTOR"))
                .andReturn();
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.path("data").path("token").asText();
    }

    private Map<String, Object> skillRequest(String name, String lifecycleStatus) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("name", name);
        request.put("description", "Contributor self-service skill");
        request.put("tags", List.of("贡献者"));
        request.put("cloneCommand", "git clone https://example.com/contributor-skills.git");
        request.put("contentMd", "# " + name);
        request.put("sourceRepositoryUrl", "https://example.com/contributor-skills");
        request.put("skillDirectory", name.toLowerCase().replaceAll("[^a-z0-9]+", "-"));
        request.put("assetLevel", "TEAM");
        request.put("lifecycleStatus", lifecycleStatus);
        request.put("maintainer", "contributor");
        request.put("teamName", "Contributor Team");
        request.put("version", "1.0.0");
        request.put("applicableScenarios", "Contributor upload flow");
        request.put("nonApplicableScenarios", "Direct approval");
        request.put("inputRequirements", "Skill package or manual fields");
        request.put("executionSteps", "1. Submit through /api/skills/me");
        request.put("outputFormat", "Skill asset");
        request.put("validationMethod", "MockMvc");
        request.put("qualityStandard", "Must enforce ownership");
        request.put("referenceMaterials", "docs/ai-skill-assetization-coverage.md");
        request.put("riskLevel", "LOW");
        return request;
    }
}
