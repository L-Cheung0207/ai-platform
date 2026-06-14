package com.example.platform.repository;

import com.example.platform.entity.GitHubTrendingEntry;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface GitHubTrendingEntryRepository extends JpaRepository<GitHubTrendingEntry, Long> {

    Optional<GitHubTrendingEntry> findByPeriodAndRepoFullName(
            GitHubTrendingEntry.Period period,
            String repoFullName
    );

    @Query("SELECT r FROM GitHubTrendingEntry r WHERE r.period = :period AND r.lastSeenBatch = :batch ORDER BY r.rank ASC")
    List<GitHubTrendingEntry> findLatestByPeriod(
            @Param("period") GitHubTrendingEntry.Period period,
            @Param("batch") Instant batch,
            Pageable pageable
    );

    long countByPeriodAndLastSeenBatch(GitHubTrendingEntry.Period period, Instant batch);
}
