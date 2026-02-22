package com.example.platform.controller;

import com.example.platform.dto.ApiResponse;
import com.example.platform.dto.PageResult;
import com.example.platform.dto.RuleDto;
import com.example.platform.service.RuleService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rules")
public class RuleController {

    private final RuleService ruleService;

    public RuleController(RuleService ruleService) {
        this.ruleService = ruleService;
    }

    @GetMapping
    public ApiResponse<PageResult<RuleDto>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) List<Long> tags,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<RuleDto> p = ruleService.listVisible(keyword, tags, page, size);
        return ApiResponse.ok(new PageResult<>(p.getContent(), p.getTotalElements()));
    }

    @GetMapping("/{id}")
    public ApiResponse<RuleDto> get(@PathVariable Long id) {
        RuleDto dto = ruleService.getVisibleById(id);
        return ApiResponse.ok(dto);
    }
}
