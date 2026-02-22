package com.example.platform.controller;

import com.example.platform.dto.ApiResponse;
import com.example.platform.dto.ArticleDto;
import com.example.platform.dto.CreateRuleRequest;
import com.example.platform.dto.CreateSkillRequest;
import com.example.platform.dto.ExternalSkillDto;
import com.example.platform.dto.ExternalSkillWriteRequest;
import com.example.platform.dto.PageResult;
import com.example.platform.dto.RuleDto;
import com.example.platform.dto.SkillDto;
import com.example.platform.dto.AiToolDto;
import com.example.platform.dto.McpServerDto;
import com.example.platform.entity.AiTool;
import com.example.platform.entity.ExternalSkill;
import com.example.platform.entity.LearningArticle;
import com.example.platform.entity.Skill;
import com.example.platform.service.AibaseNewsScraperService;
import com.example.platform.service.AiToolService;
import com.example.platform.service.AiToolScraperService;
import com.example.platform.service.McpScraperService;
import com.example.platform.service.McpServerService;
import com.example.platform.service.ExternalSkillScraperService;
import com.example.platform.service.NewsService;
import com.example.platform.service.OpenLmArenaScraperService;
import com.example.platform.service.ArticleService;
import com.example.platform.service.ExternalSkillService;
import com.example.platform.service.RuleService;
import com.example.platform.service.SkillService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final SkillService skillService;
    private final RuleService ruleService;
    private final ExternalSkillService externalSkillService;
    private final ArticleService articleService;
    private final AibaseNewsScraperService aibaseNewsScraperService;
    private final AiToolScraperService aiToolScraperService;
    private final AiToolService aiToolService;
    private final McpScraperService mcpScraperService;
    private final McpServerService mcpServerService;
    private final ExternalSkillScraperService externalSkillScraperService;
    private final NewsService newsService;
    private final OpenLmArenaScraperService openLmArenaScraperService;

    public AdminController(SkillService skillService, RuleService ruleService, ExternalSkillService externalSkillService, ArticleService articleService, AibaseNewsScraperService aibaseNewsScraperService, AiToolScraperService aiToolScraperService, AiToolService aiToolService, McpScraperService mcpScraperService, McpServerService mcpServerService, ExternalSkillScraperService externalSkillScraperService, NewsService newsService, OpenLmArenaScraperService openLmArenaScraperService) {
        this.skillService = skillService;
        this.ruleService = ruleService;
        this.externalSkillService = externalSkillService;
        this.articleService = articleService;
        this.aibaseNewsScraperService = aibaseNewsScraperService;
        this.aiToolScraperService = aiToolScraperService;
        this.aiToolService = aiToolService;
        this.mcpScraperService = mcpScraperService;
        this.mcpServerService = mcpServerService;
        this.externalSkillScraperService = externalSkillScraperService;
        this.newsService = newsService;
        this.openLmArenaScraperService = openLmArenaScraperService;
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

    @PostMapping("/skills")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SkillDto> createSkill(@Valid @RequestBody CreateSkillRequest req, Authentication auth) {
        Long adminId = (Long) auth.getPrincipal();
        return ApiResponse.ok(skillService.create(req, adminId));
    }

    @GetMapping("/skills")
    public ApiResponse<PageResult<SkillDto>> listSkills(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<SkillDto> p = skillService.listAllForAdmin(keyword, page, size);
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

    @GetMapping("/external-skills")
    public ApiResponse<PageResult<ExternalSkillDto>> listExternalSkills(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<ExternalSkillDto> p = externalSkillService.listAllForAdmin(keyword, page, size);
        return ApiResponse.ok(new PageResult<>(p.getContent(), p.getTotalElements()));
    }

    @GetMapping("/external-skills/{id}")
    public ApiResponse<ExternalSkillDto> getExternalSkill(@PathVariable Long id) {
        return ApiResponse.ok(externalSkillService.getDtoByIdForAdmin(id));
    }

    @PostMapping("/scrape/external-skills")
    public ApiResponse<ScrapeResult> scrapeExternalSkills() {
        externalSkillScraperService.doScrapeAsync();
        return ApiResponse.ok(new ScrapeResult("started", "外部 Skill 爬取任务已在后台启动"));
    }

    @GetMapping("/scrape-status/external-skills")
    public ApiResponse<ScrapeStatusResponse> getExternalSkillsScrapeStatus() {
        var status = externalSkillScraperService.getLastStatus();
        return ApiResponse.ok(new ScrapeStatusResponse(
                status.status(),
                status.added(),
                status.error(),
                status.finishedAt() != null ? status.finishedAt().toString() : null
        ));
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

    @PostMapping("/external-skills")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ExternalSkill> createExternalSkill(@Valid @RequestBody ExternalSkillWriteRequest req) {
        ExternalSkill e = externalSkillService.create(req);
        return ApiResponse.ok(e);
    }

    @PutMapping("/external-skills/{id}")
    public ApiResponse<ExternalSkill> updateExternalSkill(@PathVariable Long id, @Valid @RequestBody ExternalSkillWriteRequest req) {
        ExternalSkill e = externalSkillService.update(id, req);
        return ApiResponse.ok(e);
    }

    @DeleteMapping("/external-skills/{id}")
    public ApiResponse<Void> deleteExternalSkill(@PathVariable Long id) {
        externalSkillService.delete(id);
        return ApiResponse.ok(null);
    }

    /** 批量清理所有外部 Skill 的 content 中的「▼…复制代码」块（与爬虫同一正则），返回被更新的条数。 */
    @PostMapping("/external-skills/clean-content")
    public ApiResponse<Integer> cleanExternalSkillsContent() {
        int updated = externalSkillService.cleanAllContentsCopyCodeBlock();
        return ApiResponse.ok(updated);
    }
}
