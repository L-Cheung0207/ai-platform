package com.example.platform.controller;

import com.example.platform.dto.ApiResponse;
import com.example.platform.dto.TagCreateRequest;
import com.example.platform.entity.Tag;
import com.example.platform.service.TagService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tags")
public class TagController {

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping
    public ApiResponse<List<Tag>> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String forEntity) {
        List<Tag> list = tagService.list(q, forEntity);
        return ApiResponse.ok(list);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Tag> create(@Valid @RequestBody TagCreateRequest req) {
        Tag t = tagService.create(req.getName());
        return ApiResponse.ok(t);
    }
}
