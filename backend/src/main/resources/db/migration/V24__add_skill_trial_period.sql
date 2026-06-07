ALTER TABLE skills ADD COLUMN trial_started_at DATE NULL;
ALTER TABLE skills ADD COLUMN trial_ends_at DATE NULL;

UPDATE skills
SET trial_started_at = COALESCE(trial_started_at, CURRENT_DATE),
    trial_ends_at = COALESCE(trial_ends_at, next_review_at)
WHERE lifecycle_status = 'TRIAL'
  AND next_review_at IS NOT NULL;
