package com.example.platform.dto;

import com.example.platform.entity.ForumPost;
import com.example.platform.entity.Tag;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class ForumPostListItemDto {

    private Long id;
    private String title;
    private String excerpt;
    private String content;
    private String postType;
    private String status;
    private Boolean pinned;
    private Boolean featured;
    private Long categoryId;
    private String categoryName;
    private String categorySlug;
    private Long authorId;
    private String authorUsername;
    private Long viewCount;
    private Long likeCount;
    private Long replyCount;
    private Long favoriteCount;
    private Boolean accepted;
    private String relatedType;
    private Long relatedId;
    private String relatedTitle;
    private Instant lastReplyAt;
    private Instant lastActivityAt;
    private Instant createdAt;
    private Instant updatedAt;
    private List<Tag> tags = new ArrayList<>();
    private Boolean likedByMe;
    private Boolean favoritedByMe;
    private Boolean canEdit;
    private Boolean canReply;

    public static ForumPostListItemDto fromEntity(ForumPost post) {
        ForumPostListItemDto dto = new ForumPostListItemDto();
        dto.setId(post.getId());
        dto.setTitle(post.getTitle());
        dto.setContent(post.getContent());
        dto.setExcerpt(excerpt(post.getContent()));
        dto.setPostType(post.getPostType() != null ? post.getPostType().name() : null);
        dto.setStatus(post.getStatus() != null ? post.getStatus().name() : null);
        dto.setPinned(post.getPinned());
        dto.setFeatured(post.getFeatured());
        dto.setCategoryId(post.getCategory() != null ? post.getCategory().getId() : null);
        dto.setCategoryName(post.getCategory() != null ? post.getCategory().getName() : null);
        dto.setCategorySlug(post.getCategory() != null ? post.getCategory().getSlug() : null);
        dto.setAuthorId(post.getAuthor() != null ? post.getAuthor().getId() : null);
        dto.setAuthorUsername(post.getAuthor() != null ? post.getAuthor().getUsername() : null);
        dto.setViewCount(post.getViewCount());
        dto.setLikeCount(post.getLikeCount());
        dto.setReplyCount(post.getReplyCount());
        dto.setFavoriteCount(post.getFavoriteCount());
        dto.setAccepted(post.getAcceptedReply() != null);
        dto.setRelatedType(post.getRelatedType() != null ? post.getRelatedType().name() : null);
        dto.setRelatedId(post.getRelatedId());
        dto.setRelatedTitle(post.getRelatedTitle());
        dto.setLastReplyAt(post.getLastReplyAt());
        dto.setLastActivityAt(post.getLastActivityAt());
        dto.setCreatedAt(post.getCreatedAt());
        dto.setUpdatedAt(post.getUpdatedAt());
        dto.setTags(post.getTags() != null ? new ArrayList<>(post.getTags()) : new ArrayList<>());
        return dto;
    }

    private static String excerpt(String content) {
        if (content == null) return "";
        String compact = content.replaceAll("\\s+", " ").trim();
        if (compact.length() <= 140) return compact;
        return compact.substring(0, 140) + "…";
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getExcerpt() { return excerpt; }
    public void setExcerpt(String excerpt) { this.excerpt = excerpt; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getPostType() { return postType; }
    public void setPostType(String postType) { this.postType = postType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Boolean getPinned() { return pinned; }
    public void setPinned(Boolean pinned) { this.pinned = pinned; }
    public Boolean getFeatured() { return featured; }
    public void setFeatured(Boolean featured) { this.featured = featured; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public String getCategorySlug() { return categorySlug; }
    public void setCategorySlug(String categorySlug) { this.categorySlug = categorySlug; }
    public Long getAuthorId() { return authorId; }
    public void setAuthorId(Long authorId) { this.authorId = authorId; }
    public String getAuthorUsername() { return authorUsername; }
    public void setAuthorUsername(String authorUsername) { this.authorUsername = authorUsername; }
    public Long getViewCount() { return viewCount; }
    public void setViewCount(Long viewCount) { this.viewCount = viewCount; }
    public Long getLikeCount() { return likeCount; }
    public void setLikeCount(Long likeCount) { this.likeCount = likeCount; }
    public Long getReplyCount() { return replyCount; }
    public void setReplyCount(Long replyCount) { this.replyCount = replyCount; }
    public Long getFavoriteCount() { return favoriteCount; }
    public void setFavoriteCount(Long favoriteCount) { this.favoriteCount = favoriteCount; }
    public Boolean getAccepted() { return accepted; }
    public void setAccepted(Boolean accepted) { this.accepted = accepted; }
    public String getRelatedType() { return relatedType; }
    public void setRelatedType(String relatedType) { this.relatedType = relatedType; }
    public Long getRelatedId() { return relatedId; }
    public void setRelatedId(Long relatedId) { this.relatedId = relatedId; }
    public String getRelatedTitle() { return relatedTitle; }
    public void setRelatedTitle(String relatedTitle) { this.relatedTitle = relatedTitle; }
    public Instant getLastReplyAt() { return lastReplyAt; }
    public void setLastReplyAt(Instant lastReplyAt) { this.lastReplyAt = lastReplyAt; }
    public Instant getLastActivityAt() { return lastActivityAt; }
    public void setLastActivityAt(Instant lastActivityAt) { this.lastActivityAt = lastActivityAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public List<Tag> getTags() { return tags; }
    public void setTags(List<Tag> tags) { this.tags = tags != null ? tags : new ArrayList<>(); }
    public Boolean getLikedByMe() { return likedByMe; }
    public void setLikedByMe(Boolean likedByMe) { this.likedByMe = likedByMe; }
    public Boolean getFavoritedByMe() { return favoritedByMe; }
    public void setFavoritedByMe(Boolean favoritedByMe) { this.favoritedByMe = favoritedByMe; }
    public Boolean getCanEdit() { return canEdit; }
    public void setCanEdit(Boolean canEdit) { this.canEdit = canEdit; }
    public Boolean getCanReply() { return canReply; }
    public void setCanReply(Boolean canReply) { this.canReply = canReply; }
}
