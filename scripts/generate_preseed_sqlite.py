#!/usr/bin/env python3
"""Generate a manual WindKlar SQLDelight-compatible seed database."""

from __future__ import annotations

import argparse
import json
import sqlite3
from pathlib import Path
from typing import Any, Iterable


REPO_ROOT = Path(__file__).resolve().parents[1]
DEFAULT_SNAPSHOT = (
    REPO_ROOT
    / "composeApp"
    / "src"
    / "commonMain"
    / "composeResources"
    / "files"
    / "snapshots"
    / "windklar_snapshot.json"
)
DEFAULT_METADATA = DEFAULT_SNAPSHOT.with_name("windklar_snapshot_metadata.json")
DEFAULT_OUTPUT = REPO_ROOT / "data" / "snapshots" / "windklar_seed.db"
ANDROID_ASSETS_OUTPUT = (
    REPO_ROOT / "androidApp" / "src" / "main" / "assets" / "windklar_seed.db"
)
# Mirrors SnapshotSeedDataImporter companion constants so the importer's
# fast-path detects the preseeded DB and skips the full JSON import.
SETTING_METRIC_IMPORT_VERSION_KEY = "snapshot_metric_import_version"
SETTING_METRIC_IMPORT_VERSION = "snapshot_metrics_v1"
SCHEMA_DIR = (
    REPO_ROOT
    / "composeApp"
    / "src"
    / "commonMain"
    / "sqldelight"
    / "app"
    / "data"
    / "local"
    / "db"
)
SCHEMA_FILES = (
    "WindPark.sq",
    "WindTurbine.sq",
    "Metric.sq",
    "SnapshotMetadata.sq",
    "Favorite.sq",
    "RecentWindPark.sq",
    "DataHint.sq",
    "Setting.sq",
)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description=(
            "Generate windklar_seed.db from the bundled WindKlar JSON snapshot "
            "and the current SQLDelight CREATE TABLE schema."
        )
    )
    parser.add_argument(
        "--snapshot",
        type=Path,
        default=DEFAULT_SNAPSHOT,
        help=f"Path to windklar_snapshot.json (default: {DEFAULT_SNAPSHOT})",
    )
    parser.add_argument(
        "--metadata",
        type=Path,
        default=DEFAULT_METADATA,
        help=f"Path to windklar_snapshot_metadata.json (default: {DEFAULT_METADATA})",
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=DEFAULT_OUTPUT,
        help=f"Output SQLite database path (default: {DEFAULT_OUTPUT})",
    )
    parser.add_argument(
        "--force",
        action="store_true",
        help="Replace an existing output database.",
    )
    return parser.parse_args()


def read_json(path: Path) -> dict[str, Any]:
    with path.open("r", encoding="utf-8") as file:
        value = json.load(file)
    if not isinstance(value, dict):
        raise ValueError(f"{path} must contain a JSON object.")
    return value


def extract_create_table(sql_path: Path) -> str:
    sql = sql_path.read_text(encoding="utf-8")
    marker = "CREATE TABLE"
    start = sql.find(marker)
    if start == -1:
        raise ValueError(f"No CREATE TABLE statement found in {sql_path}.")
    end = sql.find(";", start)
    if end == -1:
        raise ValueError(f"Unterminated CREATE TABLE statement in {sql_path}.")
    return sql[start : end + 1]


def current_sqldelight_schema_version() -> int:
    migrations = [
        path
        for path in SCHEMA_DIR.glob("*.sqm")
        if path.stem.isdigit()
    ]
    if not migrations:
        return 1
    return max(int(path.stem) for path in migrations) + 1


def require_mapping(value: Any, key: str) -> dict[str, Any]:
    nested = value.get(key) if isinstance(value, dict) else None
    if not isinstance(nested, dict):
        raise ValueError(f"Expected object field '{key}'.")
    return nested


def require_list(value: dict[str, Any], key: str) -> list[Any]:
    nested = value.get(key)
    if not isinstance(nested, list):
        raise ValueError(f"Expected array field '{key}'.")
    return nested


def validate_metadata(snapshot: dict[str, Any], metadata_wrapper: dict[str, Any]) -> None:
    snapshot_metadata = require_mapping(snapshot, "snapshotMetadata")
    external_metadata = require_mapping(metadata_wrapper, "snapshotMetadata")
    for key in (
        "snapshotId",
        "sourceName",
        "sourceUrl",
        "mastrExportDate",
        "processedAt",
        "pipelineVersion",
        "checksumSha256",
    ):
        if snapshot_metadata.get(key) != external_metadata.get(key):
            raise ValueError(
                f"Metadata mismatch for '{key}': "
                f"{snapshot_metadata.get(key)!r} != {external_metadata.get(key)!r}"
            )


def create_schema(connection: sqlite3.Connection) -> None:
    for filename in SCHEMA_FILES:
        connection.execute(extract_create_table(SCHEMA_DIR / filename))
    connection.execute(f"PRAGMA user_version = {current_sqldelight_schema_version()}")


def insert_wind_parks(connection: sqlite3.Connection, rows: Iterable[dict[str, Any]]) -> int:
    values = [
        (
            row["id"],
            row["name"],
            row["municipalityId"],
            row["municipalityName"],
            row["districtId"],
            row["districtName"],
            row["stateId"],
            row["stateName"],
            row["latitude"],
            row["longitude"],
            row.get("turbineCount"),
            row.get("installedCapacityKw"),
            row["groupingMethod"],
            row["sourceName"],
            row["sourceUrl"],
            row["sourceUpdatedAt"],
            row["dataQuality"],
        )
        for row in rows
    ]
    connection.executemany(
        """
        INSERT INTO wind_park(
            id,
            name,
            municipality_id,
            municipality_name,
            district_id,
            district_name,
            state_id,
            state_name,
            latitude,
            longitude,
            turbine_count,
            installed_capacity_kw,
            grouping_method,
            source_name,
            source_url,
            source_updated_at,
            data_quality
        )
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        values,
    )
    return len(values)


def insert_wind_turbines(connection: sqlite3.Connection, rows: Iterable[dict[str, Any]]) -> int:
    values = [
        (
            row["id"],
            row["windParkId"],
            row["name"],
            row["municipalityId"],
            row["municipalityName"],
            row["districtId"],
            row["districtName"],
            row["stateId"],
            row["stateName"],
            row["latitude"],
            row["longitude"],
            row.get("installedCapacityKw"),
            row.get("status"),
            row.get("turbineType"),
            row.get("manufacturer"),
            row.get("model"),
            row.get("hubHeightM"),
            row.get("rotorDiameterM"),
            row["sourceName"],
            row["sourceUrl"],
            row["sourceUpdatedAt"],
            row["dataQuality"],
        )
        for row in rows
    ]
    connection.executemany(
        """
        INSERT INTO wind_turbine(
            id,
            wind_park_id,
            name,
            municipality_id,
            municipality_name,
            district_id,
            district_name,
            state_id,
            state_name,
            latitude,
            longitude,
            installed_capacity_kw,
            status,
            turbine_type,
            manufacturer,
            model,
            hub_height_m,
            rotor_diameter_m,
            source_name,
            source_url,
            source_updated_at,
            data_quality
        )
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        values,
    )
    return len(values)


def insert_metrics(connection: sqlite3.Connection, rows: Iterable[dict[str, Any]]) -> int:
    values = [
        (
            row["id"],
            row["subjectType"],
            row["subjectId"],
            row["metricType"],
            row.get("value"),
            row["unit"],
            row.get("period"),
            row["sourceName"],
            row["sourceUrl"],
            row["sourceUpdatedAt"],
            row["dataQuality"],
            row.get("calculationNote"),
        )
        for row in rows
    ]
    connection.executemany(
        """
        INSERT INTO metric(
            id,
            subject_type,
            subject_id,
            metric_type,
            metric_value,
            unit,
            period,
            source_name,
            source_url,
            source_updated_at,
            data_quality,
            calculation_note
        )
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        values,
    )
    return len(values)


def insert_snapshot_metadata(connection: sqlite3.Connection, snapshot: dict[str, Any]) -> None:
    metadata = require_mapping(snapshot, "snapshotMetadata")
    assumptions = require_list(snapshot, "assumptions")
    limitations = metadata.get("limitations")
    if not isinstance(limitations, list):
        raise ValueError("Expected snapshotMetadata.limitations to be an array.")

    connection.execute(
        """
        INSERT INTO snapshot_metadata(
            snapshot_id,
            schema_version,
            source_name,
            source_url,
            attribution,
            mastr_export_date,
            processed_at,
            pipeline_version,
            checksum_sha256,
            assumptions_json,
            limitations,
            imported_at
        )
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        (
            metadata["snapshotId"],
            str(snapshot["schemaVersion"]),
            metadata["sourceName"],
            metadata["sourceUrl"],
            metadata["attribution"],
            metadata["mastrExportDate"],
            metadata["processedAt"],
            metadata["pipelineVersion"],
            metadata["checksumSha256"],
            json.dumps(assumptions, ensure_ascii=False, separators=(",", ":")),
            "\n".join(str(item) for item in limitations),
            metadata["processedAt"],
        ),
    )


def populate_database(connection: sqlite3.Connection, snapshot: dict[str, Any]) -> dict[str, int]:
    wind_parks = require_list(snapshot, "windParks")
    wind_turbines = require_list(snapshot, "windTurbines")
    metrics = require_list(snapshot, "metrics")

    with connection:
        create_schema(connection)
        park_count = insert_wind_parks(connection, wind_parks)
        turbine_count = insert_wind_turbines(connection, wind_turbines)
        metric_count = insert_metrics(connection, metrics)
        insert_snapshot_metadata(connection, snapshot)
        connection.execute(
            "INSERT OR REPLACE INTO app_setting(key, value) VALUES (?, ?)",
            (SETTING_METRIC_IMPORT_VERSION_KEY, SETTING_METRIC_IMPORT_VERSION),
        )

    return {
        "wind_park": park_count,
        "wind_turbine": turbine_count,
        "metric": metric_count,
        "snapshot_metadata": 1,
    }


def generate_database(snapshot_path: Path, metadata_path: Path, output_path: Path, force: bool) -> None:
    snapshot = read_json(snapshot_path)
    metadata_wrapper = read_json(metadata_path)
    validate_metadata(snapshot, metadata_wrapper)

    output_path.parent.mkdir(parents=True, exist_ok=True)
    if output_path.exists() and not force:
        raise FileExistsError(f"{output_path} already exists. Use --force to replace it.")

    temporary_output = output_path.with_name(output_path.name + ".tmp")
    if temporary_output.exists():
        temporary_output.unlink()

    try:
        connection = sqlite3.connect(temporary_output)
        try:
            connection.execute("PRAGMA foreign_keys = ON")
            connection.execute("PRAGMA synchronous = OFF")
            counts = populate_database(connection, snapshot)
            violations = connection.execute("PRAGMA foreign_key_check").fetchall()
            if violations:
                raise ValueError(f"Foreign key check failed: {violations[:5]}")
        finally:
            connection.close()

        temporary_output.replace(output_path)
    except Exception:
        if temporary_output.exists():
            temporary_output.unlink()
        raise

    formatted_counts = ", ".join(f"{table}={count}" for table, count in counts.items())
    print(f"Wrote {output_path}")
    print(f"Rows: {formatted_counts}")
    print(f"SQLDelight user_version={current_sqldelight_schema_version()}")

    if ANDROID_ASSETS_OUTPUT.parent.exists():
        ANDROID_ASSETS_OUTPUT.parent.mkdir(parents=True, exist_ok=True)
        import shutil
        shutil.copy2(output_path, ANDROID_ASSETS_OUTPUT)
        print(f"Copied preseed DB to Android assets: {ANDROID_ASSETS_OUTPUT}")
    else:
        print(
            "Skipped Android assets copy (androidApp/src/main/assets not found)."
        )


def main() -> None:
    args = parse_args()
    generate_database(
        snapshot_path=args.snapshot.resolve(),
        metadata_path=args.metadata.resolve(),
        output_path=args.output.resolve(),
        force=args.force,
    )


if __name__ == "__main__":
    main()
