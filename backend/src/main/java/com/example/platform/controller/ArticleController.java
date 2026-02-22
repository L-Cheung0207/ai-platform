package com.example.platform.controller;

import com.example.platform.dto.ApiResponse;
import com.example.platform.dto.ArticleDto;
import com.example.platform.dto.ArticleWriteRequest;
import com.example.platform.dto.PageResult;
import com.example.platform.entity.LearningArticle;
import com.example.platform.service.ArticleService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/articles")
public class ArticleController {

    private final ArticleService articleService;

    public ArticleController(ArticleService articleService) {
        this.articleService = articleService;
    }

    @GetMapping
    public ApiResponse<PageResult<ArticleDto>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<LearningArticle> p = articleService.listPublished(page, size);
        List<ArticleDto> items = p.getContent().stream().map(ArticleDto::fromEntity).collect(Collectors.toList());
        return ApiResponse.ok(new PageResult<>(items, p.getTotalElements()));
    }

    @GetMapping("/{id}")
    public ApiResponse<ArticleDto> get(@PathVariable Long id) {
        LearningArticle a = articleService.getPublishedById(id);
        return ApiResponse.ok(ArticleDto.fromEntity(a));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ArticleDto> create(@Valid @RequestBody ArticleWriteRequest req, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        LearningArticle a = articleService.create(req, userId);
        return ApiResponse.ok(ArticleDto.fromEntity(a));
    }

    @PutMapping("/{id}")
    public ApiResponse<ArticleDto> update(@PathVariable Long id, @Valid @RequestBody ArticleWriteRequest req) {
        LearningArticle a = articleService.update(id, req);
        return ApiResponse.ok(ArticleDto.fromEntity(a));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        articleService.delete(id);
        return ApiResponse.ok(null);
    }
}
