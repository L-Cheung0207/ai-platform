-- MCP 服务器表：爬取 ai.codefather.cn/mcp
CREATE TABLE IF NOT EXISTS mcp_servers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    summary VARCHAR(500),
    content MEDIUMTEXT,
    url VARCHAR(500),
    logo_url VARCHAR(500),
    logo_path VARCHAR(500),
    category_name VARCHAR(100),
    tag_names VARCHAR(500),
    source_url VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_mcp_servers_category ON mcp_servers(category_name);
CREATE INDEX idx_mcp_servers_source_url ON mcp_servers(source_url);
