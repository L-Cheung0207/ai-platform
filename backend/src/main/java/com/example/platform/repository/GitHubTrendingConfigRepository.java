package com.example.platform.repository;

import com.example.platform.entity.GitHubTrendingConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GitHubTrendingConfigRepository extends JpaRepository<GitHubTrendingConfig, Long> {
}
