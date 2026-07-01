# WindKlar

WindKlar is the product context for making local onshore wind energy understandable to citizens in Germany. The language centers on onshore wind parks, their municipalities, local production context, and citizen-facing trust signals. Offshore wind installations and offshore wind parks are completely excluded from the app's scope.

## Language

**Windpark**:
A local grouping of one or more onshore wind energy installations that users discover, save, and inspect as the primary UX object in the app.
_Avoid_: Anlage as the primary discovery object, turbine as the primary discovery object, offshore wind parks

**Windanlage**:
An individual onshore wind energy installation that can belong to a wind park and is the atomic unit for master data, coordinates, and data hints.
_Avoid_: Windpark when referring to a single installation, offshore wind turbines

**Gemeinde**:
The municipality connected to a wind park and used to frame local production, benefit, and context.
_Avoid_: city, region, location when the municipal administration or community is meant

**Produktionskontext**:
The citizen-facing explanation of a wind park's or municipality's energy production, including comparable metrics such as annual production, CO2 savings, and household equivalents.
_Avoid_: raw stats, technical dashboard

**Favorit**:
A wind park saved locally by the user for quick return without requiring an account.
_Avoid_: bookmark, account favorite

**Zuletzt angesehen**:
The locally stored list of wind parks a user opened most recently, regardless of whether the park was reached from map, search, or favorites.
_Avoid_: Suchverlauf when map and favorite openings are included, analytics history, tracking history

**Datenqualitaet**:
A visible trust label that tells users whether a displayed value is official, measured, derived, estimated, simulated, or missing.
_Avoid_: accuracy when the app only knows provenance/confidence

**Datenhinweis**:
A user-submitted or automatically detected hint that a wind installation or its master data may be missing, incorrect, outdated, or worth review.
_Avoid_: official correction, MaStR correction, support ticket

**Datenhinweis-Kategorie**:
The reason a data hint was submitted: missing installation, wrong location, wrong status, wrong wind park assignment, wrong technical data, installation removed, or other.
_Avoid_: free-form issue type for MVP

**Hinweissicherheit**:
The user's confidence in a data hint, expressed as unsure, likely, or certain.
_Avoid_: proof, verification

**Hinweisstatus**:
The local review/export state of a data hint, such as draft, ready for review, or exported.
_Avoid_: moderation status when no backend moderation exists

**Metric**:
A displayed quantitative value with its own unit, period, source, data quality, and optional calculation note.
_Avoid_: raw column when the value needs user-facing trust context

**Abgeleitete Daten**:
Values computed mechanically from official or source-backed fields, such as wind park turbine count or aggregated installed capacity derived from individual MaStR wind installations.
_Avoid_: estimated data when no assumption or model is involved

**Anlagen-Stammdaten**:
Publicly available master data for an individual wind energy installation, such as type, height, installed capacity, coordinates, status, and wind park assignment.
_Avoid_: production data, live data

**Energiedaten**:
Production-related data for a wind park or wind energy installation, such as expected yearly production, full-load hours, or forecast-based estimates.
_Avoid_: master data

**Akzeptanzvisualisierung**:
A citizen-facing visualization that translates technical or energy data into understandable local impact, such as households supplied, CO2 savings, or possible municipal participation.
_Avoid_: raw KPI, operator report

**Kommunale Beteiligung**:
An estimated citizen-facing value for expected municipal participation under §6 EEG, based on assumed 0.2 ct/kWh and estimated yearly production unless confirmed payment data is available.
_Avoid_: confirmed municipal payment without a payment source

**Echte Stammdaten**:
Public source-backed master data for wind energy installations, especially MaStR fields such as coordinates, installed capacity, status, type, height, and wind park assignment.
_Avoid_: demo data when the value comes from a public register

**Geschaetzte Wirkungsdaten**:
Derived or assumed impact values such as expected yearly production, CO2 savings, households supplied, or possible municipal participation when measured public values are unavailable.
_Avoid_: measured data, official production data

**Berechnungsannahme**:
A documented configurable assumption used to calculate estimated impact values, such as full-load hours, emission factor, or average household electricity consumption.
_Avoid_: hard-coded UI constant

**Deutschlandweiter Datensatz**:
The MVP data scope covers wind energy installations across Germany rather than a local Leipzig or Saxony-only demo region.
_Avoid_: demo region when describing the intended MVP scope

**Snapshot-Datensatz**:
A preprocessed JSON data extract from public sources that is bundled with or imported into the app for the MVP instead of being fetched live from an external API at runtime.
_Avoid_: live API when describing MVP data loading, raw CSV as the app-facing format

**Snapshot-Metadaten**:
Metadata describing the bundled/imported snapshot, including source, import timestamp, version, and calculation assumptions.
_Avoid_: hidden import constants

**Quellengebundener FAQ-Assistent**:
A later optional explanation assistant that answers natural-language questions only from curated WindKlar knowledge, selected app context, source metadata and data quality labels.
_Avoid_: general chatbot, unsupported AI advice, live operations diagnosis

**Live-Betriebsursache**:
The current operational reason why a wind installation is running, curtailed or standing still, which WindKlar may only claim when a source-backed live status or event source exists.
_Avoid_: presenting common possible reasons as known current facts

**Windpark-Aggregat**:
A precomputed grouping of wind installations in the snapshot, including representative location, turbine count, installed capacity, and municipality context for app display.
_Avoid_: runtime grouping in the app for MVP

**Kartenansicht**:
The shared discovery surface for browsing wind park aggregates and, at detailed levels, individual wind installations.
_Avoid_: separate native map products for Android and iOS in the MVP

**Nutzerstandort**:
An optional, user-initiated location context used to center the map or support nearby discovery, not stored as part of the MVP.
_Avoid_: required onboarding permission, tracking location

**Nature/Trust Designrichtung**:
The accepted MVP visual direction using green, nature-oriented, trustworthy styling for citizen-facing wind energy transparency.
_Avoid_: unrelated tech-dashboard aesthetic, one-off screen-local color systems
