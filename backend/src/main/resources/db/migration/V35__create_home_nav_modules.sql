CREATE TABLE IF NOT EXISTS home_nav_modules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    nav_label VARCHAR(100),
    nav_path VARCHAR(200),
    visible BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

INSERT INTO home_nav_modules (code, name, nav_label, nav_path, visible, sort_order) VALUES
    ('SKILLS', 'Skill 资产', 'Skill资产库', '/skills', TRUE, 10),
    ('GITHUB_TRENDING', 'GitHub Trending', NULL, NULL, TRUE, 15),
    ('ARTICLES', 'AI 知识库', 'AI知识库', '/articles', TRUE, 20),
    ('FORUM', '技术论坛', '论坛', '/forum', TRUE, 30),
    ('NEWS', 'AI 资讯', 'AI资讯', '/news', TRUE, 40),
    ('LLM_LEADERBOARD', 'LLM 排行榜', 'LLM 排行榜', '/llm-leaderboard', TRUE, 50);
