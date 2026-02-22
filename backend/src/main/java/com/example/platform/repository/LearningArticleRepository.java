package com.example.platform.repository;

import com.example.platform.entity.LearningArticle;
import com.example.platform.entity.LearningArticle.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LearningArticleRepository extends JpaRepository<LearningArticle, Long> {

    Page<LearningArticle> findAllByOrderByUpdatedAtDesc(Pageable pageable);

    Page<LearningArticle> findByStatusOrderByUpdatedAtDesc(Status status, Pageable pageable);

    java.util.List<LearningArticle> findTop5ByStatusOrderByUpdatedAtDesc(Status status);
}
