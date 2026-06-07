UPDATE users
SET skill_governance_role = 'CONTRIBUTOR'
WHERE role = 'NORMAL'
  AND skill_governance_role = 'TECH_LEAD';
