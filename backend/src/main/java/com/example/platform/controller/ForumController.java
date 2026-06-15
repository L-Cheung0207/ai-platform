package com.example.platform.controller;

import com.example.platform.dto.*;
import com.example.platform.entity.Tag;
import com.example.platform.service.ForumCategoryService;
import com.example.platform.service.ForumService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/forum")
public class ForumController {

    private final ForumService forumService;
    private final ForumCategoryService forumCategoryService;

    public ForumController(ForumService forumService, ForumCategoryService forumCategoryService) {
        this.forumService = forumService;
        this.forumCategoryService = forumCategoryService;
    }

    @GetMapping("/categories")
    public ApiResponse<List<ForumCategoryDto>> categories() {
        return ApiResponse.ok(forumCategoryService.listPublic());
    }

    @GetMapping("/tags")
    public ApiResponse<List<Tag>> tags(@RequestParam(required = false) String q) {
        return ApiResponse.ok(forumService.listTags(q));
    }

    @GetMapping("/posts")
    public ApiResponse<PageResult<ForumPostListItemDto>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) List<Long> tagIds,
            @RequestParam(required = false) String postType,
            @RequestParam(required = false, defaultValue = "latest") String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        Page<ForumPostListItemDto> p = forumService.listPublic(keyword, categoryId, tagIds, postType, sort, page, size, currentUserId(auth));
        return ApiResponse.ok(new PageResult<>(p.getContent(), p.getTotalElements()));
    }

    @GetMapping("/posts/{id}")
    public ApiResponse<ForumPostDetailDto> get(@PathVariable Long id, Authentication auth) {
        return ApiResponse.ok(forumService.getPost(id, currentUserId(auth), false));
    }

    @PostMapping("/posts")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ForumPostDetailDto> create(@Valid @RequestBody ForumPostWriteRequest req, Authentication auth) {
        return ApiResponse.ok(forumService.createPost(req, currentUserIdOrThrow(auth)));
    }

    @PutMapping("/posts/{id}")
    public ApiResponse<ForumPostDetailDto> update(@PathVariable Long id, @Valid @RequestBody ForumPostWriteRequest req, Authentication auth) {
        return ApiResponse.ok(forumService.updatePost(id, req, currentUserIdOrThrow(auth)));
    }

    @DeleteMapping("/posts/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, Authentication auth) {
        forumService.deletePost(id, currentUserIdOrThrow(auth), false);
        return ApiResponse.ok(null);
    }

    @PostMapping("/posts/{id}/replies")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ForumReplyDto> reply(@PathVariable Long id, @Valid @RequestBody ForumReplyWriteRequest req, Authentication auth) {
        return ApiResponse.ok(forumService.reply(id, req, currentUserIdOrThrow(auth)));
    }

    @PutMapping("/replies/{id}")
    public ApiResponse<ForumReplyDto> updateReply(@PathVariable Long id, @Valid @RequestBody ForumReplyWriteRequest req, Authentication auth) {
        return ApiResponse.ok(forumService.updateReply(id, req, currentUserIdOrThrow(auth), false));
    }

    @DeleteMapping("/replies/{id}")
    public ApiResponse<Void> deleteReply(@PathVariable Long id, Authentication auth) {
        forumService.deleteReply(id, currentUserIdOrThrow(auth), false);
        return ApiResponse.ok(null);
    }

    @PostMapping("/posts/{id}/like")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> like(@PathVariable Long id, Authentication auth) {
        forumService.togglePostLike(id, currentUserIdOrThrow(auth), true);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/posts/{id}/like")
    public ApiResponse<Void> unlike(@PathVariable Long id, Authentication auth) {
        forumService.togglePostLike(id, currentUserIdOrThrow(auth), false);
        return ApiResponse.ok(null);
    }

    @PostMapping("/posts/{id}/favorite")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> favorite(@PathVariable Long id, Authentication auth) {
        forumService.toggleFavorite(id, currentUserIdOrThrow(auth), true);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/posts/{id}/favorite")
    public ApiResponse<Void> unfavorite(@PathVariable Long id, Authentication auth) {
        forumService.toggleFavorite(id, currentUserIdOrThrow(auth), false);
        return ApiResponse.ok(null);
    }

    @PostMapping("/replies/{id}/like")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> likeReply(@PathVariable Long id, Authentication auth) {
        forumService.toggleReplyLike(id, currentUserIdOrThrow(auth), true);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/replies/{id}/like")
    public ApiResponse<Void> unlikeReply(@PathVariable Long id, Authentication auth) {
        forumService.toggleReplyLike(id, currentUserIdOrThrow(auth), false);
        return ApiResponse.ok(null);
    }

    @PostMapping("/posts/{postId}/accept/{replyId}")
    public ApiResponse<Void> acceptReply(@PathVariable Long postId, @PathVariable Long replyId, Authentication auth) {
        forumService.acceptReply(postId, replyId, currentUserIdOrThrow(auth), false);
        return ApiResponse.ok(null);
    }

    @GetMapping("/mine/posts")
    public ApiResponse<PageResult<ForumPostListItemDto>> minePosts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        Long userId = currentUserIdOrThrow(auth);
        Page<ForumPostListItemDto> p = forumService.listMinePosts(userId, page, size);
        return ApiResponse.ok(new PageResult<>(p.getContent(), p.getTotalElements()));
    }

    @GetMapping("/mine/replies")
    public ApiResponse<PageResult<ForumReplyDto>> mineReplies(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        Long userId = currentUserIdOrThrow(auth);
        Page<ForumReplyDto> p = forumService.listMineReplies(userId, page, size);
        return ApiResponse.ok(new PageResult<>(p.getContent(), p.getTotalElements()));
    }

    @GetMapping("/mine/favorites")
    public ApiResponse<PageResult<ForumPostListItemDto>> mineFavorites(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        Long userId = currentUserIdOrThrow(auth);
        Page<ForumPostListItemDto> p = forumService.listMineFavorites(userId, page, size);
        return ApiResponse.ok(new PageResult<>(p.getContent(), p.getTotalElements()));
    }

    private Long currentUserId(Authentication auth) {
        if (auth == null) return null;
        Object principal = auth.getPrincipal();
        return principal instanceof Long ? (Long) principal : null;
    }

    private Long currentUserIdOrThrow(Authentication auth) {
        Long id = currentUserId(auth);
        if (id == null) {
            throw new IllegalArgumentException("请先登录");
        }
        return id;
    }
}
