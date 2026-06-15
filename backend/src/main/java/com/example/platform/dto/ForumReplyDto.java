package com.example.platform.dto;

import com.example.platform.entity.ForumReply;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class ForumReplyDto {

    private Long id;
    private Long postId;
    private Long parentReplyId;
    private String content;
    private String status;
    private Long likeCount;
    private Long authorId;
    private String authorUsername;
    private Boolean accepted;
    private Boolean likedByMe;
    private Boolean canEdit;
    private Instant createdAt;
    private Instant updatedAt;
    private List<ForumReplyDto> children = new ArrayList<>();

    public static ForumReplyDto fromEntity(ForumReply reply) {
        ForumReplyDto dto = new ForumReplyDto();
        dto.setId(reply.getId());
        dto.setPostId(reply.getPost() != null ? reply.getPost().getId() : null);
        dto.setParentReplyId(reply.getParentReply() != null ? reply.getParentReply().getId() : null);
        dto.setContent(reply.getContent());
        dto.setStatus(reply.getStatus() != null ? reply.getStatus().name() : null);
        dto.setLikeCount(reply.getLikeCount());
        dto.setAuthorId(reply.getAuthor() != null ? reply.getAuthor().getId() : null);
        dto.setAuthorUsername(reply.getAuthor() != null ? reply.getAuthor().getUsername() : null);
        dto.setCreatedAt(reply.getCreatedAt());
        dto.setUpdatedAt(reply.getUpdatedAt());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPostId() { return postId; }
    public void setPostId(Long postId) { this.postId = postId; }
    public Long getParentReplyId() { return parentReplyId; }
    public void setParentReplyId(Long parentReplyId) { this.parentReplyId = parentReplyId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getLikeCount() { return likeCount; }
    public void setLikeCount(Long likeCount) { this.likeCount = likeCount; }
    public Long getAuthorId() { return authorId; }
    public void setAuthorId(Long authorId) { this.authorId = authorId; }
    public String getAuthorUsername() { return authorUsername; }
    public void setAuthorUsername(String authorUsername) { this.authorUsername = authorUsername; }
    public Boolean getAccepted() { return accepted; }
    public void setAccepted(Boolean accepted) { this.accepted = accepted; }
    public Boolean getLikedByMe() { return likedByMe; }
    public void setLikedByMe(Boolean likedByMe) { this.likedByMe = likedByMe; }
    public Boolean getCanEdit() { return canEdit; }
    public void setCanEdit(Boolean canEdit) { this.canEdit = canEdit; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public List<ForumReplyDto> getChildren() { return children; }
    public void setChildren(List<ForumReplyDto> children) { this.children = children != null ? children : new ArrayList<>(); }
}
