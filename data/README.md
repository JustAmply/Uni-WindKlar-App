# WindKlar data pipeline

This directory keeps public source-data work outside the app runtime. The
Compose app receives only an app-ready JSON snapshot and imports that snapshot
into SQLDelight.

## Directory layout

- `pipeline/`: Python tooling for fetching, normalizing, cleaning, repairing,
  aggregating, calculating, validating, and exporting snapshots.
- `raw/`: ignored MaStR downloads such as ZIP or XML exports.
- `intermediate/`: ignored normalized JSONL files.
- `snapshots/`: app-ready snapshot releases that are small enough to commit, or
  smoke snapshots used for demo/build checks.

## Source policy

The official MaStR bulk download from Bundesnetzagentur is the source of truth:
https://www.marktstammdatenregister.de/MaStR/Datendownload

`open-mastr` may be used as an adapter if it makes download or parsing easier,
but it is not the domain source. App runtime code must not call MaStR or
open-mastr directly.

Current repository snapshot:

- Official MaStR export used: `Gesamtdatenexport_20260620_26.1.zip`
  from the MaStR datendownload page.
- Raw MaStR exports are ignored and are not committed because they are large.
- Full generated snapshot:
  `data/snapshots/windklar_snapshot_2026-06-20.json` (ignored).
- Bundled app snapshot:
  `composeApp/src/commonMain/composeResources/files/snapshots/windklar_snapshot.json`.
- Bundled app metadata:
  `composeApp/src/commonMain/composeResources/files/snapshots/windklar_snapshot_metadata.json`.

## Common commands

Run the pipeline module from the repository root:

```powershell
python -m data.pipeline validate data/snapshots/windklar_snapshot_smoke.json
python -m data.pipeline smoke --output data/snapshots/windklar_snapshot_smoke.json
python -m data.pipeline fetch --source-url <official-export-url>
python -m data.pipeline fetch --source-url https://zenodo.org/api/records/14843222/files/bnetza_mastr_wind_raw.csv.zip/content
python -m data.pipeline normalize --input data/raw/<export-file> --output data/intermediate/wind_turbines.jsonl
python -m data.pipeline clean --input data/intermediate/wind_turbines.jsonl --municipalities data/raw/vg250_gemeinden.geojson --output data/intermediate/wind_turbines_clean.jsonl --report data/snapshots/windklar_cleaning_report_YYYY-MM-DD.json --metrics-output data/snapshots/windklar_cleaning_metrics_YYYY-MM-DD.json
python -m data.pipeline repair --input data/intermediate/wind_turbines.jsonl --municipalities data/raw/vg250_gemeinden.geojson --output data/intermediate/wind_turbines_repaired.jsonl --report data/snapshots/windklar_repair_report_YYYY-MM-DD.json --metrics-output data/snapshots/windklar_repair_metrics_YYYY-MM-DD.json
python -m data.pipeline aggregate --input data/intermediate/wind_turbines_repaired.jsonl --output data/intermediate/wind_parks.json
python -m data.pipeline calculate --turbines data/intermediate/wind_turbines_repaired.jsonl --parks data/intermediate/wind_parks.json --output data/snapshots/windklar_snapshot_YYYY-MM-DD.json --quality-report data/snapshots/windklar_repair_report_YYYY-MM-DD.json
```

Copy the selected demo snapshot to
`composeApp/src/commonMain/composeResources/files/snapshots/windklar_snapshot.json`
before building the app.

### Preseed SQLite generator

The preseed path is active on Android. To regenerate a SQLDelight-compatible
SQLite database from the bundled app snapshot, run this command from the
repository root:

```powershell
python scripts/generate_preseed_sqlite.py --force
```

By default this reads
`composeApp/src/commonMain/composeResources/files/snapshots/windklar_snapshot.json`
and `windklar_snapshot_metadata.json`, then writes
`data/snapshots/windklar_seed.db` and copies the result to
`androidApp/src/main/assets/windklar_seed.db`. The preseed database also
contains the `app_setting` row for the snapshot metric import version, so
`SnapshotSeedDataImporter` detects it via its fast-path checksum check and
skips the full JSON import on first start.

The output directory is ignored, so the database can be regenerated manually
when the bundled snapshot or SQLDelight schema changes.

Runtime behavior:

- Android (`MainActivity`): on first start, copies the bundled
  `windklar_seed.db` asset to the app database path before creating the
  SQLDelight driver. The importer then verifies the checksum and skips the
  JSON import. Subsequent starts reuse the existing database file.
- iOS: the preseed database is not yet added to the Xcode app bundle; iOS
  falls back to the JSON import path. To activate iOS preseed, add
  `windklar_seed.db` to the `iosApp` target resources and mirror the
  `preseedDatabaseFromAssets` copy step in `MainViewController.kt` before
  `NativeSqliteDriver` is created.

## Known limitations

- Wind park grouping is derived preprocessing, not official MaStR meaning.
- Municipality/coordinate cleaning uses a locally provided BKG VG250
  municipality GeoJSON. Hard mismatches are excluded from the app snapshot and
  documented in the cleaning report; near-boundary cases within 1 km are kept
  with warnings because VG250 geometry is generalized. The cleaning command
  also writes compact metrics automatically next to the report, or to
  `--metrics-output` when that path is provided. The repair command is the
  preferred snapshot path because it keeps auditable coordinate-based
  municipality repairs and offshore pseudo-municipalities instead of filtering
  them out.
- Impact metrics are MVP estimates based on documented assumptions.
- Raw MaStR files are intentionally ignored because they are large and updated
  frequently.
- The smoke snapshot remains useful for contract checks, but the currently
  bundled app snapshot is the generated open-MaStR wind snapshot.
