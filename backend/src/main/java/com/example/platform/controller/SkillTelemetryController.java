package com.example.platform.controller;

import com.example.platform.dto.ApiResponse;
import com.example.platform.dto.SkillToolchainTelemetryRequest;
import com.example.platform.entity.SkillUsageEvent.ToolchainSource;
import com.example.platform.service.SkillGovernanceService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/integrations/skill-telemetry")
public class SkillTelemetryController {

    private static final String TOKEN_HEADER = "X-Skill-Telemetry-Token";
    private static final String GITLAB_TOKEN_HEADER = "X-Gitlab-Token";
    private static final String SKILL_DIRECTORY_HEADER = "X-Skill-Directory";

    private final SkillGovernanceService skillGovernanceService;
    private final ObjectMapper objectMapper;
    private final String telemetryToken;
    private final String gitlabToken;

    public SkillTelemetryController(SkillGovernanceService skillGovernanceService,
                                    ObjectMapper objectMapper,
                                    @Value("${app.skill.telemetry.token:}") String telemetryToken,
                                    @Value("${app.skill.telemetry.gitlab.token:}") String gitlabToken) {
        this.skillGovernanceService = skillGovernanceService;
        this.objectMapper = objectMapper;
        this.telemetryToken = telemetryToken;
        this.gitlabToken = gitlabToken;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<?>> recordTelemetry(
            @RequestHeader(name = TOKEN_HEADER, required = false) String token,
            @Valid @RequestBody SkillToolchainTelemetryRequest request) {
        if (!StringUtils.hasText(telemetryToken)) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiResponse.fail(503, "工具链遥测 token 未配置"));
        }
        if (!telemetryToken.equals(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.fail(401, "工具链遥测 token 无效"));
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(skillGovernanceService.recordToolchainTelemetry(request)));
    }

    @PostMapping("/gitlab")
    public ResponseEntity<ApiResponse<?>> recordGitLabTelemetry(
            @RequestHeader(name = GITLAB_TOKEN_HEADER, required = false) String token,
            @RequestHeader(name = SKILL_DIRECTORY_HEADER, required = false) String headerSkillDirectory,
            @RequestBody String payload) throws Exception {
        if (!StringUtils.hasText(gitlabToken)) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiResponse.fail(503, "GitLab 遥测 token 未配置"));
        }
        if (!gitlabToken.equals(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.fail(401, "GitLab 遥测 token 无效"));
        }

        JsonNode root = objectMapper.readTree(payload);
        SkillToolchainTelemetryRequest request = mapGitLabPayload(root, headerSkillDirectory);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(skillGovernanceService.recordToolchainTelemetry(request)));
    }

    private SkillToolchainTelemetryRequest mapGitLabPayload(JsonNode root, String headerSkillDirectory) {
        JsonNode attributes = root.path("object_attributes");
        JsonNode project = root.path("project");
        JsonNode user = root.path("user");

        String eventKind = firstText(root, "object_kind", "event_name");
        String repository = firstNonBlank(
                firstText(root, "repository"),
                text(project, "path_with_namespace"),
                text(project, "web_url")
        );
        String branchName = firstNonBlank(
                firstText(root, "branchName", "branch_name", "ref"),
                text(attributes, "ref"),
                text(attributes, "source_branch"),
                firstText(root, "build_ref", "ref")
        );
        String commitSha = firstNonBlank(
                firstText(root, "commitSha", "commit_sha", "sha"),
                text(attributes, "sha"),
                text(attributes, "last_commit_id"),
                text(root.path("commit"), "id")
        );
        String ciStatus = firstNonBlank(
                firstText(root, "ciStatus", "ci_status", "build_status"),
                text(attributes, "status"),
                text(attributes, "state")
        );
        String userName = firstNonBlank(
                firstText(root, "userName", "user_name"),
                text(user, "username"),
                text(user, "name")
        );
        ToolchainSource source = gitLabSource(root, eventKind);
        String externalEventId = firstNonBlank(
                firstText(root, "externalEventId", "external_event_id"),
                gitLabExternalEventId(eventKind, repository, attributes, root, commitSha)
        );
        String scenario = firstNonBlank(
                firstText(root, "scenario"),
                gitLabScenario(eventKind, repository, ciStatus)
        );

        return new SkillToolchainTelemetryRequest(
                firstLong(root, "skillId", "skill_id"),
                firstNonBlank(headerSkillDirectory, firstText(root, "skillDirectory", "skill_directory")),
                userName,
                scenario,
                firstInt(root, "savedMinutes", "saved_minutes"),
                firstInt(root, "newcomerOnboardingSavedMinutes", "newcomer_onboarding_saved_minutes"),
                firstInt(root, "reviewIssuesBefore", "review_issues_before"),
                firstInt(root, "reviewIssuesAfter", "review_issues_after"),
                firstDouble(root, "testCoverageBefore", "test_coverage_before"),
                firstDouble(root, "testCoverageAfter", "test_coverage_after"),
                source,
                externalEventId,
                repository,
                branchName,
                commitSha,
                ciStatus
        );
    }

    private ToolchainSource gitLabSource(JsonNode root, String eventKind) {
        String explicitSource = firstText(root, "toolchainSource", "toolchain_source");
        if (StringUtils.hasText(explicitSource)) {
            return ToolchainSource.valueOf(explicitSource.trim().toUpperCase());
        }
        if ("merge_request".equalsIgnoreCase(eventKind)) {
            return ToolchainSource.CODE_REVIEW;
        }
        return ToolchainSource.CI;
    }

    private String gitLabExternalEventId(String eventKind, String repository, JsonNode attributes, JsonNode root, String commitSha) {
        String eventId = firstNonBlank(
                text(attributes, "id"),
                text(attributes, "iid"),
                firstText(root, "build_id"),
                commitSha
        );
        if (!StringUtils.hasText(repository) || !StringUtils.hasText(eventId)) {
            return null;
        }
        String kind = StringUtils.hasText(eventKind) ? eventKind : "event";
        return "gitlab:" + repository + ":" + kind + ":" + eventId;
    }

    private String gitLabScenario(String eventKind, String repository, String status) {
        String kind = StringUtils.hasText(eventKind) ? eventKind : "event";
        String target = StringUtils.hasText(repository) ? repository : "unknown repository";
        String suffix = StringUtils.hasText(status) ? " -> " + status : "";
        return "GitLab " + kind + " telemetry for " + target + suffix;
    }

    private String firstText(JsonNode node, String... fieldNames) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        for (String fieldName : fieldNames) {
            String value = text(node, fieldName);
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private String text(JsonNode node, String fieldName) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        JsonNode value = node.path(fieldName);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        if (value.isTextual()) {
            return value.asText();
        }
        if (value.isNumber() || value.isBoolean()) {
            return value.asText();
        }
        return null;
    }

    private Long firstLong(JsonNode node, String... fieldNames) {
        String value = firstText(node, fieldNames);
        return StringUtils.hasText(value) ? Long.valueOf(value) : null;
    }

    private Integer firstInt(JsonNode node, String... fieldNames) {
        String value = firstText(node, fieldNames);
        return StringUtils.hasText(value) ? Integer.valueOf(value) : null;
    }

    private Double firstDouble(JsonNode node, String... fieldNames) {
        String value = firstText(node, fieldNames);
        return StringUtils.hasText(value) ? Double.valueOf(value) : null;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }
}
