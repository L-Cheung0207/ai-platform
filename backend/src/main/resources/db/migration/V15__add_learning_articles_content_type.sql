-- 文章内容类型：RICH_TEXT=富文本HTML，MARKDOWN=原始Markdown
ALTER TABLE learning_articles ADD COLUMN content_type VARCHAR(20) NOT NULL DEFAULT 'RICH_TEXT';
