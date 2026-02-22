package com.example.platform.controller;

import com.example.platform.dto.ApiResponse;
import com.example.platform.dto.PageResult;
import com.example.platform.dto.SkillDto;
import com.example.platform.service.SkillService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/skills")
public class SkillController {

    private final SkillService skillService;

    public SkillController(SkillService skillService) {
        this.skillService = skillService;
    }

    @GetMapping
    public ApiResponse<PageResult<SkillDto>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) List<Long> tags,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<SkillDto> p = skillService.listVisible(keyword, tags, page, size);
        return ApiResponse.ok(new PageResult<>(p.getContent(), p.getTotalElements()));
    }

    @GetMapping("/{id}")
    public ApiResponse<SkillDto> get(@PathVariable Long id) {
        return ApiResponse.ok(skillService.getVisibleById(id));
    }
}
