-- Add content_md (Markdown) column to skills and rules

ALTER TABLE skills ADD COLUMN content_md MEDIUMTEXT NULL AFTER clone_command;
ALTER TABLE rules ADD COLUMN content_md MEDIUMTEXT NULL AFTER content;
