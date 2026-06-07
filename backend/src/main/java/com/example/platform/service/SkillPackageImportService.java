package com.example.platform.service;

import com.example.platform.dto.CreateSkillRequest;
import com.example.platform.dto.SkillGitLabPublishResultDto;
import com.example.platform.dto.SkillDto;
import com.example.platform.dto.SkillPackageImportResultDto;
import com.example.platform.dto.SkillTemplateValidationDto;
import com.example.platform.dto.SkillTemplateValidationItemDto;
import com.example.platform.entity.Skill.AssetLevel;
import com.example.platform.entity.Skill.BuildPriority;
import com.example.platform.entity.Skill.CreationSource;
import com.example.platform.entity.Skill.LifecycleStatus;
import com.example.platform.entity.Skill.RiskLevel;
import com.example.platform.entity.Skill.SkillCategory;
import com.example.platform.entity.Skill.TemplateValidationStatus;
import com.example.platform.repository.SkillRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class SkillPackageImportService {

    private static final long MAX_ZIP_SIZE = 10L * 1024 * 1024;
    private static final long MAX_UNCOMPRESSED_SIZE = 20L * 1024 * 1024;
    private static final int MAX_ENTRY_COUNT = 300;

    private final SkillService skillService;
    private final SkillGovernanceService skillGovernanceService;
    private final SkillRepository skillRepository;
    private final GitLabSkillPublishService gitLabSkillPublishService;

    public SkillPackageImportService(SkillService skillService,
                                     SkillGovernanceService skillGovernanceService,
                                     SkillRepository skillRepository,
                                     GitLabSkillPublishService gitLabSkillPublishService) {
        this.skillService = skillService;
        this.skillGovernanceService = skillGovernanceService;
        this.skillRepository = skillRepository;
        this.gitLabSkillPublishService = gitLabSkillPublishService;
    }

    @Transactional
    public SkillPackageImportResultDto importPackage(MultipartFile file, Long adminId) {
        return importPackageForUser(file, adminId);
    }

    @Transactional
    public SkillPackageImportResultDto importPackageForContributor(MultipartFile file, Long userId) {
        return importPackageForUser(file, userId);
    }

    private SkillPackageImportResultDto importPackageForUser(MultipartFile file, Long uploaderId) {
        PackageFiles packageFiles = readPackage(file);
        ParsedPackage parsed = parsePackage(packageFiles);
        SkillTemplateValidationDto packageValidation = validatePackage(parsed, null);
        if (!packageValidation.passed()) {
            return new SkillPackageImportResultDto(null, packageValidation, null, null);
        }

        if (skillRepository.findFirstBySkillDirectoryIgnoreCase(parsed.skillDirectory()).isPresent()) {
            SkillTemplateValidationDto duplicateReport = validatePackage(parsed,
                    failed("duplicateSkill", "资产唯一性", "Skill 目录已存在：" + parsed.skillDirectory()));
            return new SkillPackageImportResultDto(null, duplicateReport, null, null);
        }

        SkillGitLabPublishResultDto gitLabPublication = gitLabSkillPublishService.publishPackage(
                parsed.skillDirectory(),
                skillInstallName(parsed),
                publishableFiles(parsed)
        );
        CreateSkillRequest request = buildCreateRequest(parsed);
        applyGitLabPublication(request, gitLabPublication);
        SkillDto skill = skillService.createWithSource(request, uploaderId, CreationSource.SKILL_CREATOR_PACKAGE);
        SkillTemplateValidationDto assetValidation = skillGovernanceService.validateTemplate(skill.getId());
        SkillDto savedSkill = skillService.getVisibleById(skill.getId());
        return new SkillPackageImportResultDto(savedSkill, packageValidation, assetValidation, gitLabPublication);
    }

    private PackageFiles readPackage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择 skill-creator 生成的 zip 包");
        }
        if (file.getSize() > MAX_ZIP_SIZE) {
            throw new IllegalArgumentException("Skill 包不能超过 10MB");
        }
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase(Locale.ROOT) : "";
        if (!filename.endsWith(".zip")) {
            throw new IllegalArgumentException("仅支持 .zip 格式的 Skill 包");
        }

        Map<String, byte[]> files = new LinkedHashMap<>();
        Set<String> directories = new LinkedHashSet<>();
        long totalSize = 0;
        int count = 0;
        try (ZipInputStream zip = new ZipInputStream(file.getInputStream(), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = normalizeEntryName(entry.getName());
                if (name == null) {
                    continue;
                }
                count++;
                if (count > MAX_ENTRY_COUNT) {
                    throw new IllegalArgumentException("Skill 包文件数量过多，最多 " + MAX_ENTRY_COUNT + " 个");
                }
                collectParentDirectories(name, directories);
                if (entry.isDirectory()) {
                    directories.add(trimTrailingSlash(name));
                    continue;
                }
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int read;
                while ((read = zip.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                    totalSize += read;
                    if (totalSize > MAX_UNCOMPRESSED_SIZE) {
                        throw new IllegalArgumentException("Skill 包解压后不能超过 20MB");
                    }
                }
                files.put(name, out.toByteArray());
            }
        } catch (IOException ex) {
            throw new IllegalArgumentException("Skill 包读取失败：" + ex.getMessage());
        }
        return new PackageFiles(files, directories);
    }

    private ParsedPackage parsePackage(PackageFiles packageFiles) {
        String skillMdPath = findSkillMd(packageFiles.files().keySet());
        String skillMd = skillMdPath != null ? readText(packageFiles.files().get(skillMdPath)) : "";
        Map<String, String> frontmatter = parseFrontmatter(skillMd);
        String frontmatterName = frontmatter.get("name");
        String rootDirectory = skillMdPath != null && skillMdPath.endsWith("/SKILL.md")
                ? skillMdPath.substring(0, skillMdPath.length() - "/SKILL.md".length())
                : "";
        String skillDirectory = hasText(rootDirectory) ? lastSegment(rootDirectory) : frontmatterName;
        String displayName = firstHeading(skillMd);
        if (!hasText(displayName)) displayName = frontmatterName;
        String description = frontmatter.get("description");
        return new ParsedPackage(
                packageFiles,
                skillMdPath,
                skillMd,
                frontmatter,
                blankToNull(skillDirectory),
                blankToNull(displayName),
                blankToNull(description)
        );
    }

    private SkillTemplateValidationDto validatePackage(ParsedPackage parsed, SkillTemplateValidationItemDto extraFailure) {
        List<SkillTemplateValidationItemDto> items = new ArrayList<>();
        items.add(required("skillMd", "SKILL.md", parsed.skillMdPath(), "需要包含 skill-creator 生成的 SKILL.md"));
        items.add(required("frontmatterName", "frontmatter name", parsed.frontmatter().get("name"), "SKILL.md frontmatter 需要 name"));
        items.add(required("frontmatterDescription", "frontmatter description", parsed.frontmatter().get("description"), "SKILL.md frontmatter 需要 description"));
        items.add(required("skillDirectory", "Skill 目录", parsed.skillDirectory(), "zip 中需要包含 Skill 目录或 frontmatter name"));
        items.add(required("directoryMatchesName", "目录名与 name 一致", directoryMatchesName(parsed), "Skill 目录名应与 frontmatter name 保持一致"));
        items.add(required("agentsOpenaiYaml", "agents/openai.yaml", hasAgentsOpenaiYaml(parsed), "需要包含 agents/openai.yaml 便于发现和展示"));
        if (gitLabSkillPublishService.isPublicationEnabled()) {
            items.add(recommended("sourceRepositoryUrl", "仓库来源", hasText(sourceRepositoryUrl(parsed)), "包内未标注时将由 GitLab 自动发布生成"));
        } else {
            items.add(required("sourceRepositoryUrl", "仓库来源", sourceRepositoryUrl(parsed), "需要在 frontmatter 标注 sourceRepositoryUrl/repository"));
        }
        items.add(required("quickValidateEvidence", "quick_validate.py 校验", hasQuickValidateEvidence(parsed), "需要保留 quick_validate.py 校验通过证据"));
        items.add(recommended("skillCategory", "Skill 分类", parseSkillCategory(parsed) != null, "建议在 frontmatter 标注 skillCategory/category"));
        items.add(recommended("buildPriority", "建设优先级", parseBuildPriority(parsed) != null, "建议在 frontmatter 标注 buildPriority/priority"));
        items.add(recommended("referencesDirectory", "references 目录", hasDirectory(parsed, "references"), "建议放置规范、业务规则或示例"));
        items.add(recommended("scriptsDirectory", "scripts 目录", hasDirectory(parsed, "scripts"), "如包含脚本，建议放入 scripts"));
        items.add(recommended("assetsDirectory", "assets 目录", hasDirectory(parsed, "assets"), "如包含模板或素材，建议放入 assets"));
        if (extraFailure != null) {
            items.add(extraFailure);
        }

        long requiredTotal = items.stream().filter(SkillTemplateValidationItemDto::required).count();
        long requiredPassed = items.stream().filter(i -> i.required() && i.passed()).count();
        boolean passed = requiredTotal == requiredPassed;
        return new SkillTemplateValidationDto(
                null,
                passed ? TemplateValidationStatus.PASSED.name() : TemplateValidationStatus.FAILED.name(),
                passed,
                (int) requiredPassed,
                (int) requiredTotal,
                Instant.now(),
                buildNotes(items),
                items
        );
    }

    private CreateSkillRequest buildCreateRequest(ParsedPackage parsed) {
        CreateSkillRequest request = new CreateSkillRequest();
        request.setName(parsed.displayName());
        request.setDescription(parsed.description());
        request.setCloneCommand(buildGitCloneCommand(sourceRepositoryUrl(parsed)));
        request.setContentMd(parsed.skillMd());
        request.setSourceRepositoryUrl(sourceRepositoryUrl(parsed));
        request.setSkillDirectory(parsed.skillDirectory());
        request.setAssetLevel(AssetLevel.TEAM);
        request.setLifecycleStatus(LifecycleStatus.CANDIDATE);
        request.setSkillCategory(parseSkillCategory(parsed));
        request.setBuildPriority(parseBuildPriority(parsed));
        request.setVersion(parsed.frontmatter().getOrDefault("version", "1.0.0"));
        request.setApplicableScenarios(section(parsed.skillMd(), "适用场景", "触发场景", "When to Use"));
        request.setNonApplicableScenarios(section(parsed.skillMd(), "不适用场景", "不适用范围", "Out of Scope"));
        request.setInputRequirements(section(parsed.skillMd(), "输入要求", "输入材料", "Inputs"));
        request.setExecutionSteps(section(parsed.skillMd(), "执行步骤", "工作流", "Workflow", "Procedure"));
        request.setOutputFormat(section(parsed.skillMd(), "输出格式", "输出要求", "Outputs"));
        request.setValidationMethod(validationMethod(parsed));
        request.setQualityStandard(section(parsed.skillMd(), "质量标准", "验收标准", "Quality"));
        request.setReferenceMaterials(referenceMaterials(parsed));
        request.setRiskLevel(RiskLevel.LOW);
        request.setReviewNotes("由 skill-creator zip 包导入，待团队试用和评审。");
        request.setTags(tags(parsed));
        return request;
    }

    private void applyGitLabPublication(CreateSkillRequest request, SkillGitLabPublishResultDto publication) {
        if (publication == null || !"PUBLISHED".equals(publication.status())) {
            return;
        }
        if (hasText(publication.repositoryUrl())) {
            request.setSourceRepositoryUrl(publication.repositoryUrl());
            request.setCloneCommand(buildGitCloneCommand(publication.repositoryUrl()));
        }

        List<String> notes = new ArrayList<>();
        if (hasText(request.getReviewNotes())) {
            notes.add(request.getReviewNotes().trim());
        }
        if (hasText(publication.mergeRequestUrl())) {
            notes.add("GitLab MR：" + publication.mergeRequestUrl());
        }
        if (hasText(publication.branchName())) {
            notes.add("GitLab 分支：" + publication.branchName());
        }
        if (hasText(publication.skillPath())) {
            notes.add("GitLab 路径：" + publication.skillPath());
        }
        request.setReviewNotes(blankToNull(String.join("\n", notes)));
    }

    private Map<String, byte[]> publishableFiles(ParsedPackage parsed) {
        Map<String, byte[]> files = new LinkedHashMap<>();
        parsed.packageFiles().files().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> files.put(relativePath(parsed, entry.getKey()), entry.getValue()));
        return files;
    }

    private String skillInstallName(ParsedPackage parsed) {
        return parsed.frontmatter().getOrDefault("name", parsed.skillDirectory());
    }

    private List<String> tags(ParsedPackage parsed) {
        List<String> tags = new ArrayList<>();
        tags.add("Skill 包导入");
        String rawTags = parsed.frontmatter().get("tags");
        if (hasText(rawTags)) {
            String normalized = rawTags.replace("[", "").replace("]", "");
            for (String tag : normalized.split("[,，]")) {
                if (hasText(tag)) tags.add(stripQuotes(tag.trim()));
            }
        }
        return tags.stream().filter(this::hasText).distinct().toList();
    }

    private String referenceMaterials(ParsedPackage parsed) {
        List<String> parts = new ArrayList<>();
        String section = section(parsed.skillMd(), "参考资料", "Reference Materials", "References");
        if (hasText(section)) parts.add(section);
        List<String> files = parsed.packageFiles().files().keySet().stream()
                .map(path -> relativePath(parsed, path))
                .filter(path -> path.startsWith("references/"))
                .sorted()
                .toList();
        if (!files.isEmpty()) {
            parts.add("references 文件：" + String.join("、", files));
        }
        return blankToNull(String.join("\n", parts));
    }

    private String sourceRepositoryUrl(ParsedPackage parsed) {
        return blankToNull(firstFrontmatterValue(parsed,
                "sourcerepositoryurl",
                "source_repository_url",
                "repositoryurl",
                "repository_url",
                "repository",
                "repo",
                "homepage",
                "url"));
    }

    private String buildGitCloneCommand(String repositoryUrl) {
        String cloneUrl = toGitCloneUrl(repositoryUrl);
        return hasText(cloneUrl) ? "git clone " + cloneUrl : "git clone <待评审后生成的仓库地址>";
    }

    private String toGitCloneUrl(String repositoryUrl) {
        String url = blankToNull(repositoryUrl);
        if (!hasText(url)) {
            return null;
        }
        if (url.endsWith(".git")) {
            return url;
        }
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url + ".git";
        }
        return url;
    }

    private String validationMethod(ParsedPackage parsed) {
        String validation = section(parsed.skillMd(), "验证方式", "校验方式", "Validation");
        if (hasText(validation)) {
            return validation;
        }
        if (hasQuickValidateEvidence(parsed)) {
            return "quick_validate.py 校验通过。";
        }
        return null;
    }

    private SkillCategory parseSkillCategory(ParsedPackage parsed) {
        String value = firstFrontmatterValue(parsed, "skillcategory", "skill_category", "category", "分类");
        if (!hasText(value)) return null;
        String normalized = normalizeText(value).replace("-", "_");
        return switch (normalized) {
            case "requirement_analysis", "requirementanalysis", "需求分析", "prd" -> SkillCategory.REQUIREMENT_ANALYSIS;
            case "architecture_design", "architecturedesign", "架构设计", "方案设计", "接口设计" -> SkillCategory.ARCHITECTURE_DESIGN;
            case "coding_implementation", "codingimplementation", "coding", "编码实现", "代码规范", "springboot", "java" -> SkillCategory.CODING_IMPLEMENTATION;
            case "testing_validation", "testingvalidation", "testing", "测试验证", "单元测试", "接口测试" -> SkillCategory.TESTING_VALIDATION;
            case "code_review", "codereview", "review", "代码review", "代码审查" -> SkillCategory.CODE_REVIEW;
            case "ops_troubleshooting", "opstroubleshooting", "ops", "troubleshooting", "排障运维", "线上故障排查", "故障排查" -> SkillCategory.OPS_TROUBLESHOOTING;
            case "documentation_knowledge", "documentationknowledge", "docs", "documentation", "文档知识", "接口文档" -> SkillCategory.DOCUMENTATION_KNOWLEDGE;
            default -> null;
        };
    }

    private BuildPriority parseBuildPriority(ParsedPackage parsed) {
        String value = firstFrontmatterValue(parsed, "buildpriority", "build_priority", "priority", "优先级");
        if (!hasText(value)) return null;
        String normalized = normalizeText(value).replace("０", "0").replace("１", "1").replace("２", "2");
        return switch (normalized) {
            case "p0", "0" -> BuildPriority.P0;
            case "p1", "1" -> BuildPriority.P1;
            case "p2", "2" -> BuildPriority.P2;
            default -> null;
        };
    }

    private String firstFrontmatterValue(ParsedPackage parsed, String... keys) {
        for (String key : keys) {
            String value = parsed.frontmatter().get(key.toLowerCase(Locale.ROOT));
            if (hasText(value)) return value;
        }
        return null;
    }

    private String section(String markdown, String... headings) {
        if (!hasText(markdown)) return null;
        List<String> captured = new ArrayList<>();
        boolean capturing = false;
        for (String line : markdown.split("\\R")) {
            String heading = headingText(line);
            if (heading != null) {
                if (capturing) break;
                if (matchesHeading(heading, headings)) {
                    capturing = true;
                }
                continue;
            }
            if (capturing) {
                captured.add(line);
            }
        }
        return blankToNull(String.join("\n", captured).trim());
    }

    private boolean matchesHeading(String heading, String... expected) {
        String normalized = normalizeText(heading);
        for (String item : expected) {
            if (normalized.contains(normalizeText(item))) {
                return true;
            }
        }
        return false;
    }

    private String headingText(String line) {
        String trimmed = line.trim();
        if (!trimmed.startsWith("#")) return null;
        int i = 0;
        while (i < trimmed.length() && trimmed.charAt(i) == '#') i++;
        if (i == 0 || i >= trimmed.length() || !Character.isWhitespace(trimmed.charAt(i))) return null;
        return trimmed.substring(i).trim();
    }

    private String firstHeading(String markdown) {
        if (!hasText(markdown)) return null;
        for (String line : markdown.split("\\R")) {
            String heading = headingText(line);
            if (hasText(heading)) return heading;
        }
        return null;
    }

    private Map<String, String> parseFrontmatter(String markdown) {
        Map<String, String> frontmatter = new LinkedHashMap<>();
        if (!hasText(markdown)) return frontmatter;
        String[] lines = markdown.stripLeading().split("\\R", -1);
        if (lines.length == 0 || !"---".equals(lines[0].trim())) return frontmatter;
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if ("---".equals(line)) break;
            int sep = line.indexOf(':');
            if (sep <= 0) continue;
            String key = line.substring(0, sep).trim().toLowerCase(Locale.ROOT);
            String value = stripQuotes(line.substring(sep + 1).trim());
            if (hasText(key) && hasText(value)) {
                frontmatter.put(key, value);
            }
        }
        return frontmatter;
    }

    private String findSkillMd(Set<String> paths) {
        return paths.stream()
                .filter(path -> path.equals("SKILL.md") || path.endsWith("/SKILL.md"))
                .min((a, b) -> Integer.compare(a.length(), b.length()))
                .orElse(null);
    }

    private boolean hasAgentsOpenaiYaml(ParsedPackage parsed) {
        return parsed.packageFiles().files().keySet().stream()
                .map(path -> relativePath(parsed, path))
                .anyMatch(path -> path.equals("agents/openai.yaml"));
    }

    private boolean hasQuickValidateEvidence(ParsedPackage parsed) {
        String frontmatterValue = firstFrontmatterValue(parsed,
                "quickvalidate",
                "quick_validate",
                "quickvalidatestatus",
                "quick_validate_status",
                "validationstatus",
                "validation_status");
        if (isPassedEvidence(frontmatterValue)) {
            return true;
        }

        String validationSection = section(parsed.skillMd(), "验证方式", "校验方式", "Validation");
        if (mentionsQuickValidate(validationSection) && isPassedEvidence(validationSection)) {
            return true;
        }

        for (Map.Entry<String, byte[]> entry : parsed.packageFiles().files().entrySet()) {
            String relativePath = relativePath(parsed, entry.getKey());
            String normalizedPath = normalizeText(relativePath).replace("_", "");
            if (!normalizedPath.contains("quickvalidate") || normalizedPath.endsWith(".py")) {
                continue;
            }
            String evidence = relativePath + "\n" + readText(entry.getValue());
            if (isPassedEvidence(evidence)) {
                return true;
            }
        }
        return false;
    }

    private boolean mentionsQuickValidate(String value) {
        if (!hasText(value)) return false;
        String normalized = normalizeText(value).replace("_", "");
        return normalized.contains("quickvalidate") || value.contains("快速校验");
    }

    private boolean isPassedEvidence(String value) {
        if (!hasText(value)) return false;
        String normalized = normalizeText(value);
        return normalized.contains("passed")
                || normalized.contains("success")
                || normalized.contains("ok")
                || normalized.contains("true")
                || value.contains("通过")
                || value.contains("成功");
    }

    private boolean hasDirectory(ParsedPackage parsed, String directory) {
        return parsed.packageFiles().files().keySet().stream()
                .map(path -> relativePath(parsed, path))
                .anyMatch(path -> path.startsWith(directory + "/"));
    }

    private boolean directoryMatchesName(ParsedPackage parsed) {
        String name = parsed.frontmatter().get("name");
        return hasText(name) && hasText(parsed.skillDirectory()) && parsed.skillDirectory().equals(name);
    }

    private String relativePath(ParsedPackage parsed, String path) {
        String root = parsed.skillMdPath() != null && parsed.skillMdPath().endsWith("/SKILL.md")
                ? parsed.skillMdPath().substring(0, parsed.skillMdPath().length() - "/SKILL.md".length())
                : "";
        if (!hasText(root)) return path;
        return path.startsWith(root + "/") ? path.substring(root.length() + 1) : path;
    }

    private SkillTemplateValidationItemDto required(String key, String label, String value, String message) {
        return new SkillTemplateValidationItemDto(key, label, true, hasText(value), hasText(value) ? "已通过" : message);
    }

    private SkillTemplateValidationItemDto required(String key, String label, boolean passed, String message) {
        return new SkillTemplateValidationItemDto(key, label, true, passed, passed ? "已通过" : message);
    }

    private SkillTemplateValidationItemDto recommended(String key, String label, boolean passed, String message) {
        return new SkillTemplateValidationItemDto(key, label, false, passed, passed ? "已检测" : message);
    }

    private SkillTemplateValidationItemDto failed(String key, String label, String message) {
        return new SkillTemplateValidationItemDto(key, label, true, false, message);
    }

    private String buildNotes(List<SkillTemplateValidationItemDto> items) {
        List<String> missing = items.stream()
                .filter(item -> item.required() && !item.passed())
                .map(SkillTemplateValidationItemDto::label)
                .toList();
        if (missing.isEmpty()) return "Skill 包结构校验通过";
        return "缺少或不符合：" + String.join("、", missing);
    }

    private String normalizeEntryName(String rawName) {
        if (!hasText(rawName)) return null;
        String name = rawName.replace('\\', '/');
        while (name.startsWith("/")) name = name.substring(1);
        name = trimTrailingSlash(name);
        if (!hasText(name)) return null;
        for (String part : name.split("/")) {
            if (part.equals("..") || part.equals(".")) {
                throw new IllegalArgumentException("Skill 包包含非法路径：" + rawName);
            }
        }
        return name;
    }

    private void collectParentDirectories(String path, Set<String> directories) {
        int index = path.lastIndexOf('/');
        while (index > 0) {
            directories.add(path.substring(0, index));
            index = path.lastIndexOf('/', index - 1);
        }
    }

    private String trimTrailingSlash(String value) {
        String result = value;
        while (result.endsWith("/")) result = result.substring(0, result.length() - 1);
        return result;
    }

    private String lastSegment(String path) {
        int index = path.lastIndexOf('/');
        return index >= 0 ? path.substring(index + 1) : path;
    }

    private String readText(byte[] bytes) {
        return bytes == null ? "" : new String(bytes, StandardCharsets.UTF_8);
    }

    private String stripQuotes(String value) {
        if (!hasText(value)) return value;
        String trimmed = value.trim();
        if ((trimmed.startsWith("\"") && trimmed.endsWith("\"")) || (trimmed.startsWith("'") && trimmed.endsWith("'"))) {
            return trimmed.substring(1, trimmed.length() - 1).trim();
        }
        return trimmed;
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
    }

    private String blankToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record PackageFiles(Map<String, byte[]> files, Set<String> directories) {}

    private record ParsedPackage(
            PackageFiles packageFiles,
            String skillMdPath,
            String skillMd,
            Map<String, String> frontmatter,
            String skillDirectory,
            String displayName,
            String description
    ) {}
}
