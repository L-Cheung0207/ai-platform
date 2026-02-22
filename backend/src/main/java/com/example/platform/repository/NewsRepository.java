package com.example.platform.repository;

import com.example.platform.entity.News;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NewsRepository extends JpaRepository<News, Long> {

    java.util.List<News> findTop5ByOrderByPublishDateDescCreatedAtDesc();

    java.util.List<News> findTop10ByOrderByPublishDateDescCreatedAtDesc();

    Page<News> findAllByOrderByPublishDateDescCreatedAtDesc(Pageable pageable);

    Page<News> findAllByPublishDateOrderByCreatedAtDesc(java.time.LocalDate publishDate, Pageable pageable);

    Page<News> findByPublishDateBetweenOrderByPublishDateDescCreatedAtDesc(java.time.LocalDate start, java.time.LocalDate end, Pageable pageable);

    boolean existsBySourceUrl(String sourceUrl);

    @Query("SELECT n FROM News n WHERE (:kw IS NULL OR :kw = '' OR LOWER(n.title) LIKE LOWER(CONCAT('%',:kw,'%')) OR LOWER(n.summary) LIKE LOWER(CONCAT('%',:kw,'%'))) AND (:pd IS NULL OR n.publishDate = :pd) ORDER BY n.publishDate DESC, n.createdAt DESC")
    Page<News> findForAdmin(@Param("kw") String keyword, @Param("pd") java.time.LocalDate publishDate, Pageable pageable);
}
