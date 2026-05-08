# AGENTS.md

## Purpose
`Mein Windpark` is a Kotlin Multiplatform app (Android + iOS) that makes wind energy transparent for users in Germany.

## Project Context
This project is part of a university seminar. The app is developed for the Umweltbundesamt as the seminar "customer".

App users are everyone who wants to get to know wind turbines and wind parks, not only expert or institutional users.

Primary user value:
- Discover nearby wind parks.
- Search wind parks quickly and revisit recent parks.
- Understand local production context for a park and its municipality (`Gemeinde`).
- Read a practical FAQ that addresses common wind-energy questions and skepticism.

## Product Scope (Current MVP Direction)
The app has 3 top-level pages plus a detail flow:

1. `Map` page  
   - Show wind parks on a map.  
   - Allow opening a park detail.  
   - Allow favoriting a park.

2. `Search` page  
   - Search for wind parks.  
   - Keep local search history.  
   - Allow opening park detail directly.

3. `FAQ` page  
   - Explain core wind-energy topics and common critiques (example: impact on birds).

Cross-page:
- `Park Detail / Production` view for a selected wind park and related municipality data.
- Reachable from both Map and Search.

## Data Strategy
- Local-first storage for now: SQLite on device.
- Shared schema files live in:
  - `composeApp/src/commonMain/sqldelight`
- Data layer boundaries:
  - UI -> ViewModel/UseCase -> Repository -> Local DB/DAO
- UI must not call DB/SQL directly.

## Project Structure (Working Contract)
- `composeApp`: shared Kotlin/Compose code for Android + iOS.
- `iosApp`: native iOS launcher.

Inside `composeApp/src/commonMain/kotlin/app`:
- `navigation/`: routes + app nav host.
- `feature/map/`, `feature/search/`, `feature/detail/`, `feature/faq/`: feature UI/state/viewmodel.
- `core/`: shared UI primitives, models, utilities.
- `data/`: repository interfaces, local entities/DAO contracts, seed import contracts.

Platform-specific code must stay thin:
- `androidMain`: Android-only integrations.
- `iosMain`: iOS-only integrations.

## Naming + Conventions
- Use `FeatureScreen`, `FeatureViewModel`, `FeatureUiState`.
- Keep one package per feature.
- Prefer shared code in `commonMain` unless platform API forces otherwise.
- Keep architecture explicit and boring: avoid hidden side effects.

## Quality Bar for New Tasks
For each feature task:
1. Keep changes scoped to the relevant feature package.
2. Add/adjust repository contract if data behavior changes.
3. Keep navigation behavior explicit (including back behavior).
4. Do not add or update tests. Automated tests are not part of the stated seminar goal for this app.
5. Keep placeholders temporary: replace with production UI/state progressively.

## Current Baseline Notes
- Navigation shell and feature placeholders exist.
- Data layer interfaces exist, but SQLite implementation is still scaffold-level.
- SQL schema files are placeholders and need real table/query definitions.

## Non-Goals (for now)
- No backend dependency required for baseline feature development.
- No cross-device sync yet.
- No analytics/telemetry requirement yet.
- No automated test implementation. Do not spend project time writing unit, integration, UI, or snapshot tests.

## Definition of Done (per vertical slice)
A feature slice is done when:
- UI flow works on Android + iOS entry points.
- State and actions are wired through ViewModel/repository boundaries.
- Required local persistence is implemented (not just interfaces).
- Behavior is documented in code-level comments where non-obvious.
