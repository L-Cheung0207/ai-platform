package com.example.platform.controller;

import com.example.platform.dto.ApiResponse;
import com.example.platform.dto.PageResult;
import com.example.platform.entity.McpServer;
import com.example.platform.service.McpServerService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/mcp")
public class McpController {

    private final McpServerService mcpServerService;

    public McpController(McpServerService mcpServerService) {
        this.mcpServerService = mcpServerService;
    }

    @GetMapping
    public ApiResponse<PageResult<Map<String, Object>>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<McpServer> p = mcpServerService.list(keyword, category, page, size);
        List<Map<String, Object>> items = p.getContent().stream()
                .map(t -> toResponseItem(t))
                .collect(Collectors.toList());
        return ApiResponse.ok(new PageResult<>(items, p.getTotalElements()));
    }

    @GetMapping("/grouped")
    public ApiResponse<List<Map<String, Object>>> grouped(@RequestParam(required = false) String keyword) {
        return ApiResponse.ok(mcpServerService.getGroupedByTag(keyword));
    }

    @GetMapping("/tags")
    public ApiResponse<List<String>> tags() {
        return ApiResponse.ok(mcpServerService.getDistinctTags());
    }

    @GetMapping("/filter-options")
    public ApiResponse<List<String>> filterOptions() {
        return ApiResponse.ok(mcpServerService.getDistinctTags());
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> get(@PathVariable Long id) {
        McpServer t = mcpServerService.getById(id);
        return ApiResponse.ok(toResponseItem(t));
    }

    private Map<String, Object> toResponseItem(McpServer t) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", t.getId());
        m.put("name", t.getName());
        m.put("summary", t.getSummary());
        m.put("content", t.getContent());
        m.put("description", t.getSummary() != null ? t.getSummary() : t.getDescription());
        m.put("url", t.getUrl());
        m.put("logoUrl", t.getLogoUrl());
        m.put("tagNames", t.getTagNames());
        m.put("createdAt", t.getCreatedAt());
        return m;
    }
}
