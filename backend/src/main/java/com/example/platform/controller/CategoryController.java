package com.example.platform.controller;

import com.example.platform.dto.ApiResponse;
import com.example.platform.entity.Category;
import com.example.platform.service.CategoryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public ApiResponse<List<Category>> list(@RequestParam(required = false) String type) {
        List<Category> list = categoryService.list(type);
        return ApiResponse.ok(list);
    }
}
