package com.example.platform.repository;

import com.example.platform.entity.LlmLeaderboardEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface LlmLeaderboardRepository extends JpaRepository<LlmLeaderboardEntry, Long> {

    @Query("SELECT e FROM LlmLeaderboardEntry e WHERE (:keyword IS NULL OR :keyword = '' OR LOWER(e.modelName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(e.organization) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<LlmLeaderboardEntry> findByKeyword(String keyword, Pageable pageable);
}
