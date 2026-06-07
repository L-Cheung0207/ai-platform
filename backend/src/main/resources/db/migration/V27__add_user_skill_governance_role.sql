ALTER TABLE users
    ADD COLUMN skill_governance_role VARCHAR(40) NOT NULL DEFAULT 'TECH_LEAD';

UPDATE users
SET skill_governance_role = 'SECURITY_QUALITY'
WHERE role = 'ADMIN';
