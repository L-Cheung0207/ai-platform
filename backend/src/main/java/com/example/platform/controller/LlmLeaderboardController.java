package com.example.platform.controller;

import com.example.platform.dto.ApiResponse;
import com.example.platform.dto.LlmLeaderboardEntryDto;
import com.example.platform.dto.PageResult;
import com.example.platform.service.LlmLeaderboardService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/llm-leaderboard")
public class LlmLeaderboardController {

    private final LlmLeaderboardService llmLeaderboardService;

    public LlmLeaderboardController(LlmLeaderboardService llmLeaderboardService) {
        this.llmLeaderboardService = llmLeaderboardService;
    }

    @GetMapping
    public ApiResponse<PageResult<LlmLeaderboardEntryDto>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortOrder) {
        Page<LlmLeaderboardEntryDto> p = llmLeaderboardService.list(keyword, page, size, sortBy, sortOrder);
        return ApiResponse.ok(new PageResult<>(p.getContent(), p.getTotalElements()));
    }
}
