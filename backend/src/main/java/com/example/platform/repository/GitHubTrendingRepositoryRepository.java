package com.example.platform.repository;

import com.example.platform.entity.GitHubTrendingRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface GitHubTrendingRepositoryRepository extends JpaRepository<GitHubTrendingRepository, Long> {

    Optional<GitHubTrendingRepository> findByPeriodAndRepoFullName(
            GitHubTrendingRepository.Period period,
            String repoFullName
    );

    @Query("SELECT r FROM GitHubTrendingRepository r WHERE r.period = :period AND r.lastSeenBatch = :batch ORDER BY r.rank ASC")
    List<GitHubTrendingRepository> findLatestByPeriod(
            GitHubTrendingRepository.Period period,
            Instant batch,
            Pageable pageable
    );

    long countByPeriodAndLastSeenBatch(GitHubTrendingRepository.Period period, Instant batch);
}
