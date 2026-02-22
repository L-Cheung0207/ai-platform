<!--
  Sync Impact Report
  Version change: (template/unversioned) → 1.0.0
  Modified principles: N/A (initial fill from template placeholders)
  Added sections: Core Principles (5), Additional Constraints, Development Workflow, Governance
  Removed sections: none
  Templates: plan-template.md (Constitution Check gates will align when /speckit.plan runs) ✅ no file edit;
            spec-template.md ✅ no change; tasks-template.md ✅ no change
  Follow-up TODOs: none
-->

# AI Resource & Learning Platform Constitution

## Core Principles

### I. Code Quality

Code MUST remain readable and maintainable. APIs and data models MUST be defined in the spec and/or implementation plan before implementation. Rationale: reduces rework and keeps implementation aligned with agreed contracts.

### II. Technical Scope

One frontend project (user-facing app and admin app in the same repository) and one backend project. MUST NOT over-split into additional services or repositories for the initial scope. Rationale: simpler deployment and maintenance for an internal platform.

### III. Security & Access

- Target: internal use only (intranet).
- Authentication: account and password only (no SSO required for v1).
- User-facing browse and read MUST NOT require login: all Skill/Rule listings, learning articles, and news are anonymously accessible.
- Login is REQUIRED only for: uploading Skill/Rule, accessing "My uploads" (view/edit/delete own content), and the admin backend.
- Roles: distinguish administrator from normal user; administrators manage categories, learning articles, news, and optional recommendation slots.

Rationale: low friction for consumption; minimal auth surface.

### IV. User Experience

MUST follow existing design norms (see README and styles.css for colors, typography, layout). List and detail views MUST support search, filter, and pagination. Rationale: consistency with current static design and predictable UX.

### V. Implementation Order

Implement in this order: first the core flow (list → detail → copy without login; login → upload → list → detail → copy for contributors), then add learning articles and news. For the first release, MUST NOT introduce complex full-text search; use database filtering (e.g. keyword + category). Rationale: deliver a working main path before expanding scope.

## Additional Constraints

- Technology stack: one Vue 3 frontend (Vite, Vue Router, Pinia) and one Java backend (Spring Boot). Details are defined in the implementation plan per feature.
- No full-text search engine (e.g. Elasticsearch) in v1; list APIs use keyword and category filters against the primary database.

## Development Workflow

- Spec and plan MUST be filled and validated before implementation. Use `/speckit.clarify` to resolve ambiguities before `/speckit.plan`.
- Tasks are derived from the plan; implementation MUST follow the task order and checkpoints.

## Governance

- This constitution overrides ad-hoc practices for this project. Amendments MUST be documented, versioned (semantic versioning: MAJOR.MINOR.PATCH), and reflected in this file with a last-amended date.
- All PRs and reviews SHOULD verify compliance with the principles above. Exceptions or complexity MUST be justified in the plan (e.g. Complexity Tracking in plan.md).

**Version**: 1.0.0 | **Ratified**: 2025-02-13 | **Last Amended**: 2025-02-13
