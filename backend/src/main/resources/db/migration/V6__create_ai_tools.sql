-- AI工具表：爬取 ai.codefather.cn/tool
CREATE TABLE IF NOT EXISTS ai_tools (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    url VARCHAR(500),
    logo_url VARCHAR(500),
    logo_path VARCHAR(500),
    category_name VARCHAR(100),
    source_url VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ai_tools_category ON ai_tools(category_name);
CREATE INDEX idx_ai_tools_source_url ON ai_tools(source_url);
