-- 分离简要描述与详细内容：summary 用于列表展示，content 存储 .markdown-body 详细作用
ALTER TABLE ai_tools ADD COLUMN summary VARCHAR(500) NULL AFTER description;
ALTER TABLE ai_tools ADD COLUMN content MEDIUMTEXT NULL AFTER summary;
