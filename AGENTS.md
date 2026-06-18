# AGENTS.md

## Source of Truth
- Product scope, roadmap and acceptance criteria live in `docs/product/WindKlar_PRD.md`.
- Domain language lives in `CONTEXT.md`; architectural decisions live in `docs/adr/`.
- Keep this file small: durable repo rules only. Do not duplicate the PRD here.

## Project
`windklar` is a Kotlin Multiplatform app for Android and iOS. It helps people in Germany understand nearby wind parks, production context, municipal benefit, data quality and local data hints.

Seminar context: the app is built for a university course with the Umweltbundesamt as seminar customer. Prefer a coherent, demonstrable MVP over broad speculative features.

Product tone: factual, accessible and transparent about uncertainty. Do not make the app feel like advertising.

## MVP Rules
- `Windanlage` is the atomic MaStR/Open-MaStR-backed source-data and coordinate unit.
- `Windpark` is the citizen-facing UX unit for map, search, favorites, detail and municipality context.
- Use a Germany-wide preprocessed local JSON snapshot imported into SQLDelight; no live API dependency for baseline runtime flows.
- Search belongs inside the `Map` flow as an overlay/sheet, not as a bottom-nav tab.
- `Profile` means `Info & Einstellungen`; no account, auth, logout, notifications or dark-mode controls unless implemented later.
- `ReportWindTurbine` means local `Datenhinweis`, not an official MaStR correction.
- Location is optional, user-initiated and not stored in the MVP.

## Structure
- `composeApp`: shared Kotlin and Compose Multiplatform code.
- `iosApp`: native iOS launcher.
- `composeApp/src/commonMain/kotlin/app/navigation`: route model and `AppNavHost`.
- `composeApp/src/commonMain/kotlin/app/feature/*`: feature UI/state/viewmodel packages.
- `composeApp/src/commonMain/kotlin/app/core`: shared UI, models, theme and utilities.
- `composeApp/src/commonMain/kotlin/app/data`: repositories, DAO contracts, entities and seed import contracts.
- `composeApp/src/commonMain/sqldelight/app/data/local/db`: SQLDelight schema files.

Keep platform code thin:
- `commonMain`: shared UI, state and repository contracts.
- `androidMain`: Android-only integrations.
- `iosMain`: iOS-only integrations.

## Navigation
Current routes:
- `Start`
- `Map`
- `Stats`
- `Favorites`
- `Faq`
- `Profile`
- `Detail(parkId)`

Bottom nav is owned by `AppNavHost` and contains `Map`, `Stats`, `Favorites`, `Faq`, `Profile`.

Rules:
- Do not duplicate bottom-nav ownership inside feature screens.
- Top-level screens should not show their own back button to `Map`.
- Back affordances belong to subflows such as detail, search overlay/sheet, data hint form and turbine subdetail.
- Add `ReportWindTurbine` only when implementing the data-hint slice.

## Data
Use this boundary:

```text
UI -> ViewModel/UseCase -> Repository -> Local DB/DAO
```

UI must not call SQLite, SQLDelight query APIs or SQL directly.

Target local model:
- `wind_turbine`
- `wind_park`
- `metric`
- `favorite_wind_park`
- `recent_wind_park`
- `data_hint`
- optional `snapshot_metadata`

- Source-data preprocessing lives in top-level `data/`; app runtime imports only the bundled app-ready JSON snapshot.
- `RecentWindPark.sq` records every opened park.
- Production and impact values belong in the generic `metric` model.
- Source, timestamp, calculation note and data quality are first-class data.

Data-quality labels: `official`, `measured`, `derived`, `estimated`, `simulated`, `missing`.

## Implementation Rules
- Use `FeatureScreen`, `FeatureViewModel` and `FeatureUiState`.
- Keep one package per feature.
- Prefer shared code in `commonMain`.
- Keep architecture explicit and unsurprising.
- Add or adjust repository contracts when data behavior changes.
- Preserve source, timestamp, quality label and missing-state behavior when displaying metrics.
- Implement charts as Compose UI/Canvas, not static images.
- Use Figma as functional/visual reference only; do not import generated React/Tailwind code.
- Centralize colors, typography, spacing, radii and elevation in Compose theme/design tokens.
- Do not add automated tests for the seminar MVP unless the project direction changes.

## Current Baseline
- App root: `app.App`, wrapping `AppNavHost` in `WindklarTheme`.
- Implemented visual slices: `StartScreen`, `MapScreen`, `FavoritesScreen`, `FaqScreen`, `StatsScreen`, `ProfileScreen`.
- Scaffold slices: `SearchScreen`, `ParkDetailScreen`, map/search/detail viewmodels, database driver factory and snapshot seed importer.
- Missing slice: `ReportWindTurbine` route/package/form.
- UI is mostly mock `UiState`; repositories/DAO contracts are not yet wired through generated SQLDelight APIs.
- AGP 9.x/KMP compatibility warning is accepted for the seminar MVP unless the build breaks.

## Build And Verify
- Android build: `.\gradlew.bat :composeApp:assembleDebug`
- iOS: open `iosApp` in Xcode and run there.
- For docs-only changes, run `git diff --check`.
- For feature changes, verify the relevant manual flow; Android manual QA is required before demo, iOS smoke test is desirable where available.
