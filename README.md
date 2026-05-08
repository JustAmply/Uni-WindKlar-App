# Mein Windpark

`Mein Windpark` is a Kotlin Multiplatform app for Android and iOS that makes wind energy more transparent for users in Germany.

The app helps users discover nearby wind parks, search for specific parks, revisit recently opened parks, understand local production context for a wind park and its `Gemeinde`, and read practical answers to common questions and skeptical critiques about wind energy.

## Product Scope

The current MVP has three top-level pages plus a shared detail flow.

### Map

- Display wind parks on a map.
- Open a selected wind park detail page.
- Mark wind parks as favorites.

### Search

- Search for wind parks directly.
- Keep a local history of recently opened wind parks.
- Open a wind park detail page from search results or history.

### Park Detail / Production

- Show production-related data for a selected wind park.
- Connect the park view with relevant municipality (`Gemeinde`) context.
- Be reachable from both the Map and Search pages.

### FAQ

- Answer core wind-energy questions in clear, practical language.
- Address common skeptical critiques, for example concerns about birds and nature impact.

## Data Strategy

The app is local-first for now. Data is stored on the device with SQLite.

- Shared SQLDelight schema files live in `composeApp/src/commonMain/sqldelight`.
- UI code must not call SQLite or SQL directly.
- Data access should follow this boundary:

```text
UI -> ViewModel/UseCase -> Repository -> Local DB/DAO
```

There is no backend dependency, cross-device sync, or analytics requirement in the current baseline.

## Project Structure

This is a Kotlin Multiplatform project targeting Android and iOS.

- `composeApp`: shared Kotlin and Compose Multiplatform code.
- `iosApp`: native iOS launcher.

Shared app code lives under `composeApp/src/commonMain/kotlin/app`:

- `navigation/`: routes and shared app navigation host.
- `feature/map/`: map screen, state, and view model.
- `feature/search/`: search screen, state, history behavior, and view model.
- `feature/detail/`: wind park detail and production context flow.
- `feature/faq/`: FAQ screen, state, and content.
- `core/`: shared UI primitives, models, and utilities.
- `data/`: repository interfaces, local entities/DAO contracts, and seed import contracts.

Platform-specific code should stay thin:

- `commonMain`: shared UI screens, state, repository interfaces, and cross-platform logic.
- `androidMain`: Android-only integrations, such as permissions or Android-specific map SDK bindings.
- `iosMain`: iOS-only integrations.

## Development Conventions

- Use `FeatureScreen`, `FeatureViewModel`, and `FeatureUiState` naming.
- Keep one package per feature.
- Prefer shared code in `commonMain` unless a platform API requires otherwise.
- Keep navigation behavior explicit, including back behavior.
- Add or adjust repository contracts when data behavior changes.
- Add tests proportional to the risk and blast radius of a change.
- Replace temporary placeholders progressively with production UI and state.

## Current Baseline

- Navigation shell and feature placeholders exist.
- Data layer interfaces exist.
- SQLite implementation is still scaffold-level.
- SQL schema files are placeholders and need real table and query definitions.

## Build And Run

### Android

Run the Android app from the IDE run configuration, or build it from the terminal:

```shell
./gradlew :composeApp:assembleDebug
```

On Windows:

```shell
.\gradlew.bat :composeApp:assembleDebug
```

### iOS

Open `iosApp` in Xcode and run the iOS app from there. The shared UI and app logic are provided by the `composeApp` module.

## Definition Of Done

A vertical feature slice is considered done when:

- The UI flow works through the Android and iOS entry points.
- State and actions are wired through ViewModel and repository boundaries.
- Required local persistence is implemented, not only declared through interfaces.
- Navigation behavior is explicit.
- Non-obvious behavior is documented in focused comments or tests.
