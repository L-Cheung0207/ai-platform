CREATE TABLE github_trending_repositories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    period VARCHAR(20) NOT NULL,
    rank_no INT NOT NULL,
    repo_full_name VARCHAR(255) NOT NULL,
    repo_url VARCHAR(500) NOT NULL,
    description VARCHAR(1000),
    language VARCHAR(100),
    stars INT,
    forks INT,
    stars_gained INT,
    effect_cn VARCHAR(1000),
    scenario_cn VARCHAR(1000),
    summary_status VARCHAR(30) NOT NULL,
    source_fetched_at TIMESTAMP,
    last_seen_batch TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_github_trending_period_repo UNIQUE (period, repo_full_name)
);

CREATE INDEX idx_github_trending_period_batch_rank
    ON github_trending_repositories (period, last_seen_batch, rank_no);

CREATE TABLE github_trending_config (
    id BIGINT PRIMARY KEY,
    language_filter VARCHAR(100),
    keyword_filter VARCHAR(500),
    home_display_count INT NOT NULL DEFAULT 10,
    refresh_cron VARCHAR(100) NOT NULL DEFAULT '0 0 8 * * *',
    last_sync_status VARCHAR(30) NOT NULL DEFAULT 'IDLE',
    last_sync_error VARCHAR(1000),
    last_sync_started_at TIMESTAMP,
    last_sync_finished_at TIMESTAMP,
    latest_weekly_batch TIMESTAMP,
    latest_monthly_batch TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
