UPDATE skills SET asset_level = 'TEAM' WHERE asset_level = 'PERSONAL';
ALTER TABLE skills ALTER COLUMN asset_level SET DEFAULT 'TEAM';
