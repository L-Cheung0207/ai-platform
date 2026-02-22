package com.example.platform.controller;

import com.example.platform.dto.ApiResponse;
import com.example.platform.dto.NewsWriteRequest;
import com.example.platform.dto.PageResult;
import com.example.platform.entity.News;
import com.example.platform.service.NewsService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/news")
public class NewsController {

    private final NewsService newsService;

    public NewsController(NewsService newsService) {
        this.newsService = newsService;
    }

    @GetMapping
    public ApiResponse<PageResult<News>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate publishDate) {
        Page<News> p = newsService.list(page, size, publishDate);
        return ApiResponse.ok(new PageResult<>(p.getContent(), p.getTotalElements()));
    }

    @GetMapping("/{id}")
    public ApiResponse<News> get(@PathVariable Long id) {
        return ApiResponse.ok(newsService.getById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<News> create(@Valid @RequestBody NewsWriteRequest req) {
        News n = newsService.create(req);
        return ApiResponse.ok(n);
    }

    @PutMapping("/{id}")
    public ApiResponse<News> update(@PathVariable Long id, @Valid @RequestBody NewsWriteRequest req) {
        News n = newsService.update(id, req);
        return ApiResponse.ok(n);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        newsService.delete(id);
        return ApiResponse.ok(null);
    }
}
