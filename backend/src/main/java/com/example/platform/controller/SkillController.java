package com.example.platform.controller;

import com.example.platform.dto.ApiResponse;
import com.example.platform.dto.CreateSkillRequest;
import com.example.platform.dto.PageResult;
import com.example.platform.dto.SkillDto;
import com.example.platform.dto.SkillFeedbackDto;
import com.example.platform.dto.SkillFeedbackRequest;
import com.example.platform.dto.SkillGovernanceSummaryDto;
import com.example.platform.dto.SkillPackageImportResultDto;
import com.example.platform.dto.SkillUsageRequest;
import com.example.platform.service.SkillGovernanceService;
import com.example.platform.service.SkillPackageImportService;
import com.example.platform.service.SkillService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/skills")
public class SkillController {

    private final SkillService skillService;
    private final SkillGovernanceService skillGovernanceService;
    private final SkillPackageImportService skillPackageImportService;

    public SkillController(SkillService skillService,
                           SkillGovernanceService skillGovernanceService,
                           SkillPackageImportService skillPackageImportService) {
        this.skillService = skillService;
        this.skillGovernanceService = skillGovernanceService;
        this.skillPackageImportService = skillPackageImportService;
    }

    @GetMapping
    public ApiResponse<PageResult<SkillDto>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) List<Long> tags,
            @RequestParam(required = false) String assetLevel,
            @RequestParam(required = false) String lifecycleStatus,
            @RequestParam(required = false) String skillCategory,
            @RequestParam(required = false) String buildPriority,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<SkillDto> p = skillService.listVisible(keyword, tags, assetLevel, lifecycleStatus, skillCategory, buildPriority, page, size);
        return ApiResponse.ok(new PageResult<>(p.getContent(), p.getTotalElements()));
    }

    @GetMapping("/me")
    public ApiResponse<PageResult<SkillDto>> listMine(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        Page<SkillDto> p = skillService.listMy(currentUserId(auth), page, size);
        return ApiResponse.ok(new PageResult<>(p.getContent(), p.getTotalElements()));
    }

    @PostMapping("/me")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SkillDto> createMine(
            @Valid @RequestBody CreateSkillRequest req,
            Authentication auth) {
        return ApiResponse.ok(skillService.create(req, currentUserId(auth)));
    }

    @PutMapping("/me/{id}")
    public ApiResponse<SkillDto> updateMine(
            @PathVariable Long id,
            @Valid @RequestBody CreateSkillRequest req,
            Authentication auth) {
        return ApiResponse.ok(SkillDto.fromEntity(skillService.update(id, req, currentUserId(auth))));
    }

    @DeleteMapping("/me/{id}")
    public ApiResponse<Void> deleteMine(@PathVariable Long id, Authentication auth) {
        skillService.delete(id, currentUserId(auth));
        return ApiResponse.ok(null);
    }

    @PostMapping(value = "/me/import-package", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<SkillPackageImportResultDto> importMine(
            @RequestPart("file") MultipartFile file,
            Authentication auth) {
        return ApiResponse.ok(skillPackageImportService.importPackageForContributor(file, currentUserId(auth)));
    }

    @GetMapping("/{id}")
    public ApiResponse<SkillDto> get(@PathVariable Long id) {
        return ApiResponse.ok(skillService.getPublicById(id));
    }

    @GetMapping("/{id}/governance-summary")
    public ApiResponse<SkillGovernanceSummaryDto> governanceSummary(@PathVariable Long id) {
        return ApiResponse.ok(skillGovernanceService.getPublicSummary(id));
    }

    @PostMapping("/{id}/usage")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SkillGovernanceSummaryDto> recordUsage(
            @PathVariable Long id,
            @Valid @RequestBody SkillUsageRequest req) {
        return ApiResponse.ok(skillGovernanceService.recordUsage(id, req));
    }

    @PostMapping("/{id}/feedback")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SkillFeedbackDto> submitFeedback(
            @PathVariable Long id,
            @Valid @RequestBody SkillFeedbackRequest req) {
        return ApiResponse.ok(skillGovernanceService.submitFeedback(id, req));
    }

    private Long currentUserId(Authentication auth) {
        return (Long) auth.getPrincipal();
    }
}
