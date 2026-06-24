# windklar

`windklar` is a Kotlin Multiplatform app for Android and iOS that makes local wind energy easier to understand for people in Germany.

WindKlar helps users discover wind parks, inspect source-backed wind installation data, understand production and municipality context, revisit parks, and create local data-quality hints when public data appears incomplete or incorrect.

The app is built for a university seminar with the Umweltbundesamt as seminar customer. The MVP should be coherent, demonstrable, factual and clear about uncertainty.

## Documentation
- [WindKlar PRD](docs/product/WindKlar_PRD.md): product scope, MVP decisions, roadmap and definition of done.
- [Domain Context](CONTEXT.md): project language and glossary.
- [Architecture Decisions](docs/adr): recorded product and technical decisions.
- [Agent Instructions](AGENTS.md): concise implementation rules for coding agents.

## Product Snapshot
- `Windanlage` is the atomic MaStR/Open-MaStR-backed source-data and coordinate unit.
- `Windpark` is the primary citizen-facing unit for map, search, favorites, detail and municipality context.
- MVP runtime data is local-first via SQLDelight.
- Baseline data should come from a Germany-wide preprocessed JSON snapshot, not live API calls.
- Production, CO2 savings, household equivalents and municipal participation need source, timestamp, data-quality and calculation metadata.
- Search is part of the map flow, not bottom navigation.
- Favorites and recently viewed wind parks are local state.
- `Profile` is `Info & Einstellungen`, not an account area.
- `ReportWindTurbine` is a local `Datenhinweis` flow, not an official correction channel.

## Main Flows
- `Start` -> `Map`.
- Browse wind parks or clusters on the map.
- Search by park, place, municipality or available identifiers inside the map flow.
- Open a park preview and continue to `Detail(parkId)`.
- View impact metrics and data-quality labels.
- Save and revisit wind parks.
- Read FAQ content about wind energy, sources, assumptions and limits.
- Use the app without granting location permission.

## Navigation
Current routes:
- `Start`
- `Map`
- `Stats`
- `Favorites`
- `Faq`
- `Profile`
- `Detail(parkId)`

Bottom navigation is owned by `AppNavHost` and contains:
- `Map`
- `Stats`
- `Favorites`
- `Faq`
- `Profile` / `Info & Einstellungen`

Top-level screens should not include their own back button to `Map`. Back affordances belong to subflows such as detail, search overlay/sheet, data hint form and turbine subdetail.

## Data
Runtime access should follow:

```text
UI -> ViewModel/UseCase -> Repository -> Local DB/DAO
```

UI code must not call SQLite, SQLDelight query APIs or SQL directly.

Target local model:
- `wind_turbine`
- `wind_park`
- `metric`
- `favorite_wind_park`
- `recent_wind_park`
- `data_hint`
- optional `snapshot_metadata`

Current SQLDelight files live in `composeApp/src/commonMain/sqldelight/app/data/local/db`:
- `WindPark.sq`
- `WindTurbine.sq`
- `Metric.sq`
- `Favorite.sq`
- `RecentWindPark.sq`
- `DataHint.sq`
- `SnapshotMetadata.sq`

MaStR/Open-MaStR preprocessing lives outside the app in `data/`. The app imports
only the app-ready JSON snapshot from Compose resources into SQLDelight.

Data-quality labels: `official`, `measured`, `derived`, `estimated`, `simulated`, `missing`.

## Project Structure
- `composeApp`: shared Kotlin and Compose Multiplatform app code.
- `iosApp`: native iOS launcher.
- `composeApp/src/commonMain/kotlin/app/navigation`: routes and app nav host.
- `composeApp/src/commonMain/kotlin/app/feature/*`: feature UI, state and viewmodels.
- `composeApp/src/commonMain/kotlin/app/core`: shared UI, models, theme and utilities.
- `composeApp/src/commonMain/kotlin/app/data`: repositories, DAO contracts, entities and seed import contracts.
- `data`: source-data pipeline, ignored raw/intermediate MaStR files and generated snapshot releases.

Platform-specific code should stay thin. Prefer shared code in `commonMain` unless a platform API requires otherwise.

## Development Notes
- Use `FeatureScreen`, `FeatureViewModel` and `FeatureUiState` naming.
- Keep one package per feature.
- Add or adjust repository contracts when data behavior changes.
- Preserve source, timestamp, data-quality and missing-state behavior when displaying metrics.
- Build charts as Compose UI/Canvas, not static images.
- Treat Figma as a functional/visual reference, not a pixel-perfect contract.
- Do not import Figma-generated React/Tailwind code.
- Do not add automated tests for the seminar MVP unless the project direction changes.

## Current Baseline
- App root: `app.App`, wrapping `AppNavHost` in `WindklarTheme`.
- All screens implemented and wired to repository: `StartScreen`, `MapScreen`, `StatsScreen`, `FavoritesScreen`, `FaqScreen`, `ProfileScreen`, `ParkDetailScreen`, `RegionDetailScreen`.
- Search is implemented inside the `Map` flow as an overlay/sheet; the separate `SearchScreen`/`SearchViewModel`/`SearchUiState` placeholder has been removed.
- `ReportWindTurbine` is implemented as a dialog composable (`ReportWindTurbineDialog`) triggered from the `MapScreen` pin-placement FAB.
- All repositories/DAO contracts are wired through generated SQLDelight APIs.
- Snapshot seed importer (`SnapshotSeedDataImporter`) runs on app startup with checksum-aware fast-path.
- Data wireframe: `UI -> ViewModel -> Repository -> SQLDelight DAOs -> SQLite`.
- `Favorites` supports both parks and regions; `Recents` records every opened park.
- `FaqScreen` renders static content from `FaqUiState.defaultFaqQuestions` (no ViewModel).
- AGP 9.x/KMP compatibility warning is accepted for the seminar MVP unless the build breaks.

## Build And Run
Android:

```shell
.\gradlew.bat :androidApp:assembleDebug
```

On Unix-like shells:

```shell
./gradlew :androidApp:assembleDebug
```

iOS: open `iosApp` in Xcode and run from there.

## Manual QA
Automated tests are not part of the stated seminar MVP delivery goal. Focus on acceptance criteria, manual QA, build verification and presentation-ready flows.

Recommended demo path:
- Start to Map.
- Search overlay.
- Park preview to Detail.
- Favorite add/remove.
- Recently viewed wind parks.
- FAQ.
- Stats.
- Info & Einstellungen.
- Denied/no-location path.

Android manual QA is required before the demo. An iOS simulator/device smoke test is desirable where available, but not a demo blocker if the shared KMP entry point remains intact.

## Android Screenshots For AI Review
Use the screenshot helper to capture full-device PNGs from the Android app. It can build, install, launch and navigate through the MVP top-level screens via `adb`.

```powershell
.\scripts\capture_android_screenshots.ps1 -Build -Install -CleanAppData
```

For stitched long screenshots of scrollable screens:

```powershell
.\scripts\capture_android_screenshots.ps1 -Build -Install -CleanAppData -FullPage -InitialWaitSeconds 25
```

By default, the run writes ignored analysis artifacts to `screenshots/android-ai/<timestamp>/`, including a small `manifest.txt`. Full-page runs also keep the intermediate scroll parts in `parts/` and scroll until the end is detected, up to `-FullPageScrolls 14` by default. If multiple devices are connected, pass `-Serial <adb-device-id>`.
