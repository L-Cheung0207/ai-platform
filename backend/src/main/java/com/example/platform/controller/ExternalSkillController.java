package com.example.platform.controller;

import com.example.platform.dto.ApiResponse;
import com.example.platform.dto.ExternalSkillDto;
import com.example.platform.dto.PageResult;
import com.example.platform.service.ExternalSkillService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/external-skills")
public class ExternalSkillController {

    private final ExternalSkillService externalSkillService;

    public ExternalSkillController(ExternalSkillService externalSkillService) {
        this.externalSkillService = externalSkillService;
    }

    @GetMapping
    public ApiResponse<PageResult<ExternalSkillDto>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) List<Long> tags,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<ExternalSkillDto> p = externalSkillService.listVisibleAsDto(keyword, tags, page, size);
        return ApiResponse.ok(new PageResult<>(p.getContent(), p.getTotalElements()));
    }

    @GetMapping("/{id}")
    public ApiResponse<ExternalSkillDto> get(@PathVariable Long id) {
        return ApiResponse.ok(externalSkillService.getVisibleDtoById(id));
    }
}
