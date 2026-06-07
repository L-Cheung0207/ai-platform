package com.example.platform.service;

import com.example.platform.dto.SkillGitLabPublishResultDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class GitLabSkillPublishService {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final String baseUrl;
    private final String token;
    private final String projectId;
    private final String projectWebUrl;
    private final String targetBranch;
    private final String repositoryPath;
    private final String branchPrefix;

    public GitLabSkillPublishService(ObjectMapper objectMapper,
                                     @Value("${app.skill.gitlab.enabled:false}") boolean enabled,
                                     @Value("${app.skill.gitlab.base-url:}") String baseUrl,
                                     @Value("${app.skill.gitlab.token:}") String token,
                                     @Value("${app.skill.gitlab.project-id:}") String projectId,
                                     @Value("${app.skill.gitlab.project-web-url:}") String projectWebUrl,
                                     @Value("${app.skill.gitlab.target-branch:main}") String targetBranch,
                                     @Value("${app.skill.gitlab.repository-path:skills}") String repositoryPath,
                                     @Value("${app.skill.gitlab.branch-prefix:skill}") String branchPrefix) {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.baseUrl = blankToNull(baseUrl);
        this.token = blankToNull(token);
        this.projectId = blankToNull(projectId);
        this.projectWebUrl = blankToNull(projectWebUrl);
        this.targetBranch = StringUtils.hasText(targetBranch) ? targetBranch.trim() : "main";
        this.repositoryPath = trimSlashes(repositoryPath);
        this.branchPrefix = trimSlashes(StringUtils.hasText(branchPrefix) ? branchPrefix : "skill");
    }

    public boolean isPublicationEnabled() {
        return enabled;
    }

    public SkillGitLabPublishResultDto publishPackage(String skillDirectory,
                                                      String skillName,
                                                      Map<String, byte[]> files) {
        if (!enabled) {
            return SkillGitLabPublishResultDto.disabled("GitLab 自动发布未启用，使用包内仓库来源");
        }
        validateConfiguration();
        if (!StringUtils.hasText(skillDirectory)) {
            throw new IllegalArgumentException("GitLab 发布失败：缺少 Skill 目录");
        }
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("GitLab 发布失败：Skill 包没有可提交文件");
        }

        String normalizedDirectory = normalizeSlug(skillDirectory);
        String displayName = StringUtils.hasText(skillName) ? skillName.trim() : normalizedDirectory;
        String branchName = buildBranchName(normalizedDirectory);
        String skillPath = joinPath(repositoryPath, normalizedDirectory);
        JsonNode commit = createCommit(displayName, branchName, skillPath, files);
        JsonNode mergeRequest = createMergeRequest(displayName, branchName, skillPath);

        return SkillGitLabPublishResultDto.published(
                resolveRepositoryUrl(),
                branchName,
                targetBranch,
                text(mergeRequest, "web_url"),
                skillPath,
                text(commit, "id")
        );
    }

    public SkillGitLabPublishResultDto deleteSkillDirectory(String skillDirectory, String skillName) {
        if (!enabled) {
            return SkillGitLabPublishResultDto.disabled("GitLab 自动发布未启用，跳过仓库删除");
        }
        if (!StringUtils.hasText(skillDirectory)) {
            return SkillGitLabPublishResultDto.disabled("Skill 缺少目录，跳过仓库删除");
        }
        validateConfiguration();

        String normalizedDirectory = normalizeSlug(skillDirectory);
        String displayName = StringUtils.hasText(skillName) ? skillName.trim() : normalizedDirectory;
        String skillPath = joinPath(repositoryPath, normalizedDirectory);
        List<String> files = listRepositoryFiles(skillPath);
        if (files.isEmpty()) {
            return SkillGitLabPublishResultDto.disabled("GitLab 未找到 Skill 路径，跳过仓库删除");
        }

        String branchName = buildBranchName("delete-" + normalizedDirectory);
        JsonNode commit = createDeleteCommit(displayName, branchName, skillPath, files);
        JsonNode mergeRequest = createDeleteMergeRequest(displayName, branchName, skillPath);

        return SkillGitLabPublishResultDto.deleted(
                resolveRepositoryUrl(),
                branchName,
                targetBranch,
                text(mergeRequest, "web_url"),
                skillPath,
                text(commit, "id")
        );
    }

    private JsonNode createCommit(String skillName,
                                  String branchName,
                                  String skillPath,
                                  Map<String, byte[]> files) {
        List<Map<String, Object>> actions = new ArrayList<>();
        files.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    String filePath = joinPath(skillPath, normalizeRelativePath(entry.getKey()));
                    Map<String, Object> action = new LinkedHashMap<>();
                    action.put("action", "create");
                    action.put("file_path", filePath);
                    action.put("content", Base64.getEncoder().encodeToString(entry.getValue()));
                    action.put("encoding", "base64");
                    actions.add(action);
                });

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("branch", branchName);
        body.put("start_branch", targetBranch);
        body.put("commit_message", "Add Skill package: " + skillName);
        body.put("actions", actions);
        return post(projectApiPath() + "/repository/commits", body);
    }

    private JsonNode createMergeRequest(String skillName, String branchName, String skillPath) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("source_branch", branchName);
        body.put("target_branch", targetBranch);
        body.put("title", "Add Skill: " + skillName);
        body.put("description", "Platform uploaded Skill package into `" + skillPath + "`.");
        body.put("remove_source_branch", true);
        return post(projectApiPath() + "/merge_requests", body);
    }

    private JsonNode createDeleteCommit(String skillName,
                                        String branchName,
                                        String skillPath,
                                        List<String> files) {
        List<Map<String, Object>> actions = new ArrayList<>();
        files.stream()
                .sorted()
                .forEach(filePath -> {
                    Map<String, Object> action = new LinkedHashMap<>();
                    action.put("action", "delete");
                    action.put("file_path", filePath);
                    actions.add(action);
                });

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("branch", branchName);
        body.put("start_branch", targetBranch);
        body.put("commit_message", "Remove Skill package: " + skillName);
        body.put("actions", actions);
        return post(projectApiPath() + "/repository/commits", body);
    }

    private JsonNode createDeleteMergeRequest(String skillName, String branchName, String skillPath) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("source_branch", branchName);
        body.put("target_branch", targetBranch);
        body.put("title", "Remove Skill: " + skillName);
        body.put("description", "Platform requested removal of Skill package at `" + skillPath + "`.");
        body.put("remove_source_branch", true);
        return post(projectApiPath() + "/merge_requests", body);
    }

    private List<String> listRepositoryFiles(String skillPath) {
        List<String> files = new ArrayList<>();
        int page = 1;
        int perPage = 100;
        while (true) {
            Map<String, String> query = new LinkedHashMap<>();
            query.put("path", skillPath);
            query.put("ref", targetBranch);
            query.put("recursive", "true");
            query.put("per_page", String.valueOf(perPage));
            query.put("page", String.valueOf(page));
            JsonNode root = get(projectApiPath() + "/repository/tree", query);
            if (!root.isArray()) {
                throw new IllegalArgumentException("GitLab 删除失败：仓库目录响应格式异常");
            }
            for (JsonNode item : root) {
                if ("blob".equals(text(item, "type")) && StringUtils.hasText(text(item, "path"))) {
                    files.add(text(item, "path"));
                }
            }
            if (root.size() < perPage) {
                return files;
            }
            page++;
        }
    }

    private JsonNode get(String apiPath, Map<String, String> query) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(apiUri(apiPath, query)).GET();
            applyToken(builder);
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() == 404) {
                return objectMapper.createArrayNode();
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalArgumentException("GitLab 删除失败：" + response.statusCode() + " " + errorMessage(response.body()));
            }
            if (!StringUtils.hasText(response.body())) {
                return objectMapper.createArrayNode();
            }
            return objectMapper.readTree(response.body());
        } catch (IOException ex) {
            throw new IllegalArgumentException("GitLab 删除失败：" + ex.getMessage());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalArgumentException("GitLab 删除被中断");
        }
    }

    private JsonNode post(String apiPath, Map<String, Object> body) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(apiUri(apiPath))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body), StandardCharsets.UTF_8));
            applyToken(builder);
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalArgumentException("GitLab 发布失败：" + response.statusCode() + " " + errorMessage(response.body()));
            }
            if (!StringUtils.hasText(response.body())) {
                return objectMapper.createObjectNode();
            }
            return objectMapper.readTree(response.body());
        } catch (IOException ex) {
            throw new IllegalArgumentException("GitLab 发布失败：" + ex.getMessage());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalArgumentException("GitLab 发布被中断");
        }
    }

    private void applyToken(HttpRequest.Builder builder) {
        if (token.startsWith("Bearer ")) {
            builder.header("Authorization", token);
            return;
        }
        builder.header("PRIVATE-TOKEN", token);
    }

    private URI apiUri(String apiPath) {
        return URI.create(trimTrailingSlash(baseUrl) + "/api/v4" + apiPath);
    }

    private URI apiUri(String apiPath, Map<String, String> query) {
        StringBuilder uri = new StringBuilder(trimTrailingSlash(baseUrl)).append("/api/v4").append(apiPath);
        if (query != null && !query.isEmpty()) {
            uri.append("?");
            boolean first = true;
            for (Map.Entry<String, String> entry : query.entrySet()) {
                if (!first) {
                    uri.append("&");
                }
                first = false;
                uri.append(encodePathSegment(entry.getKey())).append("=").append(encodePathSegment(entry.getValue()));
            }
        }
        return URI.create(uri.toString());
    }

    private String projectApiPath() {
        return "/projects/" + encodePathSegment(projectId);
    }

    private String resolveRepositoryUrl() {
        if (StringUtils.hasText(projectWebUrl)) {
            return projectWebUrl;
        }
        if (StringUtils.hasText(projectId) && !projectId.chars().allMatch(Character::isDigit)) {
            return trimTrailingSlash(baseUrl) + "/" + projectId;
        }
        return null;
    }

    private String buildBranchName(String normalizedDirectory) {
        return joinPath(branchPrefix, normalizedDirectory + "-" + Instant.now().toEpochMilli());
    }

    private void validateConfiguration() {
        List<String> missing = new ArrayList<>();
        if (!StringUtils.hasText(baseUrl)) missing.add("SKILL_GITLAB_BASE_URL");
        if (!StringUtils.hasText(token)) missing.add("SKILL_GITLAB_TOKEN");
        if (!StringUtils.hasText(projectId)) missing.add("SKILL_GITLAB_PROJECT_ID");
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("GitLab 发布配置不完整：" + String.join("、", missing));
        }
    }

    private String errorMessage(String body) {
        if (!StringUtils.hasText(body)) {
            return "无响应内容";
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode message = root.path("message");
            if (message.isMissingNode() || message.isNull()) {
                return body;
            }
            if (message.isTextual()) {
                return message.asText();
            }
            return message.toString();
        } catch (IOException ex) {
            return body;
        }
    }

    private String normalizeRelativePath(String path) {
        String normalized = path == null ? "" : path.replace('\\', '/');
        while (normalized.startsWith("/")) normalized = normalized.substring(1);
        normalized = trimTrailingSlash(normalized);
        if (!StringUtils.hasText(normalized)) {
            throw new IllegalArgumentException("GitLab 发布失败：文件路径为空");
        }
        for (String part : normalized.split("/")) {
            if (part.equals("..") || part.equals(".")) {
                throw new IllegalArgumentException("GitLab 发布失败：非法文件路径 " + path);
            }
        }
        return normalized;
    }

    private String normalizeSlug(String value) {
        String normalized = value.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9._-]+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
        if (!StringUtils.hasText(normalized)) {
            throw new IllegalArgumentException("GitLab 发布失败：Skill 目录只能包含可识别字符");
        }
        return normalized;
    }

    private String joinPath(String left, String right) {
        String a = trimSlashes(left);
        String b = trimSlashes(right);
        if (!StringUtils.hasText(a)) return b;
        if (!StringUtils.hasText(b)) return a;
        return a + "/" + b;
    }

    private String trimSlashes(String value) {
        if (value == null) return "";
        String result = value.trim().replace('\\', '/');
        while (result.startsWith("/")) result = result.substring(1);
        return trimTrailingSlash(result);
    }

    private String trimTrailingSlash(String value) {
        String result = value == null ? "" : value.trim();
        while (result.endsWith("/")) result = result.substring(0, result.length() - 1);
        return result;
    }

    private String encodePathSegment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private String text(JsonNode node, String fieldName) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        JsonNode value = node.path(fieldName);
        return value.isTextual() ? value.asText() : null;
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
