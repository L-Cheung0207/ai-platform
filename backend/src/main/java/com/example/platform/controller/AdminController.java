package com.example.platform.controller;

import com.example.platform.dto.ApiResponse;
import com.example.platform.dto.AdminUserCreateRequest;
import com.example.platform.dto.AdminUserDto;
import com.example.platform.dto.AdminUserUpdateRequest;
import com.example.platform.dto.ArticleDto;
import com.example.platform.dto.CreateRuleRequest;
import com.example.platform.dto.CreateSkillRequest;
import com.example.platform.dto.PageResult;
import com.example.platform.dto.RuleDto;
import com.example.platform.dto.SkillAssetMetricsDto;
import com.example.platform.dto.SkillDto;
import com.example.platform.dto.SkillFeedbackDto;
import com.example.platform.dto.SkillFeedbackStatusRequest;
import com.example.platform.dto.SkillMonthlyReportDto;
import com.example.platform.dto.SkillOperationReportDto;
import com.example.platform.dto.SkillPackageImportResultDto;
import com.example.platform.dto.SkillOperationsDto;
import com.example.platform.dto.SkillQuarterlyReportDto;
import com.example.platform.dto.SkillReviewDto;
import com.example.platform.dto.SkillReviewRequest;
import com.example.platform.dto.SkillTemplateValidationDto;
import com.example.platform.dto.UserSkillGovernanceRoleRequest;
import com.example.platform.dto.UserRoleRequest;
import com.example.platform.dto.AiToolDto;
import com.example.platform.dto.McpServerDto;
import com.example.platform.entity.AiTool;
import com.example.platform.entity.LearningArticle;
import com.example.platform.entity.Skill;
import com.example.platform.service.AibaseNewsScraperService;
import com.example.platform.service.AiToolService;
import com.example.platform.service.AiToolScraperService;
import com.example.platform.service.McpScraperService;
import com.example.platform.service.McpServerService;
import com.example.platform.service.NewsService;
import com.example.platform.service.OpenLmArenaScraperService;
import com.example.platform.service.ArticleService;
import com.example.platform.service.RuleService;
import com.example.platform.service.SkillGovernanceService;
import com.example.platform.service.SkillPackageImportService;
import com.example.platform.service.SkillService;
import com.example.platform.service.UserAdminService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final SkillService skillService;
    private final RuleService ruleService;
    private final ArticleService articleService;
    private final AibaseNewsScraperService aibaseNewsScraperService;
    private final AiToolScraperService aiToolScraperService;
    private final AiToolService aiToolService;
    private final McpScraperService mcpScraperService;
    private final McpServerService mcpServerService;
    private final NewsService newsService;
    private final OpenLmArenaScraperService openLmArenaScraperService;
    private final SkillGovernanceService skillGovernanceService;
    private final SkillPackageImportService skillPackageImportService;
    private final UserAdminService userAdminService;

    public AdminController(SkillService skillService, RuleService ruleService, ArticleService articleService, AibaseNewsScraperService aibaseNewsScraperService, AiToolScraperService aiToolScraperService, AiToolService aiToolService, McpScraperService mcpScraperService, McpServerService mcpServerService, NewsService newsService, OpenLmArenaScraperService openLmArenaScraperService, SkillGovernanceService skillGovernanceService, SkillPackageImportService skillPackageImportService, UserAdminService userAdminService) {
        this.skillService = skillService;
        this.ruleService = ruleService;
        this.articleService = articleService;
        this.aibaseNewsScraperService = aibaseNewsScraperService;
        this.aiToolScraperService = aiToolScraperService;
        this.aiToolService = aiToolService;
        this.mcpScraperService = mcpScraperService;
        this.mcpServerService = mcpServerService;
        this.newsService = newsService;
        this.openLmArenaScraperService = openLmArenaScraperService;
        this.skillGovernanceService = skillGovernanceService;
        this.skillPackageImportService = skillPackageImportService;
        this.userAdminService = userAdminService;
    }

    @GetMapping("/news")
    public ApiResponse<PageResult<com.example.platform.entity.News>> listNews(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate publishDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        var p = newsService.listForAdmin(keyword, publishDate, page, size);
        return ApiResponse.ok(new PageResult<>(p.getContent(), p.getTotalElements()));
    }

    @GetMapping("/mcp")
    public ApiResponse<PageResult<McpServerDto>> listMcp(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        var p = mcpServerService.list(keyword, category, page, size);
        var items = p.getContent().stream().map(McpServerDto::fromEntity).toList();
        return ApiResponse.ok(new PageResult<>(items, p.getTotalElements()));
    }

    @DeleteMapping("/mcp/{id}")
    public ApiResponse<Void> deleteMcp(@PathVariable Long id) {
        mcpServerService.delete(id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/scrape/mcp")
    public ApiResponse<ScrapeResult> scrapeMcp() {
        mcpScraperService.doScrapeAsync();
        return ApiResponse.ok(new ScrapeResult("started", "MCP 爬取任务已在后台启动"));
    }

    @GetMapping("/scrape-status/mcp")
    public ApiResponse<ScrapeStatusResponse> getMcpScrapeStatus() {
        var status = mcpScraperService.getLastStatus();
        return ApiResponse.ok(new ScrapeStatusResponse(
                status.status(),
                status.added(),
                status.error(),
                status.finishedAt() != null ? status.finishedAt().toString() : null
        ));
    }

    @GetMapping("/ai-tools")
    public ApiResponse<PageResult<AiToolDto>> listAiTools(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        var p = aiToolService.list(keyword, category, page, size);
        var items = p.getContent().stream().map(AiToolDto::fromEntity).toList();
        return ApiResponse.ok(new PageResult<>(items, p.getTotalElements()));
    }

    @DeleteMapping("/ai-tools/{id}")
    public ApiResponse<Void> deleteAiTool(@PathVariable Long id) {
        aiToolService.delete(id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/scrape/aibase-news")
    public ApiResponse<ScrapeResult> scrapeAibaseNews() {
        aibaseNewsScraperService.doScrapeAsync();
        return ApiResponse.ok(new ScrapeResult("started", "爬取任务已在后台启动，请稍后查看资讯列表"));
    }

    @PostMapping("/scrape/ai-tools")
    public ApiResponse<ScrapeResult> scrapeAiTools() {
        aiToolScraperService.doScrapeAsync();
        return ApiResponse.ok(new ScrapeResult("started", "AI工具爬取任务已在后台启动"));
    }

    @GetMapping("/scrape-status")
    public ApiResponse<ScrapeStatusResponse> getScrapeStatus() {
        var status = aibaseNewsScraperService.getLastStatus();
        return ApiResponse.ok(new ScrapeStatusResponse(
                status.status(),
                status.added(),
                status.error(),
                status.finishedAt() != null ? status.finishedAt().toString() : null
        ));
    }

    @GetMapping("/scrape-status/ai-tools")
    public ApiResponse<ScrapeStatusResponse> getAiToolsScrapeStatus() {
        var status = aiToolScraperService.getLastStatus();
        return ApiResponse.ok(new ScrapeStatusResponse(
                status.status(),
                status.added(),
                status.error(),
                status.finishedAt() != null ? status.finishedAt().toString() : null
        ));
    }

    public record ScrapeResult(String status, String message) {}
    public record ScrapeStatusResponse(String status, Integer added, String error, String finishedAt) {}

    @GetMapping("/users")
    public ApiResponse<PageResult<AdminUserDto>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<AdminUserDto> p = userAdminService.listUsers(page, size);
        return ApiResponse.ok(new PageResult<>(p.getContent(), p.getTotalElements()));
    }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AdminUserDto> createUser(@Valid @RequestBody AdminUserCreateRequest req) {
        return ApiResponse.ok(userAdminService.createUser(req));
    }

    @PutMapping("/users/{id}")
    public ApiResponse<AdminUserDto> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody AdminUserUpdateRequest req) {
        return ApiResponse.ok(userAdminService.updateUser(id, req));
    }

    @PutMapping("/users/{id}/skill-governance-role")
    public ApiResponse<AdminUserDto> updateUserSkillGovernanceRole(
            @PathVariable Long id,
            @Valid @RequestBody UserSkillGovernanceRoleRequest req) {
        return ApiResponse.ok(userAdminService.updateSkillGovernanceRole(id, req));
    }

    @PutMapping("/users/{id}/role")
    public ApiResponse<AdminUserDto> updateUserRole(
            @PathVariable Long id,
            @Valid @RequestBody UserRoleRequest req) {
        return ApiResponse.ok(userAdminService.updateRole(id, req));
    }

    @DeleteMapping("/users/{id}")
    public ApiResponse<Void> deleteUser(@PathVariable Long id, Authentication auth) {
        userAdminService.deleteUser(id, (Long) auth.getPrincipal());
        return ApiResponse.ok(null);
    }

    @PostMapping("/skills")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SkillDto> createSkill(@Valid @RequestBody CreateSkillRequest req, Authentication auth) {
        Long adminId = (Long) auth.getPrincipal();
        return ApiResponse.ok(skillService.create(req, adminId));
    }

    @GetMapping("/skills")
    public ApiResponse<PageResult<SkillDto>> listSkills(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String assetLevel,
            @RequestParam(required = false) String lifecycleStatus,
            @RequestParam(required = false) String skillCategory,
            @RequestParam(required = false) String buildPriority,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<SkillDto> p = skillService.listAllForAdmin(keyword, assetLevel, lifecycleStatus, skillCategory, buildPriority, page, size);
        return ApiResponse.ok(new PageResult<>(p.getContent(), p.getTotalElements()));
    }

    @PostMapping(value = "/skills/import-package", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<SkillPackageImportResultDto> importSkillPackage(
            @RequestParam("file") MultipartFile file,
            Authentication auth) {
        Long adminId = (Long) auth.getPrincipal();
        return ApiResponse.ok(skillPackageImportService.importPackage(file, adminId));
    }

    @GetMapping("/skill-metrics")
    public ApiResponse<SkillAssetMetricsDto> skillMetrics() {
        return ApiResponse.ok(skillGovernanceService.metrics());
    }

    @GetMapping("/skill-operations")
    public ApiResponse<SkillOperationsDto> skillOperations() {
        return ApiResponse.ok(skillGovernanceService.operationsReport());
    }

    @GetMapping("/skill-operations/monthly-report")
    public ApiResponse<SkillMonthlyReportDto> skillMonthlyReport(@RequestParam(required = false) String month) {
        return ApiResponse.ok(skillGovernanceService.monthlyReport(month));
    }

    @GetMapping("/skill-operations/quarterly-report")
    public ApiResponse<SkillQuarterlyReportDto> skillQuarterlyReport(@RequestParam(required = false) String quarter) {
        return ApiResponse.ok(skillGovernanceService.quarterlyReport(quarter));
    }

    @GetMapping("/skill-operations/monthly-reports")
    public ApiResponse<PageResult<SkillOperationReportDto>> listSkillMonthlyReports(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "6") int size) {
        Page<SkillOperationReportDto> p = skillGovernanceService.listMonthlyReports(page, size);
        return ApiResponse.ok(new PageResult<>(p.getContent(), p.getTotalElements()));
    }

    @GetMapping("/skill-feedback")
    public ApiResponse<PageResult<SkillFeedbackDto>> listAllSkillFeedback(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<SkillFeedbackDto> p = skillGovernanceService.listFeedback(null, status, page, size);
        return ApiResponse.ok(new PageResult<>(p.getContent(), p.getTotalElements()));
    }

    @GetMapping("/rules")
    public ApiResponse<PageResult<RuleDto>> listRules(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<RuleDto> p = ruleService.listAllForAdmin(keyword, page, size);
        return ApiResponse.ok(new PageResult<>(p.getContent(), p.getTotalElements()));
    }

    @PostMapping("/scrape/llm-leaderboard")
    public ApiResponse<ScrapeResult> scrapeLlmLeaderboard() {
        openLmArenaScraperService.doScrapeAsync();
        return ApiResponse.ok(new ScrapeResult("started", "LLM 排行榜爬取任务已在后台启动，数据来源 OpenLM Chatbot Arena"));
    }

    @GetMapping("/scrape-status/llm-leaderboard")
    public ApiResponse<ScrapeStatusResponse> getLlmLeaderboardScrapeStatus() {
        var status = openLmArenaScraperService.getLastStatus();
        return ApiResponse.ok(new ScrapeStatusResponse(
                status.status(),
                status.added(),
                status.error(),
                status.finishedAt() != null ? status.finishedAt().toString() : null
        ));
    }

    @GetMapping("/articles")
    public ApiResponse<PageResult<ArticleDto>> listArticles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        Page<LearningArticle> p = articleService.listAllForAdmin(page, size, status);
        List<ArticleDto> items = p.getContent().stream().map(ArticleDto::fromEntity).collect(Collectors.toList());
        return ApiResponse.ok(new PageResult<>(items, p.getTotalElements()));
    }

    @GetMapping("/articles/{id}")
    public ApiResponse<ArticleDto> getArticle(@PathVariable Long id) {
        LearningArticle a = articleService.getById(id);
        return ApiResponse.ok(ArticleDto.fromEntity(a));
    }

    @PutMapping("/skills/{id}")
    public ApiResponse<SkillDto> updateSkill(@PathVariable Long id, @Valid @RequestBody CreateSkillRequest req) {
        return ApiResponse.ok(skillService.adminUpdate(id, req));
    }

    @PostMapping("/skills/{id}/validate-template")
    public ApiResponse<SkillTemplateValidationDto> validateSkillTemplate(@PathVariable Long id) {
        return ApiResponse.ok(skillGovernanceService.validateTemplate(id));
    }

    @GetMapping("/skills/{id}/reviews")
    public ApiResponse<PageResult<SkillReviewDto>> listSkillReviews(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<SkillReviewDto> p = skillGovernanceService.listReviews(id, page, size);
        return ApiResponse.ok(new PageResult<>(p.getContent(), p.getTotalElements()));
    }

    @PostMapping("/skills/{id}/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SkillReviewDto> createSkillReview(
            @PathVariable Long id,
            @Valid @RequestBody SkillReviewRequest req,
            Authentication auth) {
        Long reviewerUserId = (Long) auth.getPrincipal();
        return ApiResponse.ok(skillGovernanceService.createReview(id, req, reviewerUserId));
    }

    @GetMapping("/skills/{id}/feedback")
    public ApiResponse<PageResult<SkillFeedbackDto>> listSkillFeedback(
            @PathVariable Long id,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<SkillFeedbackDto> p = skillGovernanceService.listFeedback(id, status, page, size);
        return ApiResponse.ok(new PageResult<>(p.getContent(), p.getTotalElements()));
    }

    @PutMapping("/skills/{id}/feedback/{feedbackId}/status")
    public ApiResponse<SkillFeedbackDto> updateSkillFeedbackStatus(
            @PathVariable Long id,
            @PathVariable Long feedbackId,
            @Valid @RequestBody SkillFeedbackStatusRequest req) {
        return ApiResponse.ok(skillGovernanceService.updateFeedbackStatus(id, feedbackId, req));
    }

    @PostMapping("/skills/{id}/hide")
    public ApiResponse<SkillDto> hideSkill(@PathVariable Long id) {
        return ApiResponse.ok(skillService.adminHide(id));
    }

    @PostMapping("/skills/{id}/unhide")
    public ApiResponse<SkillDto> unhideSkill(@PathVariable Long id) {
        return ApiResponse.ok(skillService.adminUnhide(id));
    }

    @DeleteMapping("/skills/{id}")
    public ApiResponse<Void> deleteSkill(@PathVariable Long id) {
        skillService.adminDelete(id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/rules/{id}/hide")
    public ApiResponse<RuleDto> hideRule(@PathVariable Long id) {
        return ApiResponse.ok(ruleService.adminHide(id));
    }

    @PostMapping("/rules/{id}/unhide")
    public ApiResponse<RuleDto> unhideRule(@PathVariable Long id) {
        return ApiResponse.ok(ruleService.adminUnhide(id));
    }

    @DeleteMapping("/rules/{id}")
    public ApiResponse<Void> deleteRule(@PathVariable Long id) {
        ruleService.adminDelete(id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/rules")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RuleDto> createRule(@Valid @RequestBody CreateRuleRequest req, Authentication auth) {
        Long adminId = (Long) auth.getPrincipal();
        return ApiResponse.ok(ruleService.create(req, adminId));
    }

    @PutMapping("/rules/{id}")
    public ApiResponse<RuleDto> updateRule(@PathVariable Long id, @Valid @RequestBody CreateRuleRequest req) {
        return ApiResponse.ok(ruleService.adminUpdate(id, req));
    }

}
