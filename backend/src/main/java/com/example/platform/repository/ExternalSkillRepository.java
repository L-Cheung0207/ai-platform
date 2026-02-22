package com.example.platform.repository;

import com.example.platform.entity.ExternalSkill;
import com.example.platform.entity.ExternalSkill.Visibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExternalSkillRepository extends JpaRepository<ExternalSkill, Long> {

    boolean existsByNameIgnoreCase(String name);

    java.util.List<ExternalSkill> findTop10ByVisibilityOrderByUpdatedAtDesc(ExternalSkill.Visibility visibility);

    @Query("SELECT e FROM ExternalSkill e WHERE (:keyword IS NULL OR :keyword = '' OR LOWER(e.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(e.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<ExternalSkill> findAllForAdmin(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT e FROM ExternalSkill e WHERE e.visibility = :vis " +
           "AND (:keyword IS NULL OR :keyword = '' OR LOWER(e.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(e.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<ExternalSkill> findVisibleByCategoryAndKeyword(
            @Param("vis") Visibility visibility,
            @Param("keyword") String keyword,
            Pageable pageable);

    @Query("SELECT e FROM ExternalSkill e JOIN e.tags t WHERE e.visibility = :vis AND t.id IN :tagIds " +
           "AND (:keyword IS NULL OR :keyword = '' OR LOWER(e.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(e.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "GROUP BY e.id HAVING COUNT(DISTINCT t.id) = :tagCount")
    Page<ExternalSkill> findVisibleByCategoryKeywordAndTagIds(
            @Param("vis") Visibility visibility,
            @Param("keyword") String keyword,
            @Param("tagIds") java.util.List<Long> tagIds,
            @Param("tagCount") long tagCount,
            Pageable pageable);
}
