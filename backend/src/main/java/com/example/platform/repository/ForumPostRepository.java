package com.example.platform.repository;

import com.example.platform.entity.ForumPost;
import com.example.platform.entity.ForumPost.PostStatus;
import com.example.platform.entity.ForumPost.PostType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ForumPostRepository extends JpaRepository<ForumPost, Long> {

    @EntityGraph(attributePaths = {"category", "author", "tags"})
    @Query("""
        SELECT DISTINCT p FROM ForumPost p
        WHERE p.status IN :statuses
          AND p.category.enabled = true
          AND (:categoryId IS NULL OR p.category.id = :categoryId)
          AND (:postType IS NULL OR p.postType = :postType)
          AND (:keyword IS NULL OR :keyword = '' OR LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%')))
        """)
    List<ForumPost> searchPublicPosts(
            @Param("statuses") List<PostStatus> statuses,
            @Param("categoryId") Long categoryId,
            @Param("postType") PostType postType,
            @Param("keyword") String keyword);

    @EntityGraph(attributePaths = {"category", "author", "tags"})
    @Query("""
        SELECT DISTINCT p FROM ForumPost p JOIN p.tags t
        WHERE p.status IN :statuses
          AND p.category.enabled = true
          AND (:categoryId IS NULL OR p.category.id = :categoryId)
          AND (:postType IS NULL OR p.postType = :postType)
          AND (:keyword IS NULL OR :keyword = '' OR LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND t.id IN :tagIds
        GROUP BY p.id
        HAVING COUNT(DISTINCT t.id) = :tagCount
        """)
    List<ForumPost> searchPublicPostsByTags(
            @Param("statuses") List<PostStatus> statuses,
            @Param("categoryId") Long categoryId,
            @Param("postType") PostType postType,
            @Param("keyword") String keyword,
            @Param("tagIds") List<Long> tagIds,
            @Param("tagCount") long tagCount);

    @EntityGraph(attributePaths = {"category", "author", "tags"})
    @Query("""
        SELECT DISTINCT p FROM ForumPost p
        WHERE (:categoryId IS NULL OR p.category.id = :categoryId)
          AND (:status IS NULL OR p.status = :status)
          AND (:postType IS NULL OR p.postType = :postType)
          AND (:keyword IS NULL OR :keyword = '' OR LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%')))
        """)
    List<ForumPost> searchAdminPosts(
            @Param("categoryId") Long categoryId,
            @Param("status") PostStatus status,
            @Param("postType") PostType postType,
            @Param("keyword") String keyword);

    @EntityGraph(attributePaths = {"category", "author", "tags", "acceptedReply"})
    Optional<ForumPost> findWithGraphById(Long id);

    List<ForumPost> findByAuthorIdAndStatusNotOrderByCreatedAtDesc(Long authorId, PostStatus status);

    List<ForumPost> findTop10ByStatusOrderByLastActivityAtDesc(PostStatus status);
}
