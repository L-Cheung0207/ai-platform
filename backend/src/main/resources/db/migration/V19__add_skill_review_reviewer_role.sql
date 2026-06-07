ALTER TABLE skill_reviews ADD COLUMN reviewer_role VARCHAR(40) NOT NULL DEFAULT 'TECH_LEAD';

CREATE INDEX idx_skill_reviews_reviewer_role ON skill_reviews(reviewer_role);
