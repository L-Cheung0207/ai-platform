package com.example.platform.repository;

import com.example.platform.entity.ForumReaction;
import com.example.platform.entity.ForumReaction.ReactionType;
import com.example.platform.entity.ForumReaction.TargetType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ForumReactionRepository extends JpaRepository<ForumReaction, Long> {

    Optional<ForumReaction> findByTargetTypeAndTargetIdAndUserIdAndReactionType(
            TargetType targetType, Long targetId, Long userId, ReactionType reactionType);

    List<ForumReaction> findByUserIdAndTargetTypeAndTargetIdInAndReactionType(
            Long userId, TargetType targetType, List<Long> targetIds, ReactionType reactionType);

    long countByTargetTypeAndTargetIdAndReactionType(TargetType targetType, Long targetId, ReactionType reactionType);

    void deleteByTargetTypeAndTargetId(TargetType targetType, Long targetId);

    void deleteByTargetTypeAndTargetIdIn(TargetType targetType, Iterable<Long> targetIds);
}
