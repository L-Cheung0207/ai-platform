-- 移除 MCP 服务器的 logo_path、source_url 字段
ALTER TABLE mcp_servers DROP INDEX idx_mcp_servers_source_url;
ALTER TABLE mcp_servers DROP COLUMN logo_path;
ALTER TABLE mcp_servers DROP COLUMN source_url;
