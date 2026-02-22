-- 大语言模型排行榜（数据来源 OpenLM Chatbot Arena）
CREATE TABLE IF NOT EXISTS llm_leaderboard (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    model_name VARCHAR(300) NOT NULL,
    model_url VARCHAR(500),
    rank_badge VARCHAR(20),
    arena_elo INT,
    coding INT,
    vision INT,
    aaii INT,
    mmlu_pro INT,
    arc_agi INT,
    organization VARCHAR(200),
    license_name VARCHAR(100),
    display_order INT NOT NULL DEFAULT 0,
    scraped_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_llm_leaderboard_display_order ON llm_leaderboard(display_order);
CREATE INDEX idx_llm_leaderboard_arena_elo ON llm_leaderboard(arena_elo);
CREATE INDEX idx_llm_leaderboard_model_name ON llm_leaderboard(model_name);
