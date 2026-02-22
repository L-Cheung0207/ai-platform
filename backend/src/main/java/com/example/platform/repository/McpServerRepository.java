package com.example.platform.repository;

import com.example.platform.entity.McpServer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface McpServerRepository extends JpaRepository<McpServer, Long> {

    Page<McpServer> findAllByOrderByIdDesc(Pageable pageable);

    @Query(value = "SELECT * FROM mcp_servers WHERE (CONCAT(',', IFNULL(tag_names,''), ',') LIKE CONCAT('%,', ?1, ',%')) ORDER BY id DESC",
            countQuery = "SELECT COUNT(*) FROM mcp_servers WHERE (CONCAT(',', IFNULL(tag_names,''), ',') LIKE CONCAT('%,', ?1, ',%'))",
            nativeQuery = true)
    Page<McpServer> findByTagOrderByIdDesc(String tag, Pageable pageable);

    Page<McpServer> findByNameContainingOrDescriptionContainingOrderByIdDesc(String nameKeyword, String descKeyword, Pageable pageable);

    boolean existsByNameIgnoreCase(String name);

    @Query(value = "SELECT tag_names FROM mcp_servers WHERE tag_names IS NOT NULL AND tag_names != ''", nativeQuery = true)
    List<String> findAllTagNames();
}
