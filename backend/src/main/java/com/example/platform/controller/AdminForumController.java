package com.example.platform.controller;

import com.example.platform.dto.*;
import com.example.platform.entity.ForumPost.PostStatus;
import com.example.platform.service.ForumCategoryService;
import com.example.platform.service.ForumService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/forum")
public class AdminForumController {

    private final ForumService forumService;
    private final ForumCategoryService forumCategoryService;

    public AdminForumController(ForumService forumService, ForumCategoryService forumCategoryService) {
        this.forumService = forumService;
        this.forumCategoryService = forumCategoryService;
    }

    @GetMapping("/posts")
    public ApiResponse<PageResult<ForumPostListItemDto>> posts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String postType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        Page<ForumPostListItemDto> p = forumService.listAdmin(keyword, categoryId, status, postType, page, size, currentUserId(auth));
        return ApiResponse.ok(new PageResult<>(p.getContent(), p.getTotalElements()));
    }

    @PostMapping("/posts/{id}/pin")
    public ApiResponse<Void> pin(@PathVariable Long id) {
        forumService.adminSetPinned(id, true);
        return ApiResponse.ok(null);
    }

    @PostMapping("/posts/{id}/unpin")
    public ApiResponse<Void> unpin(@PathVariable Long id) {
        return togglePinned(id, false);
    }

    @PostMapping("/posts/{id}/lock")
    public ApiResponse<Void> lock(@PathVariable Long id) {
        return toggleStatus(id, true);
    }

    @PostMapping("/posts/{id}/unlock")
    public ApiResponse<Void> unlock(@PathVariable Long id) {
        return toggleStatus(id, false);
    }

    @PostMapping("/posts/{id}/hide")
    public ApiResponse<Void> hide(@PathVariable Long id) {
        return toggleHidden(id, true);
    }

    @PostMapping("/posts/{id}/show")
    public ApiResponse<Void> show(@PathVariable Long id) {
        return toggleHidden(id, false);
    }

    @DeleteMapping("/posts/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        forumService.deletePost(id, null, true);
        return ApiResponse.ok(null);
    }

    @GetMapping("/categories")
    public ApiResponse<List<ForumCategoryDto>> categories() {
        return ApiResponse.ok(forumCategoryService.listAdmin());
    }

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ForumCategoryDto> createCategory(@Valid @RequestBody ForumCategoryWriteRequest req) {
        return ApiResponse.ok(forumCategoryService.create(req));
    }

    @PutMapping("/categories/{id}")
    public ApiResponse<ForumCategoryDto> updateCategory(@PathVariable Long id, @Valid @RequestBody ForumCategoryWriteRequest req) {
        return ApiResponse.ok(forumCategoryService.update(id, req));
    }

    @DeleteMapping("/categories/{id}")
    public ApiResponse<Void> deleteCategory(@PathVariable Long id) {
        forumCategoryService.delete(id);
        return ApiResponse.ok(null);
    }

    private ApiResponse<Void> togglePinned(Long id, boolean pinned) {
        forumService.adminSetPinned(id, pinned);
        return ApiResponse.ok(null);
    }

    private ApiResponse<Void> toggleStatus(Long id, boolean locked) {
        forumService.adminSetStatus(id, locked ? PostStatus.LOCKED : PostStatus.NORMAL);
        return ApiResponse.ok(null);
    }

    private ApiResponse<Void> toggleHidden(Long id, boolean hidden) {
        forumService.adminSetStatus(id, hidden ? PostStatus.HIDDEN : PostStatus.NORMAL);
        return ApiResponse.ok(null);
    }

    private Long currentUserId(Authentication auth) {
        if (auth == null) return null;
        Object principal = auth.getPrincipal();
        return principal instanceof Long ? (Long) principal : null;
    }
}
