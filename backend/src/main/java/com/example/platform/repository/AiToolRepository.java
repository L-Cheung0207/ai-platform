package com.example.platform.repository;

import com.example.platform.entity.AiTool;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AiToolRepository extends JpaRepository<AiTool, Long> {

    Page<AiTool> findAllByOrderByIdDesc(Pageable pageable);

    Page<AiTool> findByCategoryNameOrderByIdDesc(String categoryName, Pageable pageable);

    @Query(value = "SELECT * FROM ai_tools WHERE category_name = ?1 OR (CONCAT(',', IFNULL(tag_names,''), ',') LIKE CONCAT('%,', ?1, ',%')) ORDER BY id DESC",
            countQuery = "SELECT COUNT(*) FROM ai_tools WHERE category_name = ?1 OR (CONCAT(',', IFNULL(tag_names,''), ',') LIKE CONCAT('%,', ?1, ',%'))",
            nativeQuery = true)
    Page<AiTool> findByCategoryOrTagOrderByIdDesc(String categoryOrTag, Pageable pageable);

    Page<AiTool> findByNameContainingOrDescriptionContainingOrderByIdDesc(String nameKeyword, String descKeyword, Pageable pageable);

    boolean existsBySourceUrl(String sourceUrl);

    @Query("SELECT DISTINCT t.categoryName FROM AiTool t WHERE t.categoryName IS NOT NULL ORDER BY t.categoryName")
    List<String> findDistinctCategoryNamesOrderByCategoryName();

    @Query(value = "SELECT tag_names FROM ai_tools WHERE tag_names IS NOT NULL AND tag_names != ''", nativeQuery = true)
    List<String> findAllTagNames();
}
