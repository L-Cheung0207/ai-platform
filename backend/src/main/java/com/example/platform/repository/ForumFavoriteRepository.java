package com.example.platform.repository;

import com.example.platform.entity.ForumFavorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ForumFavoriteRepository extends JpaRepository<ForumFavorite, Long> {

    Optional<ForumFavorite> findByPostIdAndUserId(Long postId, Long userId);

    List<ForumFavorite> findByUserIdAndPostIdIn(Long userId, List<Long> postIds);

    List<ForumFavorite> findByUserIdOrderByCreatedAtDesc(Long userId);
}
