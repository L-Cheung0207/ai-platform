# GitHub Trending Module Design

Date: 2026-06-14

## Goal

Add a GitHub Trending module to the platform home page. The module should show weekly and monthly GitHub Trending repositories, with Chinese summaries organized around "排名 / 仓库 / 作用 / 场景".

The system should refresh data automatically every morning, allow an admin to refresh manually, and allow admins to revise generated Chinese summaries.

## Scope

In scope:

- Fetch GitHub Trending weekly and monthly repository lists.
- Store trending records in the backend database.
- Generate initial Chinese "作用" and "场景" text for each repository.
- Preserve admin-edited Chinese summaries during later refreshes.
- Show a compact weekly/monthly tab module on the home page.
- Add admin controls for sync status, refresh, configuration, and summary editing.

Out of scope for the first version:

- A separate public GitHub Trending detail page.
- User subscriptions or notifications.
- Daily Trending display.
- Direct runtime invocation of local Codex Skill files.

## Skill Reuse Decision

The local `github-trending-rankings` Skill is useful as a content guideline, but it is not an application runtime dependency. It contains instructions for an agent, not executable code or a stable API.

The application should reuse the Skill's output rules as backend prompt and formatting constraints:

- Keep repository names as `owner/repo`.
- Produce short, plain Chinese text.
- Generate exactly the user-facing concepts "作用" and "场景".
- If the repository purpose is uncertain, mark the generated summary as needing review instead of guessing too aggressively.

## User Experience

### Home Page

Add a `GitHub Trending` section to the existing home page right sidebar, near the current leaderboard and news modules.

The module shows:

- Title: `GitHub Trending`
- Last successful update time.
- Tabs: `周榜` and `月榜`.
- A compact ranked list, defaulting to 10 items.

Each item shows:

- Rank.
- Repository full name, linked to the GitHub repository.
- Chinese "作用".
- Chinese "场景" in a secondary line or hover/expanded text, depending on available space.

On narrow screens, the module can display fewer items if needed to keep the home page readable.

The home page should continue to load through the existing `/api/home` aggregate endpoint. Add `githubTrendingWeekly`, `githubTrendingMonthly`, and `githubTrendingUpdatedAt` fields to the home DTO.

### Admin Page

Add a new admin route: `/admin/github-trending`.

The page follows the style of the existing LLM leaderboard admin page and contains:

- Sync status cards: last sync time, weekly count, monthly count, summary status, latest error.
- Sync configuration: language filter, keyword filter, home display count, daily refresh time.
- Manual actions: refresh all trending data, regenerate a single repository summary.
- Editable table: tabs for weekly/monthly, with columns for rank, repository, effect, scenario, status, and actions.

Only `ADMIN` users can access admin APIs and UI.

## Backend Architecture

### Entities

Add `GitHubTrendingRepository` with these core fields:

- `id`
- `period`: `WEEKLY` or `MONTHLY`
- `rank`
- `repoFullName`
- `repoUrl`
- `description`
- `language`
- `stars`
- `forks`
- `starsGained`
- `effectCn`
- `scenarioCn`
- `summaryStatus`: `GENERATED`, `MANUAL`, `NEEDS_REVIEW`, or `FAILED`
- `sourceFetchedAt`
- `createdAt`
- `updatedAt`

Add a small config/status persistence model, either as a dedicated `GitHubTrendingConfig` entity or an existing settings-style table if one exists during implementation. Required settings:

- Enabled language filter, optional.
- Keyword filter list, optional.
- Home display count, default 10.
- Daily refresh time, default morning.
- Last sync status and latest error.

### Services

Use focused services:

- `GitHubTrendingScraperService`: fetch and parse GitHub Trending pages.
- `GitHubTrendingSummaryService`: create Chinese "作用 / 场景" summaries.
- `GitHubTrendingService`: orchestrate sync, upsert, admin edits, and home queries.
- `GitHubTrendingScheduler`: trigger daily refresh.

The scraper fetches:

- `https://github.com/trending?since=weekly`
- `https://github.com/trending?since=monthly`

If a language filter is configured, the scraper builds the corresponding GitHub Trending language URL. If keyword filters are configured, the service filters fetched repositories after parsing.

### Sync Behavior

Each daily sync fetches weekly and monthly lists.

Use upsert by `period + repoFullName`:

- Update rank and GitHub metadata every sync.
- Generate summaries only for new records or records explicitly marked for regeneration.
- Preserve `effectCn` and `scenarioCn` when `summaryStatus=MANUAL`.
- Do not delete old successful data before a new fetch succeeds.

Records that disappear from Trending can remain in the database but should not appear in home results unless they belong to the latest successful fetch for that period.

### Summary Generation

The summary service receives repository name, description, language, README excerpt when available, and repository URL.

If model/API configuration exists, the service calls the configured model with constraints based on the local Skill rules. If no model is configured or generation fails, it falls back to conservative text based on the GitHub description and sets `summaryStatus=NEEDS_REVIEW` or `FAILED`.

Admin edits set `summaryStatus=MANUAL`.

## API Design

Public:

- Extend `GET /api/home` to include weekly and monthly GitHub Trending lists.

Admin:

- `GET /api/admin/github-trending/status`
- `GET /api/admin/github-trending?period=WEEKLY|MONTHLY`
- `PUT /api/admin/github-trending/{id}` to edit `effectCn` and `scenarioCn`.
- `POST /api/admin/github-trending/sync` to manually refresh all configured periods.
- `POST /api/admin/github-trending/{id}/regenerate-summary`
- `GET /api/admin/github-trending/config`
- `PUT /api/admin/github-trending/config`

All admin endpoints require `ADMIN`.

## Error Handling

If GitHub fetching fails:

- Keep the previous successful data visible.
- Mark sync status as `FAILED`.
- Store the latest error for admin visibility.

If a repository README fetch fails:

- Continue with GitHub description and metadata.
- Mark the summary as needing review if the generated text is too weak.

If summary generation fails:

- Keep the repository record.
- Set `summaryStatus=FAILED` or `NEEDS_REVIEW`.
- Allow admin retry or manual edit.

If manual sync is triggered while a sync is already running:

- Reject the second request with a clear message or return the current running status.

## Testing

Backend tests:

- Parse GitHub Trending HTML into repository records.
- Sync weekly and monthly data.
- Upsert existing records without overwriting manual summaries.
- Fall back when model configuration is missing.
- Keep previous home data when a sync fails.
- Enforce `ADMIN` access for admin endpoints.
- Return configured display counts in `/api/home`.

Frontend verification:

- Home page displays weekly/monthly tabs and uses home aggregate data.
- Admin page can load status, switch period tabs, edit summaries, and trigger refresh.
- Build succeeds with the new route and components.

## Open Implementation Notes

Implementation should check whether the project already has a generic settings table before adding a dedicated config entity. If no generic settings model exists, use a dedicated GitHub Trending config/status model to keep the feature isolated.

The first implementation should avoid a separate public detail page. Links should go directly to GitHub repositories.
