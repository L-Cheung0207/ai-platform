package com.example.platform.repository;

import com.example.platform.entity.Rule;
import com.example.platform.entity.Rule.Visibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RuleRepository extends JpaRepository<Rule, Long> {

    java.util.List<Rule> findTop10ByVisibilityOrderByUpdatedAtDesc(Visibility visibility);

    Page<Rule> findByUploaderIdOrderByUpdatedAtDesc(Long uploaderId, Pageable pageable);

    Page<Rule> findByVisibilityAndUploaderIdOrderByUpdatedAtDesc(Visibility visibility, Long uploaderId, Pageable pageable);

    @Query("SELECT r FROM Rule r WHERE (:keyword IS NULL OR :keyword = '' OR LOWER(r.name) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Rule> findAllForAdmin(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT r FROM Rule r WHERE r.visibility = :vis " +
           "AND (:keyword IS NULL OR :keyword = '' OR LOWER(r.name) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Rule> findVisibleByCategoryAndKeyword(
            @Param("vis") Visibility visibility,
            @Param("keyword") String keyword,
            Pageable pageable);

    @Query("SELECT r FROM Rule r JOIN r.tags t WHERE r.visibility = :vis AND t.id IN :tagIds " +
           "AND (:keyword IS NULL OR :keyword = '' OR LOWER(r.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "GROUP BY r.id HAVING COUNT(DISTINCT t.id) = :tagCount")
    Page<Rule> findVisibleByCategoryKeywordAndTagIds(
            @Param("vis") Visibility visibility,
            @Param("keyword") String keyword,
            @Param("tagIds") java.util.List<Long> tagIds,
            @Param("tagCount") long tagCount,
            Pageable pageable);
}
