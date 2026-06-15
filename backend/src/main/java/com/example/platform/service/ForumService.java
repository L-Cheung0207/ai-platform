package com.example.platform.service;

import com.example.platform.config.ResourceNotFoundException;
import com.example.platform.dto.*;
import com.example.platform.entity.*;
import com.example.platform.entity.ForumPost.PostStatus;
import com.example.platform.entity.ForumPost.PostType;
import com.example.platform.entity.ForumPost.RelatedType;
import com.example.platform.entity.ForumReaction.ReactionType;
import com.example.platform.entity.ForumReaction.TargetType;
import com.example.platform.entity.ForumReply.ReplyStatus;
import com.example.platform.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ForumService {

    private final ForumPostRepository postRepository;
    private final ForumReplyRepository replyRepository;
    private final ForumReactionRepository reactionRepository;
    private final ForumFavoriteRepository favoriteRepository;
    private final ForumCategoryService categoryService;
    private final UserRepository userRepository;
    private final TagRepository tagRepository;

    public ForumService(ForumPostRepository postRepository,
                        ForumReplyRepository replyRepository,
                        ForumReactionRepository reactionRepository,
                        ForumFavoriteRepository favoriteRepository,
                        ForumCategoryService categoryService,
                        UserRepository userRepository,
                        TagRepository tagRepository) {
        this.postRepository = postRepository;
        this.replyRepository = replyRepository;
        this.reactionRepository = reactionRepository;
        this.favoriteRepository = favoriteRepository;
        this.categoryService = categoryService;
        this.userRepository = userRepository;
        this.tagRepository = tagRepository;
    }

    @Transactional(readOnly = true)
    public List<ForumCategoryDto> listCategories() {
        return categoryService.listPublic();
    }

    @Transactional(readOnly = true)
    public List<Tag> listTags(String q) {
        List<Tag> tags = tagRepository.findTagsUsedByVisibleForumPosts(visibleStatuses());
        if (q != null && !q.isBlank()) {
            String keyword = q.trim().toLowerCase();
            tags = tags.stream()
                    .filter(t -> t.getName() != null && t.getName().toLowerCase().contains(keyword))
                    .toList();
        }
        return tags;
    }

    @Transactional(readOnly = true)
    public Page<ForumPostListItemDto> listPublic(String keyword, Long categoryId, List<Long> tagIds, String postType, String sort, int page, int size, Long currentUserId) {
        List<ForumPost> posts = searchPublic(keyword, categoryId, tagIds, postType);
        List<ForumPost> sorted = sortPosts(posts, sort, false);
        return toPage(sorted, page, size, currentUserId);
    }

    @Transactional(readOnly = true)
    public Page<ForumPostListItemDto> listAdmin(String keyword, Long categoryId, String status, String postType, int page, int size, Long currentUserId) {
        PostStatus postStatus = parseEnum(PostStatus.class, status, null);
        PostType type = parseEnum(PostType.class, postType, null);
        List<ForumPost> posts = postRepository.searchAdminPosts(categoryId, postStatus, type, blankToNull(keyword));
        List<ForumPost> sorted = sortPosts(posts, "latest", true);
        return toPage(sorted, page, size, currentUserId);
    }

    @Transactional
    public ForumPostDetailDto getPost(Long id, Long currentUserId, boolean includeHiddenForOwner) {
        ForumPost post = postRepository.findWithGraphById(id)
                .orElseThrow(() -> new ResourceNotFoundException("帖子不存在"));
        if (!includeHiddenForOwner && !visibleStatuses().contains(post.getStatus())) {
            throw new ResourceNotFoundException("帖子不存在");
        }
        if (!includeHiddenForOwner && post.getCategory() != null && !Boolean.TRUE.equals(post.getCategory().getEnabled())) {
            throw new ResourceNotFoundException("帖子不存在");
        }
        post.setViewCount((post.getViewCount() != null ? post.getViewCount() : 0L) + 1);
        postRepository.save(post);
        return buildDetailDto(post, currentUserId);
    }

    @Transactional
    public ForumPostDetailDto createPost(ForumPostWriteRequest req, Long userId) {
        User author = getUser(userId);
        ForumCategory category = categoryService.getVisible(req.getCategoryId());
        ForumPost post = new ForumPost();
        applyWriteRequest(post, req, category);
        post.setAuthor(author);
        post.setLastActivityAt(Instant.now());
        post = postRepository.save(post);
        return buildDetailDto(post, userId);
    }

    @Transactional
    public ForumPostDetailDto updatePost(Long id, ForumPostWriteRequest req, Long userId) {
        ForumPost post = postRepository.findWithGraphById(id)
                .orElseThrow(() -> new ResourceNotFoundException("帖子不存在"));
        ensurePostEditable(post, userId);
        applyWriteRequest(post, req, categoryService.getVisible(req.getCategoryId()));
        post.setLastActivityAt(Instant.now());
        post = postRepository.save(post);
        return buildDetailDto(post, userId);
    }

    @Transactional
    public void deletePost(Long id, Long userId, boolean admin) {
        ForumPost post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("帖子不存在"));
        if (!admin) {
            ensurePostEditable(post, userId);
        }
        post.setStatus(PostStatus.DELETED);
        post.setDeletedAt(Instant.now());
        postRepository.save(post);
    }

    @Transactional
    public ForumReplyDto reply(Long postId, ForumReplyWriteRequest req, Long userId) {
        ForumPost post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("帖子不存在"));
        if (post.getStatus() == PostStatus.LOCKED) {
            throw new IllegalArgumentException("帖子已锁定，无法继续回复");
        }
        User author = getUser(userId);
        ForumReply reply = new ForumReply();
        reply.setPost(post);
        reply.setAuthor(author);
        reply.setContent(req.getContent().trim());
        if (req.getParentReplyId() != null) {
            ForumReply parent = replyRepository.findByIdAndPostId(req.getParentReplyId(), postId)
                    .orElseThrow(() -> new ResourceNotFoundException("父回复不存在"));
            reply.setParentReply(parent);
        }
        reply = replyRepository.save(reply);
        bumpPostCounters(post, true);
        return buildReplyDto(reply, userId, post.getAcceptedReply() != null ? post.getAcceptedReply().getId() : null);
    }

    @Transactional
    public ForumReplyDto updateReply(Long id, ForumReplyWriteRequest req, Long userId, boolean admin) {
        ForumReply reply = replyRepository.findWithGraphById(id)
                .orElseThrow(() -> new ResourceNotFoundException("回复不存在"));
        ensureReplyEditable(reply, userId, admin);
        reply.setContent(req.getContent().trim());
        reply = replyRepository.save(reply);
        return buildReplyDto(reply, userId, reply.getPost() != null && reply.getPost().getAcceptedReply() != null ? reply.getPost().getAcceptedReply().getId() : null);
    }

    @Transactional
    public void deleteReply(Long id, Long userId, boolean admin) {
        ForumReply reply = replyRepository.findWithGraphById(id)
                .orElseThrow(() -> new ResourceNotFoundException("回复不存在"));
        ensureReplyEditable(reply, userId, admin);
        reply.setStatus(ReplyStatus.DELETED);
        reply.setDeletedAt(Instant.now());
        replyRepository.save(reply);
        ForumPost post = reply.getPost();
        if (post != null && post.getReplyCount() != null && post.getReplyCount() > 0) {
            post.setReplyCount(post.getReplyCount() - 1);
            post.setLastActivityAt(Instant.now());
            postRepository.save(post);
        }
    }

    @Transactional
    public void togglePostLike(Long postId, Long userId, boolean like) {
        ForumPost post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("帖子不存在"));
        int delta = toggleLike(TargetType.POST, postId, userId, like);
        if (delta != 0) {
            post.setLikeCount(Math.max(0, safeCount(post.getLikeCount()) + delta));
            postRepository.save(post);
        }
    }

    @Transactional
    public void toggleReplyLike(Long replyId, Long userId, boolean like) {
        ForumReply reply = replyRepository.findById(replyId)
                .orElseThrow(() -> new ResourceNotFoundException("回复不存在"));
        int delta = toggleLike(TargetType.REPLY, replyId, userId, like);
        if (delta != 0) {
            reply.setLikeCount(Math.max(0, safeCount(reply.getLikeCount()) + delta));
            replyRepository.save(reply);
        }
    }

    @Transactional
    public void toggleFavorite(Long postId, Long userId, boolean favorite) {
        ForumPost post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("帖子不存在"));
        Optional<ForumFavorite> existing = favoriteRepository.findByPostIdAndUserId(postId, userId);
        if (favorite) {
            if (existing.isEmpty()) {
                ForumFavorite f = new ForumFavorite();
                f.setPost(post);
                f.setUser(getUser(userId));
                favoriteRepository.save(f);
                post.setFavoriteCount(safeCount(post.getFavoriteCount()) + 1);
            }
        } else if (existing.isPresent()) {
            favoriteRepository.delete(existing.get());
            post.setFavoriteCount(Math.max(0, safeCount(post.getFavoriteCount()) - 1));
        }
        postRepository.save(post);
    }

    @Transactional
    public void acceptReply(Long postId, Long replyId, Long userId, boolean admin) {
        ForumPost post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("帖子不存在"));
        if (!admin && (post.getAuthor() == null || !Objects.equals(post.getAuthor().getId(), userId))) {
            throw new AccessDeniedException("只有作者可以采纳回复");
        }
        ForumReply reply = replyRepository.findByIdAndPostId(replyId, postId)
                .orElseThrow(() -> new ResourceNotFoundException("回复不存在"));
        post.setAcceptedReply(reply);
        post.setLastActivityAt(Instant.now());
        postRepository.save(post);
    }

    @Transactional
    public void adminSetPinned(Long id, boolean pinned) {
        ForumPost post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("帖子不存在"));
        post.setPinned(pinned);
        postRepository.save(post);
    }

    @Transactional
    public void adminSetFeatured(Long id, boolean featured) {
        ForumPost post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("帖子不存在"));
        post.setFeatured(featured);
        postRepository.save(post);
    }

    @Transactional
    public void adminSetStatus(Long id, PostStatus status) {
        ForumPost post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("帖子不存在"));
        post.setStatus(status);
        if (status == PostStatus.DELETED) {
            post.setDeletedAt(Instant.now());
        }
        postRepository.save(post);
    }

    @Transactional(readOnly = true)
    public Page<ForumPostListItemDto> listMinePosts(Long userId, int page, int size) {
        List<ForumPost> posts = postRepository.findByAuthorIdAndStatusNotOrderByCreatedAtDesc(userId, PostStatus.DELETED);
        return toPage(sortPosts(posts, "latest", true), page, size, userId);
    }

    @Transactional(readOnly = true)
    public Page<ForumReplyDto> listMineReplies(Long userId, int page, int size) {
        List<ForumReply> replies = replyRepository.findByAuthorIdAndStatusNotOrderByCreatedAtDesc(userId, ReplyStatus.DELETED);
        List<ForumReplyDto> dtos = replies.stream()
                .map(r -> buildReplyDto(r, userId, r.getPost() != null && r.getPost().getAcceptedReply() != null ? r.getPost().getAcceptedReply().getId() : null))
                .toList();
        return paginate(dtos, page, size);
    }

    @Transactional(readOnly = true)
    public Page<ForumPostListItemDto> listMineFavorites(Long userId, int page, int size) {
        List<ForumPost> posts = favoriteRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(f -> f.getPost())
                .filter(Objects::nonNull)
                .filter(p -> p.getStatus() != PostStatus.DELETED)
                .toList();
        return toPage(sortPosts(posts, "latest", true), page, size, userId);
    }

    @Transactional(readOnly = true)
    public ForumPostDetailDto buildDetailDto(Long id, Long userId) {
        ForumPost post = postRepository.findWithGraphById(id)
                .orElseThrow(() -> new ResourceNotFoundException("帖子不存在"));
        return buildDetailDto(post, userId);
    }

    @Transactional(readOnly = true)
    public List<ForumCategoryDto> listAdminCategories() {
        return categoryService.listAdmin();
    }

    @Transactional(readOnly = true)
    public List<ForumPostListItemDto> listAdminTopPosts() {
        return postRepository.findTop10ByStatusOrderByLastActivityAtDesc(PostStatus.NORMAL)
                .stream().map(ForumPostListItemDto::fromEntity).toList();
    }

    private List<ForumPost> searchPublic(String keyword, Long categoryId, List<Long> tagIds, String postType) {
        PostType type = parseEnum(PostType.class, postType, null);
        String kw = blankToNull(keyword);
        if (tagIds != null && !tagIds.isEmpty()) {
            return postRepository.searchPublicPostsByTags(visibleStatuses(), categoryId, type, kw, tagIds, tagIds.size());
        }
        return postRepository.searchPublicPosts(visibleStatuses(), categoryId, type, kw);
    }

    private List<ForumPost> sortPosts(List<ForumPost> posts, String sort, boolean admin) {
        boolean unresolvedMode = "unresolved".equalsIgnoreCase(sort);
        Comparator<ForumPost> latest = Comparator
                .comparing((ForumPost p) -> Boolean.TRUE.equals(p.getPinned())).reversed()
                .thenComparing(ForumPost::getLastActivityAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(ForumPost::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(ForumPost::getId, Comparator.nullsLast(Comparator.reverseOrder()));
        Comparator<ForumPost> hot = Comparator
                .comparing((ForumPost p) -> Boolean.TRUE.equals(p.getPinned())).reversed()
                .thenComparing((ForumPost p) -> safeCount(p.getLikeCount()) + safeCount(p.getReplyCount()) * 2 + safeCount(p.getViewCount()) / 5, Comparator.reverseOrder())
                .thenComparing(ForumPost::getLastActivityAt, Comparator.nullsLast(Comparator.reverseOrder()));
        Comparator<ForumPost> unresolved = Comparator
                .comparing((ForumPost p) -> Boolean.TRUE.equals(p.getPinned())).reversed()
                .thenComparing(ForumPost::getLastActivityAt, Comparator.nullsLast(Comparator.reverseOrder()));
        Comparator<ForumPost> comparator = switch (sort == null ? "latest" : sort.toLowerCase()) {
            case "hot" -> hot;
            case "unresolved" -> unresolved;
            default -> latest;
        };
        return posts.stream()
                .filter(p -> admin || p.getStatus() != PostStatus.DELETED)
                .filter(p -> !unresolvedMode || (p.getPostType() == PostType.QUESTION && p.getAcceptedReply() == null))
                .sorted(comparator)
                .toList();
    }

    private Page<ForumPostListItemDto> toPage(List<ForumPost> posts, int page, int size, Long currentUserId) {
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), Math.min(50, Math.max(1, size)));
        int from = Math.min(pageable.getPageNumber() * pageable.getPageSize(), posts.size());
        int to = Math.min(from + pageable.getPageSize(), posts.size());
        List<ForumPostListItemDto> content = posts.subList(from, to).stream()
                .map(post -> enrich(post, currentUserId))
                .toList();
        return new PageImpl<>(content, pageable, posts.size());
    }

    private Page<ForumReplyDto> paginate(List<ForumReplyDto> replies, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), Math.min(50, Math.max(1, size)));
        int from = Math.min(pageable.getPageNumber() * pageable.getPageSize(), replies.size());
        int to = Math.min(from + pageable.getPageSize(), replies.size());
        return new PageImpl<>(replies.subList(from, to), pageable, replies.size());
    }

    private ForumPostListItemDto enrich(ForumPost post, Long currentUserId) {
        ForumPostListItemDto dto = ForumPostListItemDto.fromEntity(post);
        if (currentUserId != null) {
            dto.setLikedByMe(reactionRepository.findByTargetTypeAndTargetIdAndUserIdAndReactionType(TargetType.POST, post.getId(), currentUserId, ReactionType.LIKE).isPresent());
            dto.setFavoritedByMe(favoriteRepository.findByPostIdAndUserId(post.getId(), currentUserId).isPresent());
            dto.setCanEdit(post.getAuthor() != null && Objects.equals(post.getAuthor().getId(), currentUserId));
            dto.setCanReply(post.getStatus() != PostStatus.LOCKED);
        } else {
            dto.setLikedByMe(false);
            dto.setFavoritedByMe(false);
            dto.setCanEdit(false);
            dto.setCanReply(post.getStatus() != PostStatus.LOCKED);
        }
        return dto;
    }

    private ForumPostDetailDto buildDetailDto(ForumPost post, Long currentUserId) {
        ForumPostDetailDto dto = ForumPostDetailDto.fromListItem(enrich(post, currentUserId));
        List<ForumReply> replies = replyRepository.findByPostIdAndStatusOrderByCreatedAtAsc(post.getId(), ReplyStatus.NORMAL);
        Map<Long, ForumReplyDto> nodes = new LinkedHashMap<>();
        List<ForumReplyDto> roots = new ArrayList<>();
        Long acceptedReplyId = post.getAcceptedReply() != null ? post.getAcceptedReply().getId() : null;
        for (ForumReply reply : replies) {
            ForumReplyDto node = buildReplyDto(reply, currentUserId, acceptedReplyId);
            nodes.put(node.getId(), node);
        }
        for (ForumReply reply : replies) {
            ForumReplyDto node = nodes.get(reply.getId());
            if (reply.getParentReply() != null && nodes.containsKey(reply.getParentReply().getId())) {
                nodes.get(reply.getParentReply().getId()).getChildren().add(node);
            } else {
                roots.add(node);
            }
        }
        dto.setReplies(roots);
        dto.setAcceptedReply(acceptedReplyId != null ? nodes.get(acceptedReplyId) : null);
        return dto;
    }

    private ForumReplyDto buildReplyDto(ForumReply reply, Long currentUserId, Long acceptedReplyId) {
        ForumReplyDto dto = ForumReplyDto.fromEntity(reply);
        dto.setAccepted(acceptedReplyId != null && acceptedReplyId.equals(reply.getId()));
        if (currentUserId != null) {
            dto.setLikedByMe(reactionRepository.findByTargetTypeAndTargetIdAndUserIdAndReactionType(TargetType.REPLY, reply.getId(), currentUserId, ReactionType.LIKE).isPresent());
            dto.setCanEdit(reply.getAuthor() != null && Objects.equals(reply.getAuthor().getId(), currentUserId));
        } else {
            dto.setLikedByMe(false);
            dto.setCanEdit(false);
        }
        return dto;
    }

    private void applyWriteRequest(ForumPost post, ForumPostWriteRequest req, ForumCategory category) {
        post.setTitle(trim(req.getTitle()));
        post.setContent(trim(req.getContent()));
        post.setPostType(parseEnum(PostType.class, req.getPostType(), PostType.DISCUSSION));
        post.setCategory(category);
        post.setRelatedType(parseEnum(RelatedType.class, req.getRelatedType(), null));
        post.setRelatedId(req.getRelatedId());
        post.setRelatedTitle(trim(req.getRelatedTitle()));
        post.getTags().clear();
        post.getTags().addAll(resolveTags(req.getTagIds()));
        post.setStatus(post.getStatus() == PostStatus.DELETED ? PostStatus.DELETED : PostStatus.NORMAL);
    }

    private List<Tag> resolveTags(List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return List.of();
        }
        return tagRepository.findAllById(tagIds.stream().distinct().limit(5).toList());
    }

    private int toggleLike(TargetType targetType, Long targetId, Long userId, boolean like) {
        Optional<ForumReaction> existing = reactionRepository.findByTargetTypeAndTargetIdAndUserIdAndReactionType(targetType, targetId, userId, ReactionType.LIKE);
        if (like) {
            if (existing.isEmpty()) {
                ForumReaction reaction = new ForumReaction();
                reaction.setTargetType(targetType);
                reaction.setTargetId(targetId);
                reaction.setUser(getUser(userId));
                reaction.setReactionType(ReactionType.LIKE);
                reactionRepository.save(reaction);
                return 1;
            }
            return 0;
        }
        if (existing.isPresent()) {
            reactionRepository.delete(existing.get());
            return -1;
        }
        return 0;
    }

    private void bumpPostCounters(ForumPost post, boolean replyCreated) {
        post.setReplyCount(safeCount(post.getReplyCount()) + (replyCreated ? 1 : 0));
        post.setLastReplyAt(Instant.now());
        post.setLastActivityAt(Instant.now());
        postRepository.save(post);
    }

    private void ensurePostEditable(ForumPost post, Long userId) {
        if (post.getAuthor() == null || !Objects.equals(post.getAuthor().getId(), userId)) {
            throw new AccessDeniedException("只能编辑自己发布的帖子");
        }
        if (post.getStatus() == PostStatus.LOCKED) {
            throw new IllegalArgumentException("帖子已锁定，无法编辑");
        }
    }

    private void ensureReplyEditable(ForumReply reply, Long userId, boolean admin) {
        if (admin) return;
        if (reply.getAuthor() == null || !Objects.equals(reply.getAuthor().getId(), userId)) {
            throw new AccessDeniedException("只能编辑自己发布的回复");
        }
        if (reply.getPost() != null && reply.getPost().getStatus() == PostStatus.LOCKED) {
            throw new IllegalArgumentException("帖子已锁定，无法编辑回复");
        }
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
    }

    private <E extends Enum<E>> E parseEnum(Class<E> type, String value, E fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Enum.valueOf(type, value.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private long safeCount(Long value) {
        return value == null ? 0L : value;
    }

    private List<PostStatus> visibleStatuses() {
        return List.of(PostStatus.NORMAL, PostStatus.LOCKED);
    }
}
