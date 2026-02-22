-- External skills: description 为简要，content 为正文（Markdown）
ALTER TABLE external_skills ADD COLUMN content TEXT NULL AFTER description;
