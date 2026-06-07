ALTER TABLE skill_usage_events
    ADD COLUMN toolchain_source VARCHAR(40) NOT NULL DEFAULT 'MANUAL';

ALTER TABLE skill_usage_events
    ADD COLUMN external_event_id VARCHAR(255);

ALTER TABLE skill_usage_events
    ADD COLUMN repository VARCHAR(500);

ALTER TABLE skill_usage_events
    ADD COLUMN branch_name VARCHAR(255);

ALTER TABLE skill_usage_events
    ADD COLUMN commit_sha VARCHAR(100);

ALTER TABLE skill_usage_events
    ADD COLUMN ci_status VARCHAR(40);

CREATE INDEX idx_skill_usage_events_toolchain_source ON skill_usage_events(toolchain_source);

CREATE INDEX idx_skill_usage_events_external_event_id ON skill_usage_events(external_event_id);

CREATE UNIQUE INDEX uk_skill_usage_events_skill_external_event
    ON skill_usage_events(skill_id, external_event_id);
