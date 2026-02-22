-- 移除 MCP 服务器的 category_name 字段，仅使用 tag_names
ALTER TABLE mcp_servers DROP INDEX idx_mcp_servers_category;
ALTER TABLE mcp_servers DROP COLUMN category_name;
