ALTER TABLE skills ADD COLUMN creation_source VARCHAR(40) NOT NULL DEFAULT 'MANUAL';

UPDATE skills
SET creation_source = 'SEED'
WHERE source_repository_url LIKE 'https://git.example.com/ai-skills/%';
