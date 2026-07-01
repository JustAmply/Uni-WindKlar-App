# WindKlar data pipeline

This directory keeps public source-data work outside the app runtime. The
Compose app receives only an app-ready source SQLite seed database. JSON
snapshots may exist as pipeline artifacts, but they are not bundled with or
decoded by the app runtime.

## Directory layout

- `pipeline/`: Python tooling for fetching, normalizing, cleaning, repairing,
  aggregating, calculating, validating, and exporting snapshots.
- `raw/`: ignored MaStR downloads such as ZIP or XML exports.
- `intermediate/`: ignored normalized JSONL files.
- `snapshots/`: ignored generated snapshot releases, source seed databases, and
  smoke snapshots used for demo/build checks.

## Source policy

The official MaStR bulk download from Bundesnetzagentur is the source of truth:
https://www.marktstammdatenregister.de/MaStR/Datendownload

Helper tooling may be used for download or parsing, but it is not the domain
source. App runtime code must not call MaStR directly.

Current repository snapshot:

- Official MaStR export used: `Gesamtdatenexport_20260620_26.1.zip`
  from the MaStR datendownload page.
- Raw MaStR exports are ignored and are not committed because they are large.
- Full generated snapshots live in `data/snapshots/windklar_snapshot_YYYY-MM-DD.json` (ignored).
- Bundled app source database:
  `androidApp/src/main/assets/windklar_source_seed.db` and
  `iosApp/iosApp/Resources/windklar_source_seed.db`.

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

### Preseed SQLite generator

To regenerate the SQLDelight-compatible source SQLite seed database from the
latest local pipeline snapshot, run this command from the repository root:

```powershell
python scripts/generate_preseed_sqlite.py --force
```

By default this reads the newest dated
`data/snapshots/windklar_snapshot_*.json`, writes
`data/snapshots/windklar_source_seed.db`, and copies the result to Android and
iOS bundle resources. Pass `--snapshot <path>` to select a specific pipeline
snapshot.

The output directory is ignored, so the database can be regenerated manually
when the bundled snapshot or SQLDelight schema changes.

Runtime behavior:

- Android and iOS copy `windklar_source_seed.db` to `windklar_source.db` before
  creating SQLDelight drivers. If the bundled `snapshot_metadata.checksum_sha256`
  changes, only the source DB is replaced.
- Local user data is stored separately in `windklar_user.db`; app updates do
  not replace it.
- There is no runtime JSON fallback and no "start without data" mode.

## Known limitations

- Wind park grouping is derived preprocessing, not official MaStR meaning.
- Municipality/coordinate cleaning uses a locally provided BKG VG250
  municipality GeoJSON. Hard mismatches are excluded from the app snapshot and
  documented in the cleaning report; near-boundary cases within 1 km are kept
  with warnings because VG250 geometry is generalized. The cleaning command
  also writes compact metrics automatically next to the report, or to
  `--metrics-output` when that path is provided. The repair command is the
  preferred snapshot path because it keeps auditable coordinate-based
  municipality repairs. Offshore wind turbines and pseudo-municipalities are
  filtered out entirely, as the app strictly targets onshore wind energy.
- Impact metrics are MVP estimates based on documented assumptions.
- Raw MaStR files are intentionally ignored because they are large and updated
  frequently.
- The smoke snapshot remains useful for contract checks, but the bundled app
  artifact is the generated MaStR source SQLite seed database.
