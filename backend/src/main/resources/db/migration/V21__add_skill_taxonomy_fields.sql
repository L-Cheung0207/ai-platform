ALTER TABLE skills ADD COLUMN skill_category VARCHAR(40) NOT NULL DEFAULT 'CODING_IMPLEMENTATION';
ALTER TABLE skills ADD COLUMN build_priority VARCHAR(10) NOT NULL DEFAULT 'P2';

UPDATE skills SET skill_category = 'CODE_REVIEW', build_priority = 'P0' WHERE skill_directory = 'java-code-review';
UPDATE skills SET skill_category = 'TESTING_VALIDATION', build_priority = 'P0' WHERE skill_directory = 'unit-test-generator';
UPDATE skills SET skill_category = 'DOCUMENTATION_KNOWLEDGE', build_priority = 'P2' WHERE skill_directory = 'api-doc-generator';
UPDATE skills SET skill_category = 'OPS_TROUBLESHOOTING', build_priority = 'P1' WHERE skill_directory = 'incident-triage';
UPDATE skills SET skill_category = 'ARCHITECTURE_DESIGN', build_priority = 'P1' WHERE skill_directory = 'tech-solution-draft';
