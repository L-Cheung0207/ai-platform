package com.example.platform.repository;

import com.example.platform.entity.ForumReply;
import com.example.platform.entity.ForumReply.ReplyStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ForumReplyRepository extends JpaRepository<ForumReply, Long> {

    @EntityGraph(attributePaths = {"author", "parentReply", "post"})
    List<ForumReply> findByPostIdAndStatusOrderByCreatedAtAsc(Long postId, ReplyStatus status);

    @EntityGraph(attributePaths = {"author", "parentReply", "post"})
    List<ForumReply> findByAuthorIdAndStatusNotOrderByCreatedAtDesc(Long authorId, ReplyStatus status);

    @EntityGraph(attributePaths = {"author", "parentReply", "post"})
    Optional<ForumReply> findWithGraphById(Long id);

    Optional<ForumReply> findByIdAndPostId(Long id, Long postId);
}
