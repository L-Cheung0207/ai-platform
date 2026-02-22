-- Remove content_md from rules (Rule 不需要 Markdown 字段)
ALTER TABLE rules DROP COLUMN content_md;
