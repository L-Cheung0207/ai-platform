package com.example.platform.repository;

import com.example.platform.entity.Skill;
import com.example.platform.entity.Skill.AssetLevel;
import com.example.platform.entity.Skill.BuildPriority;
import com.example.platform.entity.Skill.LifecycleStatus;
import com.example.platform.entity.Skill.SkillCategory;
import com.example.platform.entity.Skill.Visibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SkillRepository extends JpaRepository<Skill, Long> {

    List<Skill> findTop10ByVisibilityOrderByUpdatedAtDesc(Visibility visibility);

    Page<Skill> findByUploaderIdOrderByUpdatedAtDesc(Long uploaderId, Pageable pageable);

    Page<Skill> findByVisibilityAndUploaderIdOrderByUpdatedAtDesc(Visibility visibility, Long uploaderId, Pageable pageable);

    Optional<Skill> findFirstBySkillDirectoryIgnoreCase(String skillDirectory);

    @Query("SELECT s FROM Skill s WHERE s.visibility = :vis " +
           "AND (:assetLevel IS NULL OR s.assetLevel = :assetLevel) " +
           "AND (:lifecycleStatus IS NULL OR s.lifecycleStatus = :lifecycleStatus) " +
           "AND (:skillCategory IS NULL OR s.skillCategory = :skillCategory) " +
           "AND (:buildPriority IS NULL OR s.buildPriority = :buildPriority) " +
           "AND (:keyword IS NULL OR :keyword = '' OR LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(s.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Skill> findVisibleByCategoryAndKeyword(
            @Param("vis") Visibility visibility,
            @Param("assetLevel") AssetLevel assetLevel,
            @Param("lifecycleStatus") LifecycleStatus lifecycleStatus,
            @Param("skillCategory") SkillCategory skillCategory,
            @Param("buildPriority") BuildPriority buildPriority,
            @Param("keyword") String keyword,
            Pageable pageable);

    @Query("SELECT s FROM Skill s WHERE " +
           "(:assetLevel IS NULL OR s.assetLevel = :assetLevel) " +
           "AND (:lifecycleStatus IS NULL OR s.lifecycleStatus = :lifecycleStatus) " +
           "AND (:skillCategory IS NULL OR s.skillCategory = :skillCategory) " +
           "AND (:buildPriority IS NULL OR s.buildPriority = :buildPriority) " +
           "AND (:keyword IS NULL OR :keyword = '' OR LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(s.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Skill> findAllForAdmin(
            @Param("keyword") String keyword,
            @Param("assetLevel") AssetLevel assetLevel,
            @Param("lifecycleStatus") LifecycleStatus lifecycleStatus,
            @Param("skillCategory") SkillCategory skillCategory,
            @Param("buildPriority") BuildPriority buildPriority,
            Pageable pageable);

    @Query("SELECT s FROM Skill s JOIN s.tags t WHERE s.visibility = :vis AND t.id IN :tagIds " +
           "AND (:assetLevel IS NULL OR s.assetLevel = :assetLevel) " +
           "AND (:lifecycleStatus IS NULL OR s.lifecycleStatus = :lifecycleStatus) " +
           "AND (:skillCategory IS NULL OR s.skillCategory = :skillCategory) " +
           "AND (:buildPriority IS NULL OR s.buildPriority = :buildPriority) " +
           "AND (:keyword IS NULL OR :keyword = '' OR LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(s.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "GROUP BY s.id HAVING COUNT(DISTINCT t.id) = :tagCount")
    Page<Skill> findVisibleByCategoryKeywordAndTagIds(
            @Param("vis") Visibility visibility,
            @Param("assetLevel") AssetLevel assetLevel,
            @Param("lifecycleStatus") LifecycleStatus lifecycleStatus,
            @Param("skillCategory") SkillCategory skillCategory,
            @Param("buildPriority") BuildPriority buildPriority,
            @Param("keyword") String keyword,
            @Param("tagIds") java.util.List<Long> tagIds,
            @Param("tagCount") long tagCount,
            Pageable pageable);
}
