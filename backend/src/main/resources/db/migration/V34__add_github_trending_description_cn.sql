ALTER TABLE github_trending_repositories
    ADD COLUMN description_cn VARCHAR(1000) NULL AFTER description;
