# WindKlar data pipeline

This directory keeps public source-data work outside the app runtime. The
Compose app receives only an app-ready JSON snapshot and imports that snapshot
into SQLDelight.

## Directory layout

- `pipeline/`: Python tooling for fetching, normalizing, aggregating,
  calculating, validating, and exporting snapshots.
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

- Official target export checked: `Gesamtdatenexport_20260617_26.1.zip`
  from the MaStR datendownload page, last updated `2026-06-17 00:00:00`.
- Local direct access to `download.marktstammdatenregister.de` reset HTTPS
  download connections in this environment.
- The generated app snapshot therefore uses the open-MaStR Zenodo unboxed wind
  CSV file `bnetza_mastr_wind_raw.csv.zip` from record
  https://zenodo.org/records/14843222, published `2025-02-10`.
- Raw downloaded file: `data/raw/bnetza_mastr_wind_raw.csv.zip` (ignored).
- Full generated snapshot:
  `data/snapshots/windklar_snapshot_open_mastr_2025-02-10.json`.
- Bundled app snapshot:
  `composeApp/src/commonMain/composeResources/files/snapshots/windklar_snapshot.json`.

## Common commands

Run the pipeline module from the repository root:

```powershell
python -m data.pipeline validate data/snapshots/windklar_snapshot_smoke.json
python -m data.pipeline smoke --output data/snapshots/windklar_snapshot_smoke.json
python -m data.pipeline fetch --source-url <official-export-url>
python -m data.pipeline fetch --source-url https://zenodo.org/api/records/14843222/files/bnetza_mastr_wind_raw.csv.zip/content
python -m data.pipeline normalize --input data/raw/<export-file> --output data/intermediate/wind_turbines.jsonl
python -m data.pipeline aggregate --input data/intermediate/wind_turbines.jsonl --output data/intermediate/wind_parks.json
python -m data.pipeline calculate --turbines data/intermediate/wind_turbines.jsonl --parks data/intermediate/wind_parks.json --output data/snapshots/windklar_snapshot_YYYY-MM-DD.json
```

Copy the selected demo snapshot to
`composeApp/src/commonMain/composeResources/files/snapshots/windklar_snapshot.json`
before building the app.

## Known limitations

- Wind park grouping is derived preprocessing, not official MaStR meaning.
- Impact metrics are MVP estimates based on documented assumptions.
- Raw MaStR files are intentionally ignored because they are large and updated
  frequently.
- The smoke snapshot remains useful for contract checks, but the currently
  bundled app snapshot is the generated open-MaStR wind snapshot.
