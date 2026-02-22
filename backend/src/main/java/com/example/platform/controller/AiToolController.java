package com.example.platform.controller;

import com.example.platform.dto.ApiResponse;
import com.example.platform.dto.AiToolDto;
import com.example.platform.dto.PageResult;
import com.example.platform.entity.AiTool;
import com.example.platform.service.AiToolService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/ai-tools")
public class AiToolController {

    private final AiToolService aiToolService;

    public AiToolController(AiToolService aiToolService) {
        this.aiToolService = aiToolService;
    }

    @GetMapping
    public ApiResponse<PageResult<Map<String, Object>>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<AiTool> p = aiToolService.list(keyword, category, page, size);
        List<Map<String, Object>> items = p.getContent().stream()
                .map(t -> toResponseItem(t))
                .collect(Collectors.toList());
        return ApiResponse.ok(new PageResult<>(items, p.getTotalElements()));
    }

    @GetMapping("/grouped")
    public ApiResponse<List<Map<String, Object>>> grouped(@RequestParam(required = false) String keyword) {
        return ApiResponse.ok(aiToolService.getGroupedByTag(keyword));
    }

    @GetMapping("/tags")
    public ApiResponse<List<String>> tags() {
        return ApiResponse.ok(aiToolService.getDistinctTags());
    }

    @GetMapping("/categories")
    public ApiResponse<List<String>> categories() {
        return ApiResponse.ok(aiToolService.getCategories());
    }

    @GetMapping("/filter-options")
    public ApiResponse<List<String>> filterOptions() {
        List<String> categories = aiToolService.getCategories();
        List<String> tags = aiToolService.getDistinctTags();
        List<String> all = new ArrayList<>(categories);
        for (String t : tags) {
            if (!categories.contains(t)) all.add(t);
        }
        return ApiResponse.ok(all);
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> get(@PathVariable Long id) {
        AiTool t = aiToolService.getById(id);
        return ApiResponse.ok(toResponseItem(t));
    }

    private Map<String, Object> toResponseItem(AiTool t) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", t.getId());
        m.put("name", t.getName());
        m.put("summary", t.getSummary());
        m.put("content", t.getContent());
        m.put("description", t.getSummary() != null ? t.getSummary() : t.getDescription());
        m.put("url", t.getUrl());
        m.put("logoUrl", aiToolService.getLogoDisplayUrl(t));
        m.put("categoryName", t.getCategoryName());
        m.put("tagNames", t.getTagNames());
        m.put("sourceUrl", t.getSourceUrl());
        m.put("createdAt", t.getCreatedAt());
        return m;
    }
}
