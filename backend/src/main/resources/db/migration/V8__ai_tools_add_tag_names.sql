-- 详情页标签：AI写作工具、AI写作 等
ALTER TABLE ai_tools ADD COLUMN tag_names VARCHAR(500) NULL AFTER category_name;
