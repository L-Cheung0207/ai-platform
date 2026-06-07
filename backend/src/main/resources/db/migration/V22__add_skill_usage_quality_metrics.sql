ALTER TABLE skill_usage_events
    ADD COLUMN newcomer_onboarding_saved_minutes INT NOT NULL DEFAULT 0;

ALTER TABLE skill_usage_events
    ADD COLUMN review_issues_before INT;

ALTER TABLE skill_usage_events
    ADD COLUMN review_issues_after INT;

ALTER TABLE skill_usage_events
    ADD COLUMN test_coverage_before DOUBLE;

ALTER TABLE skill_usage_events
    ADD COLUMN test_coverage_after DOUBLE;

UPDATE skill_usage_events
SET review_issues_before = 8,
    review_issues_after = 3
WHERE skill_id IN (
    SELECT id FROM skills WHERE skill_directory = 'java-code-review'
);

UPDATE skill_usage_events
SET test_coverage_before = 52.0,
    test_coverage_after = 68.0
WHERE skill_id IN (
    SELECT id FROM skills WHERE skill_directory = 'unit-test-generator'
);

UPDATE skill_usage_events
SET newcomer_onboarding_saved_minutes = 120
WHERE skill_id IN (
    SELECT id FROM skills WHERE skill_directory = 'tech-solution-draft'
);
