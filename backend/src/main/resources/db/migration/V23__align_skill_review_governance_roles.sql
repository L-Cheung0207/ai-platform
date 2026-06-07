UPDATE skill_reviews
SET reviewer_role = 'TECHNICAL_COMMITTEE',
    review_stage = 'COMPANY_REVIEW'
WHERE skill_id IN (
    SELECT id FROM skills
    WHERE asset_level = 'COMPANY'
      AND risk_level <> 'HIGH'
);

UPDATE skill_reviews
SET reviewer_role = 'SECURITY_QUALITY',
    review_stage = 'COMPANY_REVIEW'
WHERE skill_id IN (
    SELECT id FROM skills
    WHERE risk_level = 'HIGH'
);
