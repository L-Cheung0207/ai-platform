package com.example.platform.dto;

import java.util.ArrayList;
import java.util.List;

public class ForumPostDetailDto extends ForumPostListItemDto {

    private List<ForumReplyDto> replies = new ArrayList<>();
    private ForumReplyDto acceptedReply;

    public static ForumPostDetailDto fromListItem(ForumPostListItemDto base) {
        ForumPostDetailDto dto = new ForumPostDetailDto();
        dto.setId(base.getId());
        dto.setTitle(base.getTitle());
        dto.setExcerpt(base.getExcerpt());
        dto.setContent(base.getContent());
        dto.setPostType(base.getPostType());
        dto.setStatus(base.getStatus());
        dto.setPinned(base.getPinned());
        dto.setFeatured(base.getFeatured());
        dto.setCategoryId(base.getCategoryId());
        dto.setCategoryName(base.getCategoryName());
        dto.setCategorySlug(base.getCategorySlug());
        dto.setAuthorId(base.getAuthorId());
        dto.setAuthorUsername(base.getAuthorUsername());
        dto.setViewCount(base.getViewCount());
        dto.setLikeCount(base.getLikeCount());
        dto.setReplyCount(base.getReplyCount());
        dto.setFavoriteCount(base.getFavoriteCount());
        dto.setAccepted(base.getAccepted());
        dto.setLastReplyAt(base.getLastReplyAt());
        dto.setLastActivityAt(base.getLastActivityAt());
        dto.setCreatedAt(base.getCreatedAt());
        dto.setUpdatedAt(base.getUpdatedAt());
        dto.setTags(base.getTags());
        dto.setLikedByMe(base.getLikedByMe());
        dto.setFavoritedByMe(base.getFavoritedByMe());
        dto.setCanEdit(base.getCanEdit());
        dto.setCanReply(base.getCanReply());
        return dto;
    }

    public List<ForumReplyDto> getReplies() { return replies; }
    public void setReplies(List<ForumReplyDto> replies) { this.replies = replies != null ? replies : new ArrayList<>(); }
    public ForumReplyDto getAcceptedReply() { return acceptedReply; }
    public void setAcceptedReply(ForumReplyDto acceptedReply) { this.acceptedReply = acceptedReply; }
}
