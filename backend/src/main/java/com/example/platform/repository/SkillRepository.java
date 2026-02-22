package com.example.platform.repository;

import com.example.platform.entity.Skill;
import com.example.platform.entity.Skill.Visibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SkillRepository extends JpaRepository<Skill, Long> {

    List<Skill> findTop10ByVisibilityOrderByUpdatedAtDesc(Visibility visibility);

    Page<Skill> findByUploaderIdOrderByUpdatedAtDesc(Long uploaderId, Pageable pageable);

    Page<Skill> findByVisibilityAndUploaderIdOrderByUpdatedAtDesc(Visibility visibility, Long uploaderId, Pageable pageable);

    @Query("SELECT s FROM Skill s WHERE s.visibility = :vis " +
           "AND (:keyword IS NULL OR :keyword = '' OR LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(s.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Skill> findVisibleByCategoryAndKeyword(
            @Param("vis") Visibility visibility,
            @Param("keyword") String keyword,
            Pageable pageable);

    @Query("SELECT s FROM Skill s WHERE (:keyword IS NULL OR :keyword = '' OR LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(s.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Skill> findAllForAdmin(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT s FROM Skill s JOIN s.tags t WHERE s.visibility = :vis AND t.id IN :tagIds " +
           "AND (:keyword IS NULL OR :keyword = '' OR LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(s.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "GROUP BY s.id HAVING COUNT(DISTINCT t.id) = :tagCount")
    Page<Skill> findVisibleByCategoryKeywordAndTagIds(
            @Param("vis") Visibility visibility,
            @Param("keyword") String keyword,
            @Param("tagIds") java.util.List<Long> tagIds,
            @Param("tagCount") long tagCount,
            Pageable pageable);
}
