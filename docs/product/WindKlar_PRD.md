# WindKlar PRD

Product Requirements Document fuer die WindKlar-App

Status: Arbeitsfassung, aktualisiert gegen den aktuellen Stand des `windklar`-Repositories  
Kontext: Modul "Digital Product Development and Lifecycle Management", Universitaet Leipzig, Sommersemester 2026  
Produktname: WindKlar  
Kurzpositionierung: Transparenz- und Beteiligungsplattform fuer Windenergie vor Ort

## 1. Problem Statement

Der Ausbau der Windenergie ist fuer Energiewende und Klimaschutz zentral, wird lokal aber haeufig durch unklare Informationen, fehlende Beteiligung und schwer einordenbare Auswirkungen begleitet. Viele Buergerinnen und Buerger sehen Windenergieanlagen im Alltag, koennen aber nicht einfach nachvollziehen, welche Anlage sie sehen, wie viel Strom sie erzeugt, welche lokalen Vorteile entstehen, welche Belastungen auftreten und welche Schutzmassnahmen gelten.

Bestehende Loesungen zeigen oft entweder technische Daten oder Karteninformationen. Was fehlt, ist eine verstaendliche mobile Anwendung, die technische Windparkdaten, lokale Auswirkungen, kommunalen Nutzen und Beteiligungsmoeglichkeiten in einem Produkt zusammenfuehrt.

WindKlar soll diese Transparenzluecke schliessen. Die App soll nicht einseitig fuer Windenergie werben, sondern nachvollziehbar erklaeren, was ein konkreter Windpark leistet, welche Zielkonflikte bestehen und wie lokale Akteure informiert oder beteiligt werden koennen.

## 2. Product Vision

WindKlar macht sichtbar, wo der Wind wirkt: Nutzerinnen und Nutzer erkennen Windenergieanlagen in ihrer Umgebung, verstehen deren Beitrag zur Energieversorgung und sehen, welcher Nutzen und welche Auswirkungen lokal entstehen.

Die App beantwortet vier Kernfragen:

1. Welcher Windpark befindet sich in meiner Naehe, und welche Anlagen gehoeren dazu?
2. Was leistet dieser Park technisch, energetisch und klimabezogen?
3. Welcher konkrete Nutzen entsteht fuer Kommune und Buergerschaft?
4. Welche Auswirkungen, Unsicherheiten und Beteiligungsmoeglichkeiten gibt es?

## 3. Goals

- Windenergieanlagen und Windparks in der Umgebung auffindbar und verstaendlich machen.
- Oeffentliche Windenergiedaten in eine buergernahe Darstellung uebersetzen.
- Vertrauen durch klare Quellen, Datenqualitaet und Zeitstempel foerdern.
- Akzeptanzrelevante Informationen sichtbar machen, insbesondere CO2-Einsparung, Haushaltsaequivalente und kommunale Beteiligung.
- Lokale Beteiligung durch Favoriten, Verlauf, Rueckmeldungen und perspektivisch Buergerbudget-Funktionen vorbereiten.
- Einen MVP liefern, der im Rahmen eines studentischen Projekts realistisch umsetzbar, vorfuehrbar und evaluierbar ist.

## 4. Non-Goals

- WindKlar ersetzt kein formelles Genehmigungs- oder Beteiligungsverfahren.
- WindKlar trifft keine rechtsverbindlichen Aussagen zu Schall, Schattenwurf, Artenschutz oder kommunalen Einnahmen.
- WindKlar ist im MVP keine Social-Media-Plattform.
- WindKlar ist im MVP keine vollstaendig automatisierte Echtzeit-Leitwarte fuer Windparkbetrieb.
- WindKlar soll keine politischen Bewertungen erzwingen; die App soll transparent informieren und Zielkonflikte sichtbar machen.

## 5. Target Audience and Stakeholders

### Primary Users

- Interessierte Buergerinnen und Buerger, die Windparks in ihrer Umgebung verstehen wollen.
- Anwohnerinnen und Anwohner, die konkrete Informationen zu sichtbaren Anlagen suchen.
- Gemeindemitglieder, die den lokalen Nutzen und moegliche Belastungen einordnen moechten.

### Secondary Users

- Kommunen und oeffentliche Verwaltungen, die transparente Projektkommunikation unterstuetzen wollen.
- Windparkbetreiber, die verstaendliche Informationen bereitstellen moechten.
- Energie- und Klimaschutzagenturen, die Windenergie erklaeren und Akzeptanzarbeit leisten.
- Lokale Unternehmen und Vereine, die von kommunalen Windparkertraegen oder Projekten betroffen sein koennen.
- Hochschulteam und Lehrende, die Konzept, Prototyp, Umsetzung und Evaluation bewerten.

## 6. Core Personas

### Persona 1: Die interessierte Buergerin

Sie sieht Windraeder in ihrer Umgebung und moechte wissen, welche Anlage das ist, wie weit sie entfernt ist und ob sie tatsaechlich einen relevanten Beitrag leistet. Sie braucht eine klare Karte, einfache Kennzahlen und kurze Erklaertexte ohne Fachjargon.

### Persona 2: Der skeptische Anwohner

Er sorgt sich um Belastungen und moechte verstehen, welche Auswirkungen realistisch sind. Er braucht transparente Datenqualitaet, Hinweise auf Unsicherheiten, sachliche Informationen zu Schutzmassnahmen und eine Moeglichkeit, Fragen oder fehlende Daten zu melden.

### Persona 3: Die kommunale Entscheiderin

Sie moechte zeigen, welcher Nutzen aus Windenergie fuer die Kommune entsteht. Sie braucht eine Darstellung von kommunaler Beteiligung, Foerderprojekten, Ausgleichsmassnahmen und haeufigen Fragen.

### Persona 4: Der Projektentwickler oder Betreiber

Er moechte Stammdaten und Projektinformationen buergernah darstellen. Er braucht robuste Datenstrukturen, klare Quellenangaben und eine Darstellung, die Vorteile nicht ueberzeichnet.

## 7. Competitive Landscape

Die Konzeptpraesentation nennt bestehende Angebote wie WindPower, WindTurbineMap, ENMAP, EnBW E-Cockpit und Juvent. Diese Loesungen decken Teile des Problems ab, etwa Karten, technische Daten oder Betreiberinformationen. Die Differenzierung von WindKlar liegt in der Verbindung von:

- Karte und Geolocation,
- technischen Turbinendaten,
- jaehrlicher Produktion und CO2-Wirkung,
- lokaler kommunaler Beteiligung,
- buergernaher Sprache,
- Datenqualitaetskennzeichnung,
- Beteiligungs- und Rueckmeldeelementen.

## 8. MVP Scope

Der MVP konzentriert sich auf Transparenz mit hohem Nutzen fuer Buergerinnen und Buerger bei mittlerer technischer Komplexitaet.

### Must Have

1. Karte mit Windparks als primaerer UX-Ebene und Windanlagen als atomarer Datenbasis
2. Detailansicht fuer einen Windpark
3. Citizen Impact Dashboard
4. Kennzahlen zu Jahresproduktion, CO2-Einsparung und versorgten Haushalten
5. Darstellung kommunaler Beteiligung oder lokaler Nutzenindikatoren
6. Favoriten und zuletzt angesehene Windparks
7. Datenquellen, Datenqualitaet und Zeitstempel je zentraler Kennzahl
8. Basis-FAQ zu Windenergie, Datenherkunft und App-Grenzen

### Should Have

1. Filter und Suche nach Standort, Anlage, Kommune oder Windpark
2. Meldung fehlender oder fehlerhafter Anlagen
3. Vergleich von zwei Anlagen oder Windparks
4. Offline-nahe lokale Zwischenspeicherung zentraler Daten
5. Einfache Projekt- oder Windparkuebersicht fuer Kommunen

### Could Have

1. Next-day forecast auf Basis von Wetterdaten und Leistungskurven
2. Schattenwurf-, Abstand- oder Naturschutz-Layer als Demo-Daten
3. Buergerbudget-Demo fuer lokale Projektpriorisierung
4. Fragen-Tracker fuer haeufige Einwaende und Antworten
5. Szenario-Simulator fuer Variantenvergleich

### Won't Have in MVP

1. Kamera- oder AR-basierte Anlagenidentifikation
2. Rechtsverbindliche Abstimmungen
3. Moderierte Kommentarbereiche oder Social Feed
4. Vollautomatisierter Import aller Datenquellen in Produktionsqualitaet
5. KI-Assistent mit Dokumenten-RAG

## 9. User Stories

1. As an interested citizen, I want to see wind parks near my current location, so that I can understand what I am seeing in my environment.
2. As an interested citizen, I want to open a wind park from a map, so that I can inspect its key information quickly.
3. As a resident, I want to see the distance between my location and a wind park, so that I can better assess local relevance.
4. As a resident, I want wind park and turbine data in plain language, so that I do not need technical expertise to understand the information.
5. As a resident, I want to see whether data is official, measured, derived, estimated, simulated or missing, so that I can judge reliability.
6. As a citizen, I want to see annual energy production, so that I can understand the contribution of a turbine or wind park.
7. As a citizen, I want to see CO2 savings, so that the climate effect becomes more tangible.
8. As a citizen, I want to see an estimate of households supplied, so that abstract energy values become easier to understand.
9. As a citizen, I want to see municipal participation or local benefit, so that the local value of wind energy is visible.
10. As a citizen, I want to see which sources were used, so that I can trust and verify the information.
11. As a citizen, I want to save wind parks as favorites, so that I can revisit relevant locations quickly.
12. As a citizen, I want to see recently viewed wind parks, so that I can continue earlier exploration.
13. As a citizen, I want to search by municipality or location, so that I can inspect places that matter to me.
14. As a citizen, I want to filter map results, so that dense areas remain understandable.
15. As a citizen, I want to submit a structured data hint for missing or incorrect wind turbine data, so that data quality issues can become visible and reviewable.
16. As a citizen, I want a FAQ about wind energy, so that common questions are answered without searching elsewhere.
17. As a skeptical resident, I want transparent explanation of limitations, so that the app does not feel like advertising.
18. As a skeptical resident, I want potential impacts to be acknowledged, so that benefits and burdens are presented fairly.
19. As a municipality representative, I want to present local benefit data, so that citizens understand what reaches the community.
20. As a municipality representative, I want to show funded local projects, so that wind energy revenue becomes concrete.
21. As a wind park operator, I want turbine master data to be displayed consistently, so that users receive clear information.
22. As an energy agency, I want to use WindKlar as an education tool, so that citizens understand wind energy in context.
23. As a product owner, I want features prioritized by citizen value and implementation complexity, so that the MVP remains feasible.
24. As a developer, I want a structured data model for turbines, wind parks and metrics, so that features can be implemented incrementally.
25. As a tester, I want clear acceptance criteria for each feature, so that quality can be evaluated against external behavior.
26. As a lecturer, I want the product to reflect consulting, analysis, concept, design, implementation and QA phases, so that it matches the module objectives.

## 10. Functional Requirements

### Map and Geolocation

- The app shall show wind parks on an interactive map; individual turbines may be added where the data model supports them.
- The map shall use progressive disclosure: the default map level shows wind parks or clusters, while individual wind turbines become visible at higher zoom levels or inside a wind park detail context.
- The map shall support a Germany-wide dataset by clustering, filtering or otherwise reducing marker density at low zoom levels.
- The MVP map implementation should stay shared or data-driven and shall not require separate Android and iOS native map implementations.
- The app shall request user location only when needed and explain why location is useful.
- Location access shall be requested only after a user action such as "center on my location" or "near me".
- The app shall not store the user's location in the MVP.
- The app shall allow selecting a map marker to open a wind park detail view.
- The app should allow selecting an individual turbine at detailed zoom levels, while keeping wind park and municipality context visible.
- The app shall support manual location search as an alternative to GPS.
- Data hints shall allow manually placing a pin instead of using GPS.
- Search shall be part of the map flow as an overlay or sheet, not a bottom-navigation destination.
- Search shall support wind park name, municipality/place, and optionally MaStR id or installation name.
- Selecting a search result shall close the overlay, focus the map and open the selected wind park preview or detail.
- The app shall handle unavailable or denied location permission gracefully.

### Wind Park Detail View

- The app shall show wind park name, municipality, location, turbine count, installed capacity and status if available.
- The app should show individual turbine details such as identifier, manufacturer/model and nominal power where the dataset supports them.
- The app shall keep wind park context visible when showing individual turbine details, so users understand which local project and municipality the turbine belongs to.
- The app shall show production-related metrics where available or estimated.
- The app shall clearly mark missing values instead of hiding them.
- The app shall provide source and timestamp information for relevant fields.

### Citizen Impact Dashboard

- The app shall summarize key impact metrics: annual production, estimated CO2 savings, household equivalent and municipal benefit indicator.
- The app shall use understandable units and contextual labels.
- The app shall avoid overstating uncertain calculations.
- The app shall explain how estimated metrics are calculated at a high level.
- MVP annual production shall be estimated from installed capacity and documented assumed full-load hours.
- MVP CO2 savings shall be estimated from estimated production and a documented emission factor.
- MVP household equivalents shall be estimated from estimated production and a documented average household electricity consumption value.
- Calculation assumptions shall live in configuration or snapshot metadata, not as hard-coded UI constants.
- Concrete values for full-load hours, emission factor and household electricity consumption remain open until a source-backed snapshot is prepared.
- Snapshot metadata shall include assumption value, unit, source, source date or retrieval date, and calculation note for each impact assumption.

### Municipal Benefit

- The app shall show whether a municipality has documented participation, expected benefit or demo benefit data.
- The app shall distinguish real public data from demo or simulated data.
- The app should support a simple breakdown of local uses, such as school, public transport, sports, nature compensation or community infrastructure.
- For MVP, municipal participation should be shown as an estimated expected §6 EEG value, not as a confirmed payout unless a payment source exists.
- Recommended short copy: "Geschätzte kommunale Beteiligung: ca. X EUR/Jahr. Grundlage: §6 EEG, 0,2 ct/kWh, geschätzte Jahresproduktion. Keine bestätigte Auszahlung."

### Favorites and History

- The app shall allow users to save wind parks locally as favorites.
- The MVP shall not support separate favorites for individual wind turbines.
- The app shall show recently viewed wind parks, regardless of whether they were opened from map, search or favorites.
- The app shall not require a user account for MVP favorites.
- Favorites and recently viewed wind parks shall be persisted through SQLDelight in the MVP.

### Data Quality and Sources

- Each key metric shall expose a data quality label: official, measured, derived, estimated, simulated or missing.
- Each key metric shall expose a source label and timestamp where possible.
- The UI shall make data quality visible without overwhelming the main screen.
- The MVP shall use real MaStR/Open-MaStR-backed master data for wind installations where available.
- The MVP shall load MaStR/Open-MaStR master data from a preprocessed local JSON snapshot rather than calling a live external API at app runtime.
- The JSON snapshot shall contain both individual wind installations and precomputed wind park aggregates.
- MaStR/Open-MaStR-backed installation master data shall be labelled as `official`.
- Wind park aggregates computed during preprocessing shall be labelled as `derived`.
- Production and acceptance impact values shall be labelled as `estimated` or `simulated` unless measured public values are available.
- The MVP may use estimated or simulated impact values for yearly production, CO2 savings, household equivalents and municipal participation when measured public values are unavailable.
- Estimated or simulated impact values shall be visibly labelled and must not be presented as official measurements.

### FAQ and Education

- The app shall provide short explanations for common topics: wind energy basics, energy production, CO2 savings, municipal participation, data sources and limitations.
- FAQ content shall use neutral, accessible language.

### Data Hints and Missing Data

- The app should allow users to submit structured data hints for missing or incorrect wind turbines from the map flow.
- Data hints shall support the MVP categories `missing_installation`, `wrong_location`, `wrong_status`, `wrong_wind_park_assignment`, `wrong_technical_data`, `installation_removed`, and `other`.
- Data hints should be linked to a MaStR id, wind turbine id, wind park id, municipality id or map coordinate where possible.
- A data hint shall require category, location or existing object reference, short description, and confidence (`unsure`, `likely`, `certain`).
- A data hint may include an optional image or suggested corrected value.
- Contact information is not required for MVP data hints and should be avoided unless explicitly introduced later.
- Data hints shall be stored locally in SQLDelight in the MVP.
- Data hints should carry a local status such as `draft`, `ready_for_review` or `exported`.
- Data hints should be exportable in a simple reviewable format in a later slice, but the MVP shall not require a backend.
- The MVP shall not claim to submit official corrections to MaStR or another public register.
- The MVP shall avoid collecting unnecessary personal data.

## 11. Non-Functional Requirements

### Usability

- The app shall follow mobile-first navigation with predictable map, detail and dashboard flows.
- Important status and error states shall be visible.
- Technical terms shall be explained or replaced with everyday language.
- Interface elements shall be consistent and recognizable.

### Accessibility

- The app shall use sufficient contrast, scalable text and accessible labels.
- Map-based information shall have non-map alternatives where relevant.
- Critical information shall not be communicated by color alone.

### Performance

- The map shall remain responsive for a Germany-wide MVP dataset.
- Detail views should open quickly after selecting a marker.
- Cached data should be used where appropriate to reduce repeated loading.

### Privacy

- Location access shall be optional.
- Location access shall be user-initiated and not requested during onboarding.
- User location shall not be stored in the MVP.
- MVP favorites and history should be stored locally by default.
- No personal account shall be required for MVP use.
- If data hints are exported or transmitted, personal data shall be minimized.

### Reliability

- The app shall handle external API downtime by showing cached or fallback data.
- The app shall show clear empty, loading and error states.
- The app shall distinguish unavailable data from zero values.

### Transparency

- The app shall make uncertainty explicit.
- Simulated or demo values shall be labelled as such.
- The product tone shall be factual, not promotional.

## 12. Data Requirements

### Core Entities

- Wind turbine / Windanlage: id, wind park id, name or identifier, coordinates, manufacturer, model, type, hub height or total height if available, installed capacity, status, operator, source metadata. This is the atomic source-data and coordinate unit.
- Wind park: id, name, municipality, representative coordinates or geometry, turbine count, aggregated installed capacity, aggregated metrics, source metadata. This is the primary citizen-facing UX unit for map overview, favorites and municipality context.
- Metric: value, unit, period, data quality, source, timestamp, calculation note.
- Municipality benefit: municipality, benefit type, amount or qualitative indicator, period, source, data quality.
- User local state: favorites, recently viewed wind parks, optional dismissed onboarding hints.
- Data hint: hint category, subject type, subject id if available, municipality id if available, coordinates, description, confidence, optional local image reference, optional suggested value, created timestamp, local review/export status.
- Snapshot metadata: snapshot source, source version or timestamp, import timestamp, calculation assumptions and preprocessing notes.

### Current SQLDelight Schema in Repository

The repository already contains concrete SQLDelight schema files under `composeApp/src/commonMain/sqldelight/app/data/local/db`:

- `WindPark.sq`: `wind_park` table with aggregate fields, source metadata, grouping method and data quality; queries for select-all, select-by-id, text search and upsert.
- `WindTurbine.sq`: `wind_turbine` table for atomic MaStR/Open-MaStR-backed installation master data.
- `Metric.sq`: generic `metric` table for production and acceptance impact values with unit, period, source, data quality and calculation note.
- `Favorite.sq`: `favorite_wind_park` table keyed by `wind_park_id`; queries for favorite ids, favorite existence, add and remove.
- `RecentWindPark.sq`: `recent_wind_park` table keyed by `wind_park_id`; queries for recently opened wind parks, record and clear.
- `DataHint.sq`: local `data_hint` table for structured Datenhinweise.
- `SnapshotMetadata.sq`: imported snapshot identity, source, checksum and preprocessing metadata.

Current gap: the Kotlin domain model and DAO interfaces are still thinner than the SQL schema. `WindPark` currently exposes only `id`, `name`, `municipality` and `isFavorite`; `WindParkEntity` currently exposes only `id`, `name` and `municipality`. The repository and DAO contracts need to be expanded or mapped explicitly before the SQL schema can drive the UI.

Implemented local schema target:

- `wind_turbine`: atomic MaStR/Open-MaStR installation master data with source and quality metadata.
- `wind_park`: precomputed wind park aggregates for map, favorites, search and detail overview.
- `metric`: production and acceptance impact values such as annual production, CO2 savings, household equivalents and municipal participation.
- `favorite_wind_park`: saved wind parks.
- `recent_wind_park`: recently opened wind parks.
- `data_hint`: local Datenhinweise with category, subject reference, location, description, confidence and local status.
- `snapshot_metadata`: optional metadata for source, import timestamp, version and calculation assumptions.

The source-data pipeline now lives outside the app under `data/`, and the app imports only the app-ready JSON snapshot bundled under Compose resources. Raw and intermediate MaStR files are intentionally ignored.

Updated modeling decision: the SQL schema includes `wind_turbine` as the atomic MaStR/Open-MaStR-backed unit. Wind park rows are aggregates or curated groupings over those turbine rows.

Updated local-state decision: the product concept is "Zuletzt angesehen", not a search-only history. `RecentWindPark.sq` records opened wind parks regardless of entry path.

Updated local persistence decision: favorites and recently viewed wind parks are SQLDelight-backed MVP behavior, not mock-only state.

Updated data-source decision: the MVP uses real MaStR/Open-MaStR-backed master data for wind installations where available. Installation master data is labelled `official`; preprocessing aggregates are labelled `derived`; production, CO2, household equivalents and municipal participation may be `estimated` or `simulated` if measured public data is unavailable.

Updated municipal participation decision: MVP municipal participation is shown as a short estimated expected §6 EEG value, based on 0.2 ct/kWh and estimated yearly production, with a clear "no confirmed payout" note unless a payment source exists.

Updated impact calculation decision: annual production, CO2 savings and household equivalents are estimated from simple documented assumptions. Concrete values for full-load hours, emission factor and household consumption remain open until snapshot preparation, but each assumption must be stored in snapshot metadata with value, unit, source, source date or retrieval date, and calculation note.

Updated local schema decision: SQLDelight uses `wind_turbine`, `wind_park`, `metric`, `favorite_wind_park`, `recent_wind_park`, `data_hint` and `snapshot_metadata`.

Updated scope decision: the MVP dataset should cover Germany rather than a Leipzig/Saxony-only demo region. This increases map-density and import requirements, so clustering/filtering and local cache design are part of the MVP data strategy.

Updated integration decision: the MVP uses a preprocessed local MaStR/Open-MaStR JSON snapshot instead of runtime live API access. The app should import or bundle this snapshot into local SQLDelight-backed storage.

Updated aggregation decision: wind park grouping and aggregate fields should be computed during snapshot preprocessing. The app imports both wind park aggregates and individual wind installations instead of computing Germany-wide groupings at runtime.

Updated metadata decision: source and data-quality metadata should be stored directly on source-backed master-data tables for wind installations and wind park aggregates. User-facing impact values should be stored in a separate metric model with its own unit, period, source, quality label and calculation note.

Updated participation decision: the report flow is a `Datenhinweis` workflow. It collects structured hints about missing or incorrect wind installations or master data, but does not promise an official MaStR correction.

Updated data-hint persistence decision: Datenhinweise are stored locally in SQLDelight for the MVP, with a local review/export status. No backend submission or account-based workflow is required.

Updated data-hint category decision: MVP data hints use exactly these categories: `missing_installation`, `wrong_location`, `wrong_status`, `wrong_wind_park_assignment`, `wrong_technical_data`, `installation_removed`, and `other`.

Updated data-hint input decision: MVP data hints require category, location or existing object reference, short description, and confidence. Image and suggested corrected value are optional; contact information is not required.

Updated search decision: search belongs to the map flow as an overlay or sheet. It is not a bottom-navigation destination.

Updated map implementation decision: the MVP should use a shared or data-driven map approach, preferably OSM-compatible if feasible, and should not require separate native Android and iOS map implementations.

Updated location decision: location access is optional, requested only after user action, and not stored in the MVP. Manual search and manual pin placement remain alternatives.

Updated navigation decision: top-level screens use bottom navigation only and should not show a back button to `Map`. Back behavior is reserved for subflows.

Updated profile decision: Profile is an `Info & Einstellungen` area in the MVP. It should not include logout, account language, or controls for unimplemented behavior such as notifications or dark mode.

Updated design decision: the green Nature/Trust visual direction is accepted for MVP, but colors, typography, spacing, radii and elevation should be centralized as Compose theme/design tokens.

Updated Figma decision: Figma is a functional and visual reference for screen set, information architecture, component intent, rough layout and copy. It is not a pixel-perfect implementation contract for the MVP.

Updated build-tooling decision: the AGP 9.x / Kotlin Multiplatform compatibility warning is accepted as a documented seminar-MVP risk. Do not migrate to a separate Android app module before the demo unless the build breaks.

Updated QA decision: Android manual QA is required before the demo. iOS simulator/device smoke testing is desirable where available but not a demo blocker if the shared KMP entry point remains intact.

### Candidate Sources

- Marktstammdatenregister for turbine master data.
- Open-MaStR for accessible turbine master data exports or integration.
- OpenStreetMap for basemap and geographic context.
- DWD weather data for optional forecast or production estimation.
- SMARD or public energy statistics for contextual energy data.
- Umweltbundesamt, BWE or Fachagentur Wind und Solar for explanatory context and acceptance-related information.
- Demo datasets for municipal benefit, shadow, sound, nature protection or scenario data until verified public data is available.

### Data Quality Labels

- official: published by an official or authoritative public source.
- measured: measured operational or sensor data.
- derived: mechanically computed from official or source-backed fields without additional modelling assumptions.
- estimated: calculated from model, weather, power curve or public assumptions.
- simulated: demo or scenario value.
- missing: intentionally shown as unavailable.

## 13. Information Architecture

Current repository navigation:

1. `Start`: full-screen entry with CTA, no bottom navigation.
2. `Map`: primary discovery surface with integrated search field, filter chips, map actions and preview sheet.
3. `Stats`: production and energy context screen with Compose/Canvas charts.
4. `Favorites`: saved wind parks list.
5. `Faq`: accordion-based education and skepticism-answering screen.
6. `Profile`: local `Info & Einstellungen` screen without account or logout behavior.
7. `Detail(parkId)`: wind park detail route reachable from map and favorites.

Current bottom navigation:

1. `Map`
2. `Stats`
3. `Favorites`
4. `Faq`
5. `Profile` / `Info & Einstellungen`

Current flow notes:

- `Start` is the entry route and navigates to `Map`.
- `Search` exists as a feature package and placeholder screen, but the confirmed product behavior is a map overlay/sheet rather than a top-level route or bottom-nav item.
- `ReportWindTurbine` is now defined as a Datenhinweis flow and represented by a map action icon, but no route, package or form implementation exists yet.
- The confirmed map behavior is progressive disclosure: wind parks or clusters first, individual wind turbines only at higher zoom levels or in detail context.
- The confirmed favorites behavior is wind park-only for MVP; individual turbines are not separately favorited.
- Top-level feature screens should not include their own back affordance to `Map`; bottom navigation owns top-level movement.
- Back affordances belong only to subflows such as detail, search overlay/sheet, data hint form and individual turbine subdetail.
- `Profile` should be treated as `Info & Einstellungen`, not as an authenticated account area.

Possible future navigation:

1. Wind park detail with production context
2. Turbine detail where individual turbine data is available
3. Municipal benefit
4. Participation
5. Scenarios
6. Documents
7. Data hint submitted / export confirmation

## 14. UX and Design Requirements

The design should follow the lecture focus on UX, UI, usability heuristics, Gestalt principles, prototyping and user acceptance tests.

The accepted MVP visual direction is green, nature-oriented and trust-focused. Colors, typography, spacing, radii and elevation should be centralized as Compose theme/design tokens instead of repeated as screen-local constants.

Figma is the source of truth for screen set, information architecture, component intent, rough layout and available copy. Compose implementation should preserve the user-facing structure and intent, but pixel-perfect parity is not required for the seminar MVP.

- Visibility of system status: loading, GPS, stale data and missing data states must be clear.
- Match with the real world: wording should use familiar terms such as "Strom fuer Haushalte" instead of only technical units.
- User control and freedom: users can browse without location permission and clear local history if implemented.
- Consistency: map markers, data cards, source labels and status chips should behave consistently.
- Error prevention: permission and data hint flows should prevent accidental submissions.
- Recognition over recall: key metrics, saved items and recently viewed entries should be easy to find.
- Minimalist design: the dashboard should prioritize a few strong metrics instead of overwhelming users.
- Help and documentation: FAQ and source explanations should be reachable from data-heavy screens.

## 15. Implementation Decisions

- Build the MVP as a Kotlin Multiplatform mobile app for Android and iOS.
- Implement shared UI and state in Compose Multiplatform under `composeApp/src/commonMain`.
- Keep Android and iOS entry points thin: Android launches `App()` from `MainActivity`; iOS launches `App()` through `ComposeUIViewController`.
- Use SQLDelight as the local-first persistence layer.
- Use a clear domain model around turbines, wind parks, municipalities, metrics, sources and local user state.
- Treat source metadata and data quality as first-class data, not as UI afterthoughts.
- Use a hybrid metadata schema: source fields on master-data tables and a separate metric model for production and acceptance impact values.
- Use real MaStR/Open-MaStR-backed master data for the MVP baseline where available.
- Load the public-source master data through a preprocessed local JSON snapshot for MVP reliability.
- Keep wind park aggregation in preprocessing, not runtime app logic.
- Keep map behavior shared or data-driven for MVP; avoid separate Android/iOS native map stacks unless later required.
- Treat user location as optional, user-initiated context; do not store it in MVP.
- Use local demo or derived data only for impact values that cannot be sourced as measured public data within the seminar scope, and label them as estimated or simulated.
- Store Datenhinweise locally in SQLDelight for MVP; do not require a backend.
- Keep municipal benefit separate from turbine technical data because availability, source and confidence differ.
- Keep favorites and history local in the MVP to avoid authentication and privacy overhead.
- Defer camera-based identification and AR overlay because the value is interesting but complexity is XL.
- Defer social features because moderation risk is high compared to MVP value.
- Prepare the product backlog in small vertical slices: map browsing, detail view, impact metrics, saved items, data quality, FAQ, feedback.

### Current Repository Baseline

- Project type: Kotlin Multiplatform with Compose Multiplatform and Android application packaging.
- Targets: Android, `iosArm64` and `iosSimulatorArm64`.
- Main module: `composeApp`; native iOS launcher: `iosApp`.
- Package namespace/application id: `product.lifecycle.windenergy`.
- Current libraries: Kotlin 2.3.21, Compose Multiplatform 1.10.3, Material 3 1.10.0-alpha05, SQLDelight 2.3.2, Android compile/target SDK 36 and min SDK 24.
- Shared app root: `app.App`, wrapping `AppNavHost` in `WindklarTheme`.
- Current route model: `Start`, `Map`, `Stats`, `Favorites`, `Faq`, `Profile`, `Detail(parkId)`.
- Current top-level routes in bottom navigation: `Map`, `Stats`, `Favorites`, `Faq`, `Profile` / `Info & Einstellungen`.
- Implemented visual slices: `StartScreen`, `MapScreen`, `FavoritesScreen`, `FaqScreen`, `StatsScreen` and `ProfileScreen`.
- Placeholder or scaffold slices: `SearchScreen`, `ParkDetailScreen`, `MapViewModel`, `SearchViewModel`, `ParkDetailViewModel`, database driver factory and seed importer.
- Missing slice: `ReportWindTurbine` route/package/form is not implemented yet.
- Current data state: UI is mostly backed by mock `UiState` defaults; repository and DAO contracts exist but are not yet wired through generated SQLDelight database APIs.
- Current assets: start background/icon and favorite wind park thumbnails are bundled under `composeResources/drawable`.
- Current build risk: the Gradle problems report flags a Kotlin Multiplatform/Android Gradle Plugin compatibility warning for using `org.jetbrains.kotlin.multiplatform` with `com.android.application` on AGP 9.x. This is accepted for the seminar MVP; a future migration may need a separate Android application subproject if the app continues beyond the seminar or the build breaks.

## 16. Testing Decisions

For the current seminar project, new automated tests are not part of the stated delivery goal. Quality work should therefore focus on acceptance criteria, manual QA, build verification and presentation-ready flows. If the project later moves beyond the seminar MVP, automated tests should verify external behavior rather than implementation details.

### Manual QA Checkpoints

- Data transformation checkpoint: raw source/demo wind park data becomes app-ready wind park and metric objects.
- Map interaction checkpoint: selecting a map preview opens the correct `Detail(parkId)` route.
- Detail view checkpoint: missing, derived, estimated and official values render with correct labels once source metadata is implemented.
- Impact calculation checkpoint: household and CO2 equivalent calculations are deterministic and labelled as estimates.
- Local state checkpoint: favorites and recently viewed wind parks persist locally and can be changed by the user once SQLDelight wiring is complete.
- Permission checkpoint: location denied, unavailable or granted states lead to understandable UI states once location support is implemented.
- Error checkpoint: failed source/API loading shows fallback or error states without crashing if external data is introduced.

### Acceptance Checks

- A user can open the app, move from `Start` to `Map`, inspect a demo wind park preview and open its detail page.
- A user can understand at least annual production, CO2 savings and household equivalent from the dashboard.
- A user can identify whether a displayed metric is official, measured, derived, estimated, simulated or missing.
- A user can save and revisit a wind park without creating an account once persistence is wired.
- A user can use the app without granting location permission.
- A user can read sources and limitations for key data.

### Manual QA

- Test on at least one Android emulator or Android device before the demo.
- Run an iOS simulator/device smoke test where available; iOS smoke test is desirable but not a demo blocker if the shared KMP entry point remains intact.
- Check map marker density and detail navigation.
- Check empty, loading, error and missing-data states.
- Check wording with non-technical users where possible.
- Manual demo path checklist: Start to Map, Search overlay, Park preview to Detail, favorite add/remove, recently viewed, local data hint save, FAQ, Stats, Info & Einstellungen, and denied/no-location path.

## 17. Analytics and Evaluation

Evaluation should focus on whether WindKlar improves understanding and perceived transparency.

Potential product metrics:

- Map-to-detail conversion rate.
- Share of users opening source or data quality explanations.
- Number of favorites created.
- Number of FAQ entries opened.
- Number of submitted data hints.
- Completion rate for a test task such as "find a wind park near Leipzig and explain its contribution."

Potential study questions:

- I understand better what a local wind park contributes.
- I can distinguish measured data from estimates.
- I can name at least one local benefit or limitation.
- The app feels factual rather than promotional.
- I would use this app before or during local wind energy discussions.

## 18. Risk Register

| Risk | Probability | Impact | Mitigation |
|---|---:|---:|---|
| Incomplete or outdated wind farm data | High | High | Show data quality, timestamps, sources and missing states clearly. |
| Privacy or GDPR issue | Medium | Very High | Make location optional, avoid accounts in MVP, minimize feedback data. |
| Low user adoption | Medium | High | Focus MVP on immediate map value and understandable impact metrics. |
| External API downtime | Medium | Medium | Use demo data, caching and fallback states. |
| Platform build or distribution friction | Low/Medium | Medium | Validate Android and iOS build paths early and keep platform-specific code thin. |
| Social feature misuse | High | Low/Medium | Defer comments/social feed beyond MVP. |
| Map performance issues | Medium | Low/Medium | Limit MVP dataset, cluster markers if needed. |
| Security vulnerabilities | Low | Very High | Keep backend minimal, validate input, avoid unnecessary personal data. |
| Scope creep | High | Medium/High | Keep MVP strict and move AR, AI and advanced simulations to later phases. |
| GPS inaccuracy | Low/Medium | Low | Provide manual search and avoid exact claims from user position. |

## 19. Roadmap

### Phase 1: Foundation

- Align Kotlin domain models and SQLDelight schema with the target local model.
- Wire generated SQLDelight database APIs through DAO/repository contracts.
- Implement or adapt seed import for a Germany-wide preprocessed MaStR/Open-MaStR-derived JSON wind installation snapshot.
- Keep app shell and bottom navigation owned by `AppNavHost`.
- Replace placeholder detail view with repository-backed wind park detail.
- Add source and data quality fields to schema or a companion metric/source model.
- Add a metric table for yearly production, CO2 savings, household equivalents and municipal participation values.
- Add `wind_turbine`, `recent_wind_park`, `data_hint` and optional `snapshot_metadata` schema support.

### Phase 2: MVP Transparency

- Connect `Map`, `Favorites`, `Search` and `Stats` screens to local data.
- Add citizen impact dashboard for selected wind parks and municipalities.
- Add annual production, CO2 savings and household equivalent.
- Add estimated expected §6 EEG municipal participation with short "no confirmed payout" copy.
- Store impact calculation assumptions in configuration or snapshot metadata, including value, unit, source, source date or retrieval date, and calculation note.
- Add favorites and recently viewed wind parks.
- Persist favorites and recently viewed wind parks through SQLDelight.
- Keep FAQ and limitations content accessible from data-heavy flows.

### Phase 3: Validation and Polish

- Execute manual acceptance checks and document any known demo limitations.
- Improve empty, error and permission states.
- Refine copywriting and accessibility.
- Prepare presentation-ready demo data.

### Phase 4: Extension

- Add structured data hints through `ReportWindTurbine` if confirmed for MVP.
- Add comparison view.
- Add forecast or scenario functionality.
- Add richer municipal benefit and participation modules.

### Phase 5: Optional Advanced Features

- Camera-based identification or AR overlay.
- KI assistant based only on project documents and verified data.
- Buergerbudget and questions tracker.
- Real-time or automated public data import.

## 20. Definition of Done for MVP

- Core start, map, preview and wind park detail flow works end to end.
- A Germany-wide public-source-backed JSON wind installation snapshot is available locally or through an import/cache path.
- Central metrics show value, unit, period, source and data quality.
- Favorites and recently viewed wind parks work without user account.
- Location permission is optional and handled gracefully.
- FAQ and limitation text are available.
- Main user stories have manual acceptance checks.
- Manual QA has been performed on Android; iOS smoke test has been performed where available.
- The product can be demonstrated as a coherent WindKlar prototype.

## 21. Open Questions for the App Repository

- Decided: Windanlagen are the atomic source-data and coordinate unit; Windparks are the primary citizen-facing UX unit for map overview, favorites and municipality context.
- Decided: The map uses progressive disclosure. Users first see wind parks or clusters; individual wind turbines appear only at higher zoom levels or in a wind park detail context.
- Decided: Favorites are wind park-only in the MVP. Individual wind turbines can be inspected but not separately saved.
- Decided: The MVP uses real MaStR/Open-MaStR master data for wind installations where available, while production and acceptance impact values may be estimated or simulated with explicit data quality labels.
- Decided: MaStR/Open-MaStR installation master data is `official`; preprocessing-generated wind park aggregates are `derived`; production and acceptance impact values are `estimated` or `simulated` unless measured public values are available.
- Decided: The MVP dataset should cover Germany, not only Leipzig/Saxony or another local demo region.
- Decided: The MVP uses a preprocessed local MaStR/Open-MaStR JSON snapshot instead of live API access inside the app, then imports that snapshot into SQLDelight.
- Decided: `ReportWindTurbine` is a Datenhinweis flow for structured, local/exportable data-quality hints. It must not promise official MaStR correction.
- Decided: Datenhinweise are stored locally in SQLDelight in the MVP and may be exported later; no backend or account flow is required.
- Decided: Datenhinweise use the categories `missing_installation`, `wrong_location`, `wrong_status`, `wrong_wind_park_assignment`, `wrong_technical_data`, `installation_removed`, and `other`.
- Decided: Datenhinweise require category, location or existing object reference, description and confidence; image and suggested corrected value are optional, contact information is not part of the MVP.
- Decided: Search remains part of the map flow as an overlay or sheet, not a bottom-nav route.
- Decided: The MVP map should be shared/OSM-compatible or data-driven and must not require separate Android and iOS native map implementations.
- Decided: The app-facing snapshot format is JSON. Source preprocessing may use CSV or other raw formats, but the app imports JSON into SQLDelight.
- Decided: The JSON snapshot contains both individual Windanlagen and precomputed Windpark aggregates; the app does not perform Germany-wide wind park grouping at runtime.
- Decided: Use a hybrid source/quality schema. Master-data tables carry simple source fields; production and acceptance impact values live in a dedicated metric model.
- Decided: Target SQLDelight/domain model is `wind_turbine`, `wind_park`, `metric`, `favorite_wind_park`, `recent_wind_park`, `data_hint` and optional `snapshot_metadata`.
- Decided: Annual production, CO2 savings and household equivalents are estimated from simple documented assumptions; full-load hours, emission factor and household consumption live in configuration or snapshot metadata.
- Decided: Concrete assumption values remain open until snapshot preparation, but snapshot metadata must include value, unit, source, source/retrieval date and calculation note.
- Decided: Municipal participation is shown as a short estimated expected §6 EEG value using 0.2 ct/kWh and estimated yearly production, with "Keine bestätigte Auszahlung" unless a payment source exists.
- Decided: The local history concept is "Zuletzt angesehen". Every opened wind park is recorded, regardless of whether it was reached from map, search or favorites.
- Decided: Favorites and recently viewed wind parks are SQLDelight-backed in the MVP.
- Decided: Location is optional, requested only after user action, not stored, and replaceable by manual search or pin placement.
- Decided: Top-level screens do not show back buttons to `Map`; bottom navigation handles top-level movement and back affordances are reserved for subflows.
- Decided: `Profile` is `Info & Einstellungen` in the MVP, with no logout, no account language, and no controls for unimplemented notifications or dark mode.
- Decided: The green Nature/Trust visual direction is accepted for MVP, but colors, typography, spacing, radii and elevation should be centralized as Compose theme/design tokens.
- Decided: Figma is the functional/visual reference for screen set, information architecture, component intent, rough layout and copy, not a pixel-perfect contract.
- Decided: AGP 9.x/KMP compatibility warning is accepted as a documented seminar-MVP risk; no module migration before demo unless the build breaks.
- Decided: Android manual QA is mandatory before demo; iOS simulator/device smoke test is optional where available and not a demo blocker.

## 22. Further Notes

This PRD intentionally combines the lecture structure with the WindKlar concept. It reflects the module phases: consulting and analysis, stakeholder and requirements work, feature prioritization via benefit and complexity, concept and design principles, implementation choices, QA and release thinking.

The strongest MVP is not the most feature-rich version. The strongest MVP is a trustworthy, understandable transparency flow: map, wind park detail, citizen impact, local benefit, data quality and saved context.
