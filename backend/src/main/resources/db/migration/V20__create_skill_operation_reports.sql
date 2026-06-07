CREATE TABLE IF NOT EXISTS skill_operation_reports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    report_month VARCHAR(7) NOT NULL,
    generated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    monthly_usage_count BIGINT NOT NULL DEFAULT 0,
    monthly_feedback_count BIGINT NOT NULL DEFAULT 0,
    monthly_review_count BIGINT NOT NULL DEFAULT 0,
    monthly_saved_hours DOUBLE NOT NULL DEFAULT 0,
    monthly_feedback_closed_rate DOUBLE NOT NULL DEFAULT 0,
    monthly_review_pass_rate DOUBLE NOT NULL DEFAULT 0,
    markdown MEDIUMTEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_skill_operation_reports_month ON skill_operation_reports(report_month);
CREATE INDEX idx_skill_operation_reports_generated_at ON skill_operation_reports(generated_at);
