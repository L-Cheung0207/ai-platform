CREATE TABLE IF NOT EXISTS skill_reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    skill_id BIGINT NOT NULL,
    reviewer_name VARCHAR(100) NOT NULL,
    review_stage VARCHAR(30) NOT NULL DEFAULT 'TEAM_REVIEW',
    result VARCHAR(30) NOT NULL,
    truthful BOOLEAN NOT NULL DEFAULT FALSE,
    accurate BOOLEAN NOT NULL DEFAULT FALSE,
    reusable BOOLEAN NOT NULL DEFAULT FALSE,
    executable BOOLEAN NOT NULL DEFAULT FALSE,
    secure BOOLEAN NOT NULL DEFAULT FALSE,
    verifiable BOOLEAN NOT NULL DEFAULT FALSE,
    maintainable BOOLEAN NOT NULL DEFAULT FALSE,
    notes TEXT NULL,
    reviewed_at DATE NOT NULL,
    next_review_at DATE NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (skill_id) REFERENCES skills(id) ON DELETE CASCADE
);

CREATE INDEX idx_skill_reviews_skill_id ON skill_reviews(skill_id);
CREATE INDEX idx_skill_reviews_reviewed_at ON skill_reviews(reviewed_at);

CREATE TABLE IF NOT EXISTS skill_feedback (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    skill_id BIGINT NOT NULL,
    submitter_name VARCHAR(100) NULL,
    feedback_type VARCHAR(30) NOT NULL DEFAULT 'IMPROVEMENT',
    content TEXT NOT NULL,
    estimated_saved_minutes INT NOT NULL DEFAULT 0,
    rating INT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (skill_id) REFERENCES skills(id) ON DELETE CASCADE
);

CREATE INDEX idx_skill_feedback_skill_id ON skill_feedback(skill_id);
CREATE INDEX idx_skill_feedback_status ON skill_feedback(status);
CREATE INDEX idx_skill_feedback_created_at ON skill_feedback(created_at);

CREATE TABLE IF NOT EXISTS skill_usage_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    skill_id BIGINT NOT NULL,
    user_name VARCHAR(100) NULL,
    scenario TEXT NULL,
    saved_minutes INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (skill_id) REFERENCES skills(id) ON DELETE CASCADE
);

CREATE INDEX idx_skill_usage_events_skill_id ON skill_usage_events(skill_id);
CREATE INDEX idx_skill_usage_events_created_at ON skill_usage_events(created_at);
