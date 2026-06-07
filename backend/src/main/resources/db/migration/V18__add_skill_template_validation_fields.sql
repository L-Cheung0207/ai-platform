ALTER TABLE skills ADD COLUMN source_repository_url VARCHAR(1000) NULL;
ALTER TABLE skills ADD COLUMN skill_directory VARCHAR(255) NULL;
ALTER TABLE skills ADD COLUMN quality_standard TEXT NULL;
ALTER TABLE skills ADD COLUMN reference_materials TEXT NULL;
ALTER TABLE skills ADD COLUMN template_validation_status VARCHAR(20) NOT NULL DEFAULT 'UNVALIDATED';
ALTER TABLE skills ADD COLUMN template_validation_notes TEXT NULL;
ALTER TABLE skills ADD COLUMN last_validated_at TIMESTAMP NULL;
