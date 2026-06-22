from __future__ import annotations

import argparse
import csv
import hashlib
import json
import shutil
import sys
import urllib.request
import xml.etree.ElementTree as ET
import zipfile
from collections import defaultdict
from copy import deepcopy
from datetime import datetime, timezone
from math import cos, radians, sqrt
from pathlib import Path
from typing import Any, Iterable

PIPELINE_VERSION = "0.3.0"
SOURCE_NAME = "Marktstammdatenregister der Bundesnetzagentur"
SOURCE_URL = "https://www.marktstammdatenregister.de/MaStR/Datendownload"
ATTRIBUTION = "Quelle: Marktstammdatenregister der Bundesnetzagentur"
BKG_VG250_SOURCE_NAME = "BKG VG250 Verwaltungsgebiete"
BKG_VG250_SOURCE_URL = "https://gdz.bkg.bund.de/index.php/default/digitale-geodaten/verwaltungsgebiete.html"
VALID_QUALITIES = {"official", "measured", "derived", "estimated", "simulated", "missing"}
GERMANY_LAT_RANGE = (47.0, 55.2)
GERMANY_LON_RANGE = (5.5, 15.5)
BOUNDARY_TOLERANCE_KM = 1.0
MAX_REPAIR_DISTANCE_KM = 30.0
SPATIAL_INDEX_DEGREES = 0.25
OFFSHORE_NORTH_SEA_ID = "offshore_north_sea"
OFFSHORE_NORTH_SEA_NAME = "Offshore Nordsee"
OFFSHORE_BALTIC_SEA_ID = "offshore_baltic_sea"
OFFSHORE_BALTIC_SEA_NAME = "Offshore Ostsee"
OFFSHORE_MIN_LAT = 53.5
OFFSHORE_NORTH_SEA_MAX_LON = 10.0

DEFAULT_ASSUMPTIONS = {
    "full_load_hours": {
        "label": "Angenommene jährliche Volllaststunden",
        "value": 2000.0,
        "unit": "h/a",
        "sourceName": "WindKlar MVP-Annahme",
        "sourceUrl": SOURCE_URL,
        "sourceDate": "2026-06-18",
        "calculationNote": "MVP-Schätzung, bis eine regional belegte Annahme gewählt wird.",
    },
    "emission_factor_kg_per_kwh": {
        "label": "Vermiedenes CO₂ pro kWh",
        "value": 0.38,
        "unit": "kg CO2/kWh",
        "sourceName": "WindKlar MVP-Annahme",
        "sourceUrl": SOURCE_URL,
        "sourceDate": "2026-06-18",
        "calculationNote": "MVP-Schätzung für die klimarelevante Wirkung aus Bürgersicht.",
    },
    "household_consumption_kwh": {
        "label": "Durchschnittlicher jährlicher Haushaltsstrombedarf",
        "value": 3500.0,
        "unit": "kWh/a",
        "sourceName": "WindKlar MVP-Annahme",
        "sourceUrl": SOURCE_URL,
        "sourceDate": "2026-06-18",
        "calculationNote": "MVP-Schätzung für Haushaltsäquivalente.",
    },
    "municipal_benefit_eur_per_kwh": {
        "label": "Geschätzte kommunale Beteiligung nach § 6 EEG",
        "value": 0.002,
        "unit": "EUR/kWh",
        "sourceName": "WindKlar MVP-Annahme",
        "sourceUrl": SOURCE_URL,
        "sourceDate": "2026-06-18",
        "calculationNote": "Schätzung von 0,2 ct/kWh; keine bestätigte Auszahlung.",
    },
}

FIELD_ALIASES = {
    "id": ["id", "mastr_id", "einheitmastrnummer", "einheit_mastr_nummer", "mastrnummer"],
    "windParkId": ["windparkid", "wind_park_id", "windpark_id", "lokationmastrnummer"],
    "windParkName": ["windparkname", "wind_park_name", "namewindpark"],
    "name": ["name", "anlagenname", "einheitname", "einheitenname", "bezeichnung"],
    "municipalityId": ["gemeindeid", "municipalityid", "municipality_id", "ags", "gemeindeschluessel"],
    "municipalityName": ["gemeinde", "gemeindename", "municipality", "municipalityname", "ort"],
    "latitude": ["latitude", "lat", "breitengrad"],
    "longitude": ["longitude", "lon", "lng", "laengengrad", "langengrad"],
    "installedCapacityKw": ["installedcapacitykw", "nettonennleistung", "bruttonennleistung", "leistungkw"],
    "status": ["status", "betriebsstatus", "einheitbetriebsstatus"],
    "turbineType": ["turbinetype", "technologie", "energietraeger", "energietrager"],
    "manufacturer": ["manufacturer", "hersteller"],
    "model": ["model", "typ", "anlagenmodell"],
    "hubHeightM": ["hubheightm", "nabenhoehe", "nabenhohe"],
    "rotorDiameterM": ["rotordiameterm", "rotordurchmesser"],
}

MUNICIPALITY_FIELD_ALIASES = {
    "id": ["ags", "gemeindeid", "gemeindeschluessel", "municipalityid", "municipality_id", "rs"],
    "name": ["gen", "gemeinde", "gemeindename", "name", "municipality", "municipalityname"],
}


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="WindKlar MaStR snapshot pipeline")
    subparsers = parser.add_subparsers(dest="command", required=True)

    fetch_parser = subparsers.add_parser("fetch", help="Download an official MaStR export")
    fetch_parser.add_argument("--source-url", required=True)
    fetch_parser.add_argument("--output-dir", default="data/raw")

    normalize_parser = subparsers.add_parser("normalize", help="Normalize source rows to turbine JSONL")
    normalize_parser.add_argument("--input", required=True)
    normalize_parser.add_argument("--output", default="data/intermediate/wind_turbines.jsonl")

    clean_parser = subparsers.add_parser("clean", help="Clean turbines against BKG VG250 municipality geometry")
    clean_parser.add_argument("--input", required=True)
    clean_parser.add_argument("--municipalities", required=True)
    clean_parser.add_argument("--output", default="data/intermediate/wind_turbines_clean.jsonl")
    clean_parser.add_argument("--report", default=f"data/snapshots/windklar_cleaning_report_{today()}.json")
    clean_parser.add_argument("--metrics-output")
    clean_parser.add_argument("--boundary-tolerance-km", type=float, default=BOUNDARY_TOLERANCE_KM)

    repair_parser = subparsers.add_parser("repair", help="Repair turbines using BKG VG250 municipality geometry")
    repair_parser.add_argument("--input", required=True)
    repair_parser.add_argument("--municipalities", required=True)
    repair_parser.add_argument("--output", default="data/intermediate/wind_turbines_repaired.jsonl")
    repair_parser.add_argument("--report", default=f"data/snapshots/windklar_repair_report_{today()}.json")
    repair_parser.add_argument("--metrics-output")
    repair_parser.add_argument("--boundary-tolerance-km", type=float, default=BOUNDARY_TOLERANCE_KM)

    aggregate_parser = subparsers.add_parser("aggregate", help="Build derived wind park aggregates")
    aggregate_parser.add_argument("--input", required=True)
    aggregate_parser.add_argument("--output", default="data/intermediate/wind_parks.json")

    calculate_parser = subparsers.add_parser("calculate", help="Build a complete app snapshot")
    calculate_parser.add_argument("--turbines", required=True)
    calculate_parser.add_argument("--parks", required=True)
    calculate_parser.add_argument("--output", required=True)
    calculate_parser.add_argument("--mastr-export-date", default=today())
    calculate_parser.add_argument("--cleaning-report")
    calculate_parser.add_argument("--repair-report")
    calculate_parser.add_argument("--quality-report")

    validate_parser = subparsers.add_parser("validate", help="Validate an app snapshot")
    validate_parser.add_argument("snapshot")

    smoke_parser = subparsers.add_parser("smoke", help="Write a tiny valid smoke snapshot")
    smoke_parser.add_argument("--output", default="data/snapshots/windklar_snapshot_smoke.json")

    args = parser.parse_args(argv)
    if args.command == "fetch":
        return fetch(args.source_url, Path(args.output_dir))
    if args.command == "normalize":
        return normalize(Path(args.input), Path(args.output))
    if args.command == "clean":
        return clean(
            Path(args.input),
            Path(args.municipalities),
            Path(args.output),
            Path(args.report),
            Path(args.metrics_output) if args.metrics_output else None,
            args.boundary_tolerance_km,
        )
    if args.command == "repair":
        return repair(
            Path(args.input),
            Path(args.municipalities),
            Path(args.output),
            Path(args.report),
            Path(args.metrics_output) if args.metrics_output else None,
            args.boundary_tolerance_km,
        )
    if args.command == "aggregate":
        return aggregate(Path(args.input), Path(args.output))
    if args.command == "calculate":
        quality_report = args.quality_report or args.repair_report or args.cleaning_report
        quality_report_path = Path(quality_report) if quality_report else None
        return calculate(Path(args.turbines), Path(args.parks), Path(args.output), args.mastr_export_date, quality_report_path)
    if args.command == "validate":
        return validate(Path(args.snapshot))
    if args.command == "smoke":
        return write_smoke_snapshot(Path(args.output))
    raise AssertionError(args.command)


def fetch(source_url: str, output_dir: Path) -> int:
    output_dir.mkdir(parents=True, exist_ok=True)
    with urllib.request.urlopen(source_url) as response:
        filename = filename_from_response(source_url, response)
        target = output_dir / filename
        with target.open("wb") as handle:
            shutil.copyfileobj(response, handle)
    checksum = sha256_file(target)
    (target.with_suffix(target.suffix + ".sha256")).write_text(f"{checksum}  {target.name}\n", encoding="utf-8")
    print(f"Downloaded {target} ({checksum})")
    return 0


def normalize(input_path: Path, output_path: Path) -> int:
    output_path.parent.mkdir(parents=True, exist_ok=True)
    count = 0
    with output_path.open("w", encoding="utf-8", newline="\n") as handle:
        for row in iter_source_rows(input_path):
            turbine = normalize_row(row)
            if turbine is None:
                continue
            handle.write(json.dumps(turbine, sort_keys=True, ensure_ascii=True) + "\n")
            count += 1
    print(f"Wrote {count} normalized wind turbines to {output_path}")
    return 0 if count > 0 else 2


def clean(
    input_path: Path,
    municipalities_path: Path,
    output_path: Path,
    report_path: Path,
    metrics_output_path: Path | None,
    boundary_tolerance_km: float,
) -> int:
    municipalities = load_municipalities(municipalities_path)
    if not municipalities:
        print(f"ERROR: No municipality polygons found in {municipalities_path}", file=sys.stderr)
        return 2
    spatial_index = build_spatial_index(municipalities)

    seen_by_id: dict[str, dict[str, Any]] = {}
    kept: list[dict[str, Any]] = []
    excluded: list[dict[str, Any]] = []
    warnings: list[dict[str, Any]] = []
    duplicate_bit_equal_count = 0

    input_count = 0
    for turbine in read_jsonl(input_path):
        input_count += 1
        reason = basic_turbine_error(turbine)
        if reason:
            excluded.append(exclusion(turbine, reason))
            continue

        turbine_id = turbine["id"]
        previous = seen_by_id.get(turbine_id)
        if previous is not None:
            if canonical_json(previous) == canonical_json(turbine):
                duplicate_bit_equal_count += 1
            else:
                excluded.append(exclusion(turbine, "duplicate_turbine_id_conflict"))
            continue
        seen_by_id[turbine_id] = turbine

        municipality_id = normalize_municipality_id(turbine.get("municipalityId"))
        if municipality_id is None:
            excluded.append(exclusion(turbine, "invalid_municipality_id"))
            continue
        turbine["municipalityId"] = municipality_id

        lat = turbine["latitude"]
        lon = turbine["longitude"]
        candidates = municipalities_containing_point(municipalities, spatial_index, lon, lat)
        matched = first_matching_municipality(candidates, municipality_id)
        if matched is not None:
            kept.append(turbine)
            continue

        detected = candidates[0] if len(candidates) == 1 else None
        expected = municipalities.get(municipality_id)
        if not candidates and expected is not None:
            distance_km = distance_to_geometry_km(lon, lat, expected["geometry"])
            if distance_km <= boundary_tolerance_km:
                warnings.append(
                    warning(
                        "municipality_boundary_ambiguous",
                        turbine,
                        expected,
                        distanceKm=round(distance_km, 3),
                    )
                )
                kept.append(turbine)
                continue

        if detected is not None:
            excluded.append(exclusion(turbine, "municipality_coordinate_mismatch", detected))
        else:
            excluded.append(exclusion(turbine, "coordinate_outside_municipality_reference"))

    warnings.extend(mixed_municipality_wind_park_warnings(kept))

    output_path.parent.mkdir(parents=True, exist_ok=True)
    with output_path.open("w", encoding="utf-8", newline="\n") as handle:
        for turbine in sorted(kept, key=lambda item: item["id"]):
            handle.write(json.dumps(turbine, sort_keys=True, ensure_ascii=True) + "\n")

    report = {
        "summary": {
            "inputCount": input_count,
            "keptCount": len(kept),
            "excludedCount": len(excluded),
            "boundaryAmbiguousCount": count_by_code(warnings, "municipality_boundary_ambiguous"),
            "duplicateBitEqualCount": duplicate_bit_equal_count,
            "duplicateConflictCount": count_by_code(excluded, "duplicate_turbine_id_conflict"),
            "mixedMunicipalityWindParkCount": count_by_code(warnings, "mixed_municipality_wind_park"),
            "boundaryToleranceKm": boundary_tolerance_km,
        },
        "excluded": sorted(excluded, key=lambda item: (item["reasonCode"], item.get("turbineId") or "")),
        "warnings": sorted(warnings, key=lambda item: (item["reasonCode"], item.get("turbineId") or item.get("windParkKey") or "")),
        "sources": {
            "mastr": {
                "sourceName": SOURCE_NAME,
                "sourceUrl": SOURCE_URL,
            },
            "municipalities": {
                "sourceName": BKG_VG250_SOURCE_NAME,
                "sourceUrl": BKG_VG250_SOURCE_URL,
                "path": str(municipalities_path),
            },
            "pipelineVersion": PIPELINE_VERSION,
            "processedAt": now_iso(),
        },
    }
    report_path.parent.mkdir(parents=True, exist_ok=True)
    write_json(report_path, report)
    metrics_path = metrics_output_path or report_path.with_name(f"{report_path.stem}_metrics.json")
    metrics_path.parent.mkdir(parents=True, exist_ok=True)
    write_json(metrics_path, cleaning_metrics(report))

    print(f"Wrote {len(kept)} cleaned wind turbines to {output_path}")
    print(f"Wrote cleaning report with {len(excluded)} exclusions to {report_path}")
    print(f"Wrote cleaning metrics to {metrics_path}")
    return 0 if kept else 2


def repair(
    input_path: Path,
    municipalities_path: Path,
    output_path: Path,
    report_path: Path,
    metrics_output_path: Path | None,
    boundary_tolerance_km: float,
) -> int:
    municipalities = load_municipalities(municipalities_path)
    if not municipalities:
        print(f"ERROR: No municipality polygons found in {municipalities_path}", file=sys.stderr)
        return 2
    spatial_index = build_spatial_index(municipalities)

    seen_by_id: dict[str, dict[str, Any]] = {}
    kept: list[dict[str, Any]] = []
    repaired: list[dict[str, Any]] = []
    excluded: list[dict[str, Any]] = []
    warnings: list[dict[str, Any]] = []
    duplicate_bit_equal_count = 0
    unchanged_count = 0

    input_count = 0
    for turbine in read_jsonl(input_path):
        input_count += 1
        reason = coordinate_turbine_error(turbine)
        if reason:
            excluded.append(repair_exclusion(turbine, reason))
            continue

        turbine_id = turbine["id"]
        previous = seen_by_id.get(turbine_id)
        if previous is not None:
            if canonical_json(previous) == canonical_json(turbine):
                duplicate_bit_equal_count += 1
            else:
                excluded.append(repair_exclusion(turbine, "duplicate_turbine_id_conflict"))
            continue
        seen_by_id[turbine_id] = deepcopy(turbine)

        lat = turbine["latitude"]
        lon = turbine["longitude"]
        original_municipality_id = normalize_municipality_id(turbine.get("municipalityId"))
        original_municipality_name = as_text(turbine.get("municipalityName"))
        candidates = municipalities_containing_point(municipalities, spatial_index, lon, lat)
        matched = first_matching_municipality(candidates, original_municipality_id) if original_municipality_id else None

        if matched is not None:
            turbine["municipalityId"] = original_municipality_id
            turbine["municipalityName"] = original_municipality_name or matched["name"]
            kept.append(turbine)
            unchanged_count += 1
            continue

        detected = candidates[0] if len(candidates) == 1 else None
        if detected is not None:
            expected = municipalities.get(original_municipality_id) if original_municipality_id else None
            if expected is not None:
                distance_km = distance_to_geometry_km(lon, lat, expected["geometry"])
                if distance_km > MAX_REPAIR_DISTANCE_KM:
                    excluded.append(repair_exclusion(turbine, "coordinate_municipality_distance_exceeded"))
                    continue

            old_turbine = dict(turbine)
            apply_municipality_repair(turbine, detected)
            kept.append(turbine)
            repaired.append(
                repair_action(
                    "municipality_repaired_from_coordinate",
                    old_turbine,
                    turbine,
                    detected,
                )
            )
            continue

        expected = municipalities.get(original_municipality_id) if original_municipality_id else None
        if expected is not None:
            distance_km = distance_to_geometry_km(lon, lat, expected["geometry"])
            if distance_km <= boundary_tolerance_km:
                turbine["municipalityId"] = original_municipality_id
                turbine["municipalityName"] = original_municipality_name or expected["name"]
                kept.append(turbine)
                unchanged_count += 1
                warnings.append(
                    warning(
                        "municipality_boundary_ambiguous",
                        turbine,
                        expected,
                        distanceKm=round(distance_km, 3),
                    )
                )
                continue

        if not original_municipality_id and is_offshore_coordinate(lat, lon):
            old_turbine = dict(turbine)
            offshore = offshore_municipality(lon)
            apply_municipality_repair(turbine, offshore)
            kept.append(turbine)
            repaired.append(
                repair_action(
                    "offshore_pseudo_municipality_assigned",
                    old_turbine,
                    turbine,
                    offshore,
                )
            )
            continue

        if is_placeholder_coordinate(lat, lon):
            excluded.append(repair_exclusion(turbine, "placeholder_coordinates"))
            continue

        if len(candidates) > 1:
            excluded.append(repair_exclusion(turbine, "ambiguous_municipality_from_coordinate"))
        else:
            excluded.append(repair_exclusion(turbine, "coordinate_outside_municipality_reference"))

    warnings.extend(mixed_municipality_wind_park_warnings(kept))

    output_path.parent.mkdir(parents=True, exist_ok=True)
    with output_path.open("w", encoding="utf-8", newline="\n") as handle:
        for turbine in sorted(kept, key=lambda item: item["id"]):
            handle.write(json.dumps(turbine, sort_keys=True, ensure_ascii=True) + "\n")

    report = {
        "summary": {
            "inputCount": input_count,
            "keptCount": len(kept),
            "unchangedCount": unchanged_count,
            "repairedCount": len(repaired),
            "offshoreAssignedCount": count_by_code(repaired, "offshore_pseudo_municipality_assigned"),
            "excludedAfterRepairCount": len(excluded),
            "boundaryAmbiguousCount": count_by_code(warnings, "municipality_boundary_ambiguous"),
            "duplicateBitEqualCount": duplicate_bit_equal_count,
            "duplicateConflictCount": count_by_code(excluded, "duplicate_turbine_id_conflict"),
            "mixedMunicipalityWindParkCount": count_by_code(warnings, "mixed_municipality_wind_park"),
            "boundaryToleranceKm": boundary_tolerance_km,
        },
        "repaired": sorted(repaired, key=lambda item: (item["reasonCode"], item.get("turbineId") or "")),
        "excluded": sorted(excluded, key=lambda item: (item["reasonCode"], item.get("turbineId") or "")),
        "warnings": sorted(warnings, key=lambda item: (item["reasonCode"], item.get("turbineId") or item.get("windParkKey") or "")),
        "sources": {
            "mastr": {
                "sourceName": SOURCE_NAME,
                "sourceUrl": SOURCE_URL,
            },
            "municipalities": {
                "sourceName": BKG_VG250_SOURCE_NAME,
                "sourceUrl": BKG_VG250_SOURCE_URL,
                "path": str(municipalities_path),
            },
            "pipelineVersion": PIPELINE_VERSION,
            "processedAt": now_iso(),
        },
    }
    report_path.parent.mkdir(parents=True, exist_ok=True)
    write_json(report_path, report)
    metrics_path = metrics_output_path or report_path.with_name(f"{report_path.stem}_metrics.json")
    metrics_path.parent.mkdir(parents=True, exist_ok=True)
    write_json(metrics_path, repair_metrics(report))

    print(f"Wrote {len(kept)} repaired wind turbines to {output_path}")
    print(f"Wrote repair report with {len(repaired)} repairs and {len(excluded)} exclusions to {report_path}")
    print(f"Wrote repair metrics to {metrics_path}")
    return 0 if kept else 2


def aggregate(input_path: Path, output_path: Path) -> int:
    turbines = list(read_jsonl(input_path))
    parks_by_key: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for turbine in turbines:
        parks_by_key[park_group_key(turbine)].append(turbine)

    parks = []
    for key, group in sorted(parks_by_key.items()):
        lat = sum(t["latitude"] for t in group) / len(group)
        lon = sum(t["longitude"] for t in group) / len(group)
        capacity = sum(t.get("installedCapacityKw") or 0 for t in group) or None
        municipality_id, municipality_name = representative_municipality(group)
        wind_park_name = first_present(group, "windParkName") or f"Windpark {municipality_name}"
        park_id = first_present(group, "windParkId")
        if not park_id:
            park_id = "wp_" + stable_hash(key)[:12]
        parks.append(
            {
                "id": park_id,
                "name": wind_park_name,
                "municipalityId": municipality_id,
                "municipalityName": municipality_name,
                "latitude": round(lat, 6),
                "longitude": round(lon, 6),
                "turbineCount": len(group),
                "installedCapacityKw": capacity,
                "turbineIds": sorted(t["id"] for t in group),
                "groupingMethod": grouping_method(group),
                "sourceName": SOURCE_NAME,
                "sourceUrl": SOURCE_URL,
                "sourceUpdatedAt": today(),
                "dataQuality": "derived",
            }
        )

    output_path.parent.mkdir(parents=True, exist_ok=True)
    write_json(output_path, parks)
    print(f"Wrote {len(parks)} wind park aggregates to {output_path}")
    return 0 if parks else 2


def calculate(
    turbines_path: Path,
    parks_path: Path,
    output_path: Path,
    mastr_export_date: str,
    quality_report_path: Path | None = None,
) -> int:
    turbines = list(read_jsonl(turbines_path))
    parks = json.loads(parks_path.read_text(encoding="utf-8"))
    park_by_id = {park["id"]: park for park in parks}
    for turbine in turbines:
        if not turbine.get("windParkId") or turbine["windParkId"] not in park_by_id:
            turbine["windParkId"] = find_park_for_turbine(turbine, parks)

    snapshot = build_snapshot(turbines, parks, mastr_export_date, quality_report=read_optional_quality_report(quality_report_path))
    output_path.parent.mkdir(parents=True, exist_ok=True)
    write_json(output_path, snapshot)
    print(f"Wrote app snapshot to {output_path}")
    return validate(output_path)


def validate(snapshot_path: Path) -> int:
    snapshot = json.loads(snapshot_path.read_text(encoding="utf-8"))
    errors = validate_snapshot(snapshot)
    if errors:
        for error in errors:
            print(f"ERROR: {error}", file=sys.stderr)
        return 1
    print(f"Snapshot valid: {snapshot_path}")
    return 0


def write_smoke_snapshot(output_path: Path) -> int:
    turbines = [
        {
            "id": "mastr_wind_leipzig_001",
            "windParkId": "wp_leipzig_smoke",
            "name": "Windanlage Leipzig Smoke 1",
            "municipalityId": "14713000",
            "municipalityName": "Leipzig",
            "latitude": 51.3397,
            "longitude": 12.3731,
            "installedCapacityKw": 3500,
            "status": "in_operation",
            "turbineType": "Wind",
            "manufacturer": "Demo Hersteller",
            "model": "WK-3500",
            "hubHeightM": 135.0,
            "rotorDiameterM": 120.0,
            "sourceName": SOURCE_NAME,
            "sourceUrl": SOURCE_URL,
            "sourceUpdatedAt": "2026-06-18",
            "dataQuality": "official",
        },
        {
            "id": "mastr_wind_leipzig_002",
            "windParkId": "wp_leipzig_smoke",
            "name": "Windanlage Leipzig Smoke 2",
            "municipalityId": "14713000",
            "municipalityName": "Leipzig",
            "latitude": 51.3521,
            "longitude": 12.4012,
            "installedCapacityKw": 3500,
            "status": "in_operation",
            "turbineType": "Wind",
            "manufacturer": "Demo Hersteller",
            "model": "WK-3500",
            "hubHeightM": 135.0,
            "rotorDiameterM": 120.0,
            "sourceName": SOURCE_NAME,
            "sourceUrl": SOURCE_URL,
            "sourceUpdatedAt": "2026-06-18",
            "dataQuality": "official",
        },
    ]
    parks = [
        {
            "id": "wp_leipzig_smoke",
            "name": "Windpark Leipzig Smoke",
            "municipalityId": "14713000",
            "municipalityName": "Leipzig",
            "latitude": 51.3459,
            "longitude": 12.38715,
            "turbineCount": 2,
            "installedCapacityKw": 7000,
            "turbineIds": ["mastr_wind_leipzig_001", "mastr_wind_leipzig_002"],
            "groupingMethod": "smoke_fixture",
            "sourceName": SOURCE_NAME,
            "sourceUrl": SOURCE_URL,
            "sourceUpdatedAt": "2026-06-18",
            "dataQuality": "derived",
        }
    ]
    output_path.parent.mkdir(parents=True, exist_ok=True)
    write_json(output_path, build_snapshot(turbines, parks, "2026-06-18", snapshot_id="windklar-smoke-2026-06-18"))
    print(f"Wrote smoke snapshot to {output_path}")
    return validate(output_path)


def build_snapshot(
    turbines: list[dict[str, Any]],
    parks: list[dict[str, Any]],
    mastr_export_date: str,
    snapshot_id: str | None = None,
    quality_report: dict[str, Any] | None = None,
) -> dict[str, Any]:
    assumptions = [
        {"id": key, **value}
        for key, value in sorted(DEFAULT_ASSUMPTIONS.items())
    ]
    metrics = build_metrics(parks)
    limitations = [
        "Die Gruppierung von Windparks beruht auf einer algorithmischen Zuordnung bei der Vorverarbeitung.",
        "Die berechneten Kennzahlen zur Klimawirkung sind Schätzwerte des MVP und keine offiziellen Messdaten.",
    ]
    limitations.extend(quality_report_limitations(quality_report))

    snapshot = {
        "schemaVersion": "1",
        "snapshotMetadata": {
            "snapshotId": snapshot_id or f"windklar-{mastr_export_date}",
            "sourceName": SOURCE_NAME,
            "sourceUrl": SOURCE_URL,
            "attribution": ATTRIBUTION,
            "mastrExportDate": mastr_export_date,
            "processedAt": now_iso(),
            "pipelineVersion": PIPELINE_VERSION,
            "checksumSha256": "",
            "limitations": limitations,
        },
        "assumptions": assumptions,
        "windTurbines": sorted((snapshot_turbine(turbine) for turbine in turbines), key=lambda item: item["id"]),
        "windParks": sorted(parks, key=lambda item: item["id"]),
        "metrics": sorted(metrics, key=lambda item: item["id"]),
    }
    snapshot["snapshotMetadata"]["checksumSha256"] = snapshot_checksum(snapshot)
    return snapshot


def build_metrics(parks: list[dict[str, Any]]) -> list[dict[str, Any]]:
    full_load_hours = DEFAULT_ASSUMPTIONS["full_load_hours"]["value"]
    emission_factor = DEFAULT_ASSUMPTIONS["emission_factor_kg_per_kwh"]["value"]
    household_consumption = DEFAULT_ASSUMPTIONS["household_consumption_kwh"]["value"]
    municipal_rate = DEFAULT_ASSUMPTIONS["municipal_benefit_eur_per_kwh"]["value"]
    metrics: list[dict[str, Any]] = []
    for park in parks:
        capacity = park.get("installedCapacityKw") or 0
        annual_kwh = capacity * full_load_hours
        metrics.extend(
            [
                metric(park["id"], "annual_production", annual_kwh, "kWh/a", "Installierte Leistung multipliziert mit den angenommenen Volllaststunden."),
                metric(park["id"], "co2_savings", annual_kwh * emission_factor, "kg CO2/a", "Geschätzte Produktion multipliziert mit dem angenommenen CO₂-Vermeidungsfaktor."),
                metric(park["id"], "household_equivalent", annual_kwh / household_consumption, "households", "Geschätzte Produktion geteilt durch den angenommenen jährlichen Haushaltsstrombedarf."),
                metric(park["id"], "municipal_participation", annual_kwh * municipal_rate, "EUR/a", "Schätzung nach § 6 EEG mit 0,2 ct/kWh; keine bestätigte Auszahlung."),
            ]
        )
    return metrics


def metric(park_id: str, metric_type: str, value: float, unit: str, note: str) -> dict[str, Any]:
    return {
        "id": f"metric_{park_id}_{metric_type}",
        "subjectType": "wind_park",
        "subjectId": park_id,
        "metricType": metric_type,
        "value": round(value, 3),
        "unit": unit,
        "period": "year",
        "sourceName": "WindKlar MVP-Berechnung",
        "sourceUrl": SOURCE_URL,
        "sourceUpdatedAt": today(),
        "dataQuality": "estimated",
        "calculationNote": note,
    }


def iter_source_rows(input_path: Path) -> Iterable[dict[str, Any]]:
    suffix = input_path.suffix.lower()
    if suffix == ".csv":
        with input_path.open("r", encoding="utf-8-sig", newline="") as handle:
            yield from csv.DictReader(handle)
    elif suffix in {".json", ".geojson"}:
        data = json.loads(input_path.read_text(encoding="utf-8"))
        rows = data.get("features", data) if isinstance(data, dict) else data
        for row in rows:
            if isinstance(row, dict) and "properties" in row:
                merged = dict(row["properties"])
                if row.get("geometry", {}).get("type") == "Point":
                    lon, lat = row["geometry"]["coordinates"][:2]
                    merged.setdefault("longitude", lon)
                    merged.setdefault("latitude", lat)
                yield merged
            else:
                yield row
    elif suffix == ".xml":
        yield from iter_xml_rows(input_path)
    elif suffix == ".zip":
        with zipfile.ZipFile(input_path) as archive:
            for name in archive.namelist():
                lower_name = name.lower()
                if lower_name.endswith(".csv"):
                    with archive.open(name) as zipped_file:
                        text = text_wrapper(zipped_file)
                        yield from csv.DictReader(text)
                elif lower_name.endswith(".xml"):
                    with archive.open(name) as zipped_file:
                        yield from iter_xml_rows(zipped_file)
    else:
        raise ValueError(f"Unsupported input type: {input_path}")


def iter_xml_rows(input_source: Any) -> Iterable[dict[str, Any]]:
    for event, elem in ET.iterparse(input_source, events=("end",)):
        children = list(elem)
        if not children:
            elem.clear()
            continue
        row = {local_name(child.tag): (child.text or "").strip() for child in children}
        text = " ".join(str(value) for value in row.values()).lower()
        if "wind" in text:
            yield row
        elem.clear()


def normalize_row(row: dict[str, Any]) -> dict[str, Any] | None:
    normalized_keys = {clean_key(key): value for key, value in row.items()}
    if not looks_like_wind(normalized_keys):
        return None
    lat = parse_float(pick(normalized_keys, "latitude"))
    lon = parse_float(pick(normalized_keys, "longitude"))
    if lat is None or lon is None or not in_germany(lat, lon):
        return None
    turbine_id = as_text(pick(normalized_keys, "id")) or "wt_" + stable_hash(json.dumps(row, sort_keys=True))[:12]
    municipality_name = as_text(pick(normalized_keys, "municipalityName")) or "Unbekannte Gemeinde"
    municipality_id = as_text(pick(normalized_keys, "municipalityId")) or "unknown"
    return {
        "id": turbine_id,
        "windParkId": as_text(pick(normalized_keys, "windParkId")),
        "windParkName": as_text(pick(normalized_keys, "windParkName")),
        "name": as_text(pick(normalized_keys, "name")) or turbine_id,
        "municipalityId": municipality_id,
        "municipalityName": municipality_name,
        "latitude": lat,
        "longitude": lon,
        "installedCapacityKw": parse_int(pick(normalized_keys, "installedCapacityKw")),
        "status": as_text(pick(normalized_keys, "status")),
        "turbineType": as_text(pick(normalized_keys, "turbineType")) or "Wind",
        "manufacturer": as_text(pick(normalized_keys, "manufacturer")),
        "model": as_text(pick(normalized_keys, "model")),
        "hubHeightM": parse_float(pick(normalized_keys, "hubHeightM")),
        "rotorDiameterM": parse_float(pick(normalized_keys, "rotorDiameterM")),
        "sourceName": SOURCE_NAME,
        "sourceUrl": SOURCE_URL,
        "sourceUpdatedAt": today(),
        "dataQuality": "official",
    }


def load_municipalities(path: Path) -> dict[str, dict[str, Any]]:
    data = json.loads(path.read_text(encoding="utf-8"))
    features = data.get("features", []) if isinstance(data, dict) else []
    municipalities: dict[str, dict[str, Any]] = {}
    for feature in features:
        if not isinstance(feature, dict):
            continue
        geometry = feature.get("geometry")
        if not geometry or geometry.get("type") not in {"Polygon", "MultiPolygon"}:
            continue
        properties = feature.get("properties") or {}
        normalized = {clean_key(str(key)): value for key, value in properties.items()}
        municipality_id = normalize_municipality_id(pick_municipality_field(normalized, "id"))
        if municipality_id is None:
            continue
        municipality = {
            "id": municipality_id,
            "name": as_text(pick_municipality_field(normalized, "name")) or municipality_id,
            "geometry": geometry,
            "bbox": geometry_bbox(geometry),
        }
        municipalities.setdefault(municipality_id, municipality)
    return municipalities


def basic_turbine_error(turbine: dict[str, Any]) -> str | None:
    for key in ["id", "latitude", "longitude", "municipalityId", "municipalityName"]:
        if turbine.get(key) in (None, ""):
            return f"missing_{key}"
    lat = parse_float(turbine.get("latitude"))
    lon = parse_float(turbine.get("longitude"))
    if lat is None or lon is None:
        return "invalid_coordinates"
    turbine["latitude"] = lat
    turbine["longitude"] = lon
    if not in_germany(lat, lon):
        return "coordinates_outside_germany_bounds"
    if normalize_municipality_id(turbine.get("municipalityId")) is None:
        return "invalid_municipality_id"
    return None


def coordinate_turbine_error(turbine: dict[str, Any]) -> str | None:
    for key in ["id", "latitude", "longitude"]:
        if turbine.get(key) in (None, ""):
            return f"missing_{key}"
    lat = parse_float(turbine.get("latitude"))
    lon = parse_float(turbine.get("longitude"))
    if lat is None or lon is None:
        return "invalid_coordinates"
    turbine["latitude"] = lat
    turbine["longitude"] = lon
    if not in_germany(lat, lon):
        return "coordinates_outside_germany_bounds"
    return None


def normalize_municipality_id(value: Any) -> str | None:
    text = as_text(value)
    if text is None:
        return None
    digits = "".join(char for char in text if char.isdigit())
    if len(digits) == 8:
        return digits
    if 1 <= len(digits) < 8:
        return digits.zfill(8)
    if len(digits) > 8:
        return digits[:8]
    return None


def apply_municipality_repair(turbine: dict[str, Any], municipality: dict[str, Any]) -> None:
    turbine["municipalityId"] = municipality["id"]
    turbine["municipalityName"] = municipality["name"]
    turbine["dataQuality"] = "derived"


def is_offshore_coordinate(lat: float, lon: float) -> bool:
    return lat >= OFFSHORE_MIN_LAT and in_germany(lat, lon)


def offshore_municipality(lon: float) -> dict[str, Any]:
    if lon < OFFSHORE_NORTH_SEA_MAX_LON:
        return {"id": OFFSHORE_NORTH_SEA_ID, "name": OFFSHORE_NORTH_SEA_NAME}
    return {"id": OFFSHORE_BALTIC_SEA_ID, "name": OFFSHORE_BALTIC_SEA_NAME}


def is_placeholder_coordinate(lat: float, lon: float) -> bool:
    return coordinate_precision(lat) <= 1 and coordinate_precision(lon) <= 1


def coordinate_precision(value: float) -> int:
    text = f"{value:.10f}".rstrip("0").rstrip(".")
    if "." not in text:
        return 0
    return len(text.rsplit(".", 1)[1])


def pick_municipality_field(row: dict[str, Any], canonical: str) -> Any:
    for alias in MUNICIPALITY_FIELD_ALIASES[canonical]:
        if alias in row and row[alias] not in (None, ""):
            return row[alias]
    return None


def municipalities_containing_point(
    municipalities: dict[str, dict[str, Any]],
    spatial_index: dict[tuple[int, int], set[str]],
    lon: float,
    lat: float,
) -> list[dict[str, Any]]:
    matches = []
    for municipality_id in spatial_index.get(spatial_cell(lon, lat), set()):
        municipality = municipalities[municipality_id]
        if bbox_contains_point(municipality["bbox"], lon, lat) and point_in_geometry(lon, lat, municipality["geometry"]):
            matches.append(municipality)
    return matches


def build_spatial_index(municipalities: dict[str, dict[str, Any]]) -> dict[tuple[int, int], set[str]]:
    index: dict[tuple[int, int], set[str]] = defaultdict(set)
    for municipality_id, municipality in municipalities.items():
        min_lon, min_lat, max_lon, max_lat = municipality["bbox"]
        min_lon_cell, min_lat_cell = spatial_cell(min_lon, min_lat)
        max_lon_cell, max_lat_cell = spatial_cell(max_lon, max_lat)
        for lon_cell in range(min_lon_cell, max_lon_cell + 1):
            for lat_cell in range(min_lat_cell, max_lat_cell + 1):
                index[(lon_cell, lat_cell)].add(municipality_id)
    return index


def spatial_cell(lon: float, lat: float) -> tuple[int, int]:
    return int(lon / SPATIAL_INDEX_DEGREES), int(lat / SPATIAL_INDEX_DEGREES)


def first_matching_municipality(candidates: list[dict[str, Any]], municipality_id: str) -> dict[str, Any] | None:
    for candidate in candidates:
        if candidate["id"] == municipality_id:
            return candidate
    return None


def exclusion(
    turbine: dict[str, Any],
    reason_code: str,
    detected_municipality: dict[str, Any] | None = None,
) -> dict[str, Any]:
    return {
        "reasonCode": reason_code,
        "turbineId": turbine.get("id"),
        "originalMunicipalityId": turbine.get("municipalityId"),
        "originalMunicipalityName": turbine.get("municipalityName"),
        "detectedMunicipalityId": detected_municipality.get("id") if detected_municipality else None,
        "detectedMunicipalityName": detected_municipality.get("name") if detected_municipality else None,
        "latitude": turbine.get("latitude"),
        "longitude": turbine.get("longitude"),
    }


def repair_action(
    reason_code: str,
    original_turbine: dict[str, Any],
    repaired_turbine: dict[str, Any],
    detected_municipality: dict[str, Any],
) -> dict[str, Any]:
    return {
        "action": "repaired",
        "reasonCode": reason_code,
        "turbineId": repaired_turbine.get("id"),
        "originalMunicipalityId": original_turbine.get("municipalityId"),
        "originalMunicipalityName": original_turbine.get("municipalityName"),
        "newMunicipalityId": repaired_turbine.get("municipalityId"),
        "newMunicipalityName": repaired_turbine.get("municipalityName"),
        "detectedMunicipalityId": detected_municipality.get("id"),
        "detectedMunicipalityName": detected_municipality.get("name"),
        "latitude": repaired_turbine.get("latitude"),
        "longitude": repaired_turbine.get("longitude"),
        "dataQuality": repaired_turbine.get("dataQuality"),
    }


def repair_exclusion(turbine: dict[str, Any], reason_code: str) -> dict[str, Any]:
    return {
        "action": "excluded",
        "reasonCode": reason_code,
        "turbineId": turbine.get("id"),
        "originalMunicipalityId": turbine.get("municipalityId"),
        "originalMunicipalityName": turbine.get("municipalityName"),
        "latitude": turbine.get("latitude"),
        "longitude": turbine.get("longitude"),
    }


def warning(
    reason_code: str,
    turbine: dict[str, Any],
    municipality: dict[str, Any] | None = None,
    **extra: Any,
) -> dict[str, Any]:
    result = {
        "reasonCode": reason_code,
        "turbineId": turbine.get("id"),
        "municipalityId": turbine.get("municipalityId"),
        "municipalityName": turbine.get("municipalityName"),
        "referenceMunicipalityId": municipality.get("id") if municipality else None,
        "referenceMunicipalityName": municipality.get("name") if municipality else None,
        "latitude": turbine.get("latitude"),
        "longitude": turbine.get("longitude"),
    }
    result.update(extra)
    return result


def mixed_municipality_wind_park_warnings(turbines: list[dict[str, Any]]) -> list[dict[str, Any]]:
    warnings = []
    groups: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for turbine in turbines:
        groups[park_group_key(turbine)].append(turbine)
    for key, group in sorted(groups.items()):
        municipality_ids = {turbine.get("municipalityId") for turbine in group if turbine.get("municipalityId")}
        if len(municipality_ids) <= 1:
            continue
        representative_id, representative_name = representative_municipality(group)
        warnings.append(
            {
                "reasonCode": "mixed_municipality_wind_park",
                "windParkKey": key,
                "windParkId": first_present(group, "windParkId"),
                "windParkName": first_present(group, "windParkName"),
                "representativeMunicipalityId": representative_id,
                "representativeMunicipalityName": representative_name,
                "municipalityIds": sorted(municipality_ids),
                "turbineCount": len(group),
            }
        )
    return warnings


def representative_municipality(group: list[dict[str, Any]]) -> tuple[str, str]:
    by_municipality: dict[str, dict[str, Any]] = {}
    for turbine in group:
        municipality_id = turbine.get("municipalityId") or "unknown"
        entry = by_municipality.setdefault(
            municipality_id,
            {
                "id": municipality_id,
                "name": turbine.get("municipalityName") or "Unbekannte Gemeinde",
                "count": 0,
                "capacity": 0,
            },
        )
        entry["count"] += 1
        entry["capacity"] += turbine.get("installedCapacityKw") or 0
    representative = sorted(
        by_municipality.values(),
        key=lambda item: (-item["count"], -item["capacity"], item["id"]),
    )[0]
    return representative["id"], representative["name"]


def geometry_bbox(geometry: dict[str, Any]) -> tuple[float, float, float, float]:
    points = list(iter_geometry_points(geometry))
    lons = [point[0] for point in points]
    lats = [point[1] for point in points]
    return min(lons), min(lats), max(lons), max(lats)


def iter_geometry_points(geometry: dict[str, Any]) -> Iterable[tuple[float, float]]:
    for polygon in geometry_polygons(geometry):
        for ring in polygon:
            for coordinate in ring:
                if len(coordinate) >= 2:
                    yield float(coordinate[0]), float(coordinate[1])


def bbox_contains_point(bbox: tuple[float, float, float, float], lon: float, lat: float) -> bool:
    min_lon, min_lat, max_lon, max_lat = bbox
    return min_lon <= lon <= max_lon and min_lat <= lat <= max_lat


def point_in_geometry(lon: float, lat: float, geometry: dict[str, Any]) -> bool:
    for polygon in geometry_polygons(geometry):
        if point_in_polygon(lon, lat, polygon):
            return True
    return False


def point_in_polygon(lon: float, lat: float, polygon: list[list[list[float]]]) -> bool:
    if not polygon:
        return False
    if not point_in_ring(lon, lat, polygon[0]):
        return False
    for hole in polygon[1:]:
        if point_in_ring(lon, lat, hole):
            return False
    return True


def point_in_ring(lon: float, lat: float, ring: list[list[float]]) -> bool:
    inside = False
    count = len(ring)
    if count < 3:
        return False
    previous_lon, previous_lat = ring[-1][:2]
    for coordinate in ring:
        current_lon, current_lat = coordinate[:2]
        if point_on_segment(lon, lat, previous_lon, previous_lat, current_lon, current_lat):
            return True
        crosses = (current_lat > lat) != (previous_lat > lat)
        if crosses:
            intersection_lon = (previous_lon - current_lon) * (lat - current_lat) / (previous_lat - current_lat) + current_lon
            if lon < intersection_lon:
                inside = not inside
        previous_lon, previous_lat = current_lon, current_lat
    return inside


def point_on_segment(
    lon: float,
    lat: float,
    lon_a: float,
    lat_a: float,
    lon_b: float,
    lat_b: float,
    epsilon: float = 1e-10,
) -> bool:
    cross = (lat - lat_a) * (lon_b - lon_a) - (lon - lon_a) * (lat_b - lat_a)
    if abs(cross) > epsilon:
        return False
    return min(lon_a, lon_b) - epsilon <= lon <= max(lon_a, lon_b) + epsilon and min(lat_a, lat_b) - epsilon <= lat <= max(lat_a, lat_b) + epsilon


def distance_to_geometry_km(lon: float, lat: float, geometry: dict[str, Any]) -> float:
    if point_in_geometry(lon, lat, geometry):
        return 0.0
    distances = []
    for polygon in geometry_polygons(geometry):
        for ring in polygon:
            distances.append(distance_to_ring_km(lon, lat, ring))
    return min(distances) if distances else float("inf")


def distance_to_ring_km(lon: float, lat: float, ring: list[list[float]]) -> float:
    if len(ring) < 2:
        return float("inf")
    distances = []
    previous_lon, previous_lat = ring[-1][:2]
    for coordinate in ring:
        current_lon, current_lat = coordinate[:2]
        distances.append(point_to_segment_distance_km(lon, lat, previous_lon, previous_lat, current_lon, current_lat))
        previous_lon, previous_lat = current_lon, current_lat
    return min(distances)


def point_to_segment_distance_km(
    lon: float,
    lat: float,
    lon_a: float,
    lat_a: float,
    lon_b: float,
    lat_b: float,
) -> float:
    ref_lat = radians((lat + lat_a + lat_b) / 3)
    km_per_degree_lat = 111.32
    km_per_degree_lon = 111.32 * cos(ref_lat)
    px = lon * km_per_degree_lon
    py = lat * km_per_degree_lat
    ax = lon_a * km_per_degree_lon
    ay = lat_a * km_per_degree_lat
    bx = lon_b * km_per_degree_lon
    by = lat_b * km_per_degree_lat
    dx = bx - ax
    dy = by - ay
    if dx == 0 and dy == 0:
        return sqrt((px - ax) ** 2 + (py - ay) ** 2)
    t = ((px - ax) * dx + (py - ay) * dy) / (dx * dx + dy * dy)
    t = max(0.0, min(1.0, t))
    closest_x = ax + t * dx
    closest_y = ay + t * dy
    return sqrt((px - closest_x) ** 2 + (py - closest_y) ** 2)


def geometry_polygons(geometry: dict[str, Any]) -> list[list[list[list[float]]]]:
    geometry_type = geometry.get("type")
    coordinates = geometry.get("coordinates") or []
    if geometry_type == "Polygon":
        return [coordinates]
    if geometry_type == "MultiPolygon":
        return coordinates
    return []


def count_by_code(items: list[dict[str, Any]], reason_code: str) -> int:
    return sum(1 for item in items if item.get("reasonCode") == reason_code)


def cleaning_metrics(report: dict[str, Any]) -> dict[str, Any]:
    summary = report.get("summary") or {}
    input_count = summary.get("inputCount") or 0
    kept_count = summary.get("keptCount") or 0
    excluded_count = summary.get("excludedCount") or 0
    warnings = report.get("warnings") or []
    excluded = report.get("excluded") or []
    return {
        "inputCount": input_count,
        "keptCount": kept_count,
        "excludedCount": excluded_count,
        "keptRate": ratio(kept_count, input_count),
        "excludedRate": ratio(excluded_count, input_count),
        "boundaryAmbiguousCount": summary.get("boundaryAmbiguousCount") or 0,
        "duplicateBitEqualCount": summary.get("duplicateBitEqualCount") or 0,
        "duplicateConflictCount": summary.get("duplicateConflictCount") or 0,
        "mixedMunicipalityWindParkCount": summary.get("mixedMunicipalityWindParkCount") or 0,
        "boundaryToleranceKm": summary.get("boundaryToleranceKm"),
        "exclusionReasonCounts": reason_counts(excluded),
        "warningReasonCounts": reason_counts(warnings),
        "sources": report.get("sources") or {},
    }


def repair_metrics(report: dict[str, Any]) -> dict[str, Any]:
    summary = report.get("summary") or {}
    input_count = summary.get("inputCount") or 0
    kept_count = summary.get("keptCount") or 0
    excluded_count = summary.get("excludedAfterRepairCount") or 0
    repaired = report.get("repaired") or []
    excluded = report.get("excluded") or []
    warnings = report.get("warnings") or []
    return {
        "inputCount": input_count,
        "unchangedCount": summary.get("unchangedCount") or 0,
        "repairedCount": summary.get("repairedCount") or 0,
        "offshoreAssignedCount": summary.get("offshoreAssignedCount") or 0,
        "keptCount": kept_count,
        "excludedAfterRepairCount": excluded_count,
        "keptRateAfterRepair": ratio(kept_count, input_count),
        "excludedRateAfterRepair": ratio(excluded_count, input_count),
        "boundaryAmbiguousCount": summary.get("boundaryAmbiguousCount") or 0,
        "duplicateBitEqualCount": summary.get("duplicateBitEqualCount") or 0,
        "duplicateConflictCount": summary.get("duplicateConflictCount") or 0,
        "mixedMunicipalityWindParkCount": summary.get("mixedMunicipalityWindParkCount") or 0,
        "boundaryToleranceKm": summary.get("boundaryToleranceKm"),
        "repairActionCounts": reason_counts(repaired),
        "exclusionReasonCounts": reason_counts(excluded),
        "warningReasonCounts": reason_counts(warnings),
        "sources": report.get("sources") or {},
    }


def ratio(value: int | float, total: int | float) -> float:
    if not total:
        return 0.0
    return round(value / total, 6)


def reason_counts(items: list[dict[str, Any]]) -> dict[str, int]:
    counts: dict[str, int] = defaultdict(int)
    for item in items:
        reason = item.get("reasonCode") or "unknown"
        counts[reason] += 1
    return dict(sorted(counts.items()))


def canonical_json(value: dict[str, Any]) -> str:
    return json.dumps(value, sort_keys=True, ensure_ascii=True, separators=(",", ":"))


def read_optional_quality_report(path: Path | None) -> dict[str, Any] | None:
    if path is None:
        return None
    return json.loads(path.read_text(encoding="utf-8"))


def quality_report_limitations(report: dict[str, Any] | None) -> list[str]:
    if not report:
        return []
    summary = report.get("summary") or {}
    if "excludedAfterRepairCount" in summary:
        repaired_count = summary.get("repairedCount") or 0
        offshore_count = summary.get("offshoreAssignedCount") or 0
        excluded_count = summary.get("excludedAfterRepairCount") or 0
        limitations = []
        if repaired_count:
            limitations.append(
                f"{repaired_count} Windanlagen wurden bei der Vorverarbeitung aus Koordinaten- oder Offshore-Kontext abgeleitet repariert; davon {offshore_count} mit Offshore-Pseudo-Gemeinde."
            )
        if excluded_count:
            limitations.append(
                f"{excluded_count} Windanlagen wurden nach dem Reparaturversuch weiterhin aus dem MVP-Snapshot ausgeschlossen."
            )
        return limitations
    excluded_count = summary.get("excludedCount") or 0
    if excluded_count:
        return [
            f"{excluded_count} Windanlagen wurden wegen nicht plausibler Gemeinde-Koordinaten-Zuordnung aus dem MVP-Snapshot ausgeschlossen."
        ]
    return []


def validate_snapshot(snapshot: dict[str, Any]) -> list[str]:
    errors: list[str] = []
    required = ["schemaVersion", "snapshotMetadata", "assumptions", "windTurbines", "windParks", "metrics"]
    for key in required:
        if key not in snapshot:
            errors.append(f"Missing top-level key: {key}")
    if errors:
        return errors

    metadata = snapshot["snapshotMetadata"]
    for key in ["snapshotId", "sourceName", "sourceUrl", "attribution", "mastrExportDate", "processedAt", "pipelineVersion", "checksumSha256", "limitations"]:
        if not metadata.get(key):
            errors.append(f"Missing snapshotMetadata.{key}")

    turbines = snapshot["windTurbines"]
    parks = snapshot["windParks"]
    metrics = snapshot["metrics"]
    if not turbines:
        errors.append("Snapshot must contain at least one wind turbine")
    if not parks:
        errors.append("Snapshot must contain at least one wind park")

    turbine_ids = {item.get("id") for item in turbines}
    park_ids = {item.get("id") for item in parks}
    for turbine in turbines:
        check_quality(errors, "windTurbines", turbine)
        if turbine.get("dataQuality") not in {"official", "derived"}:
            errors.append(f"Wind turbine {turbine.get('id')} must be official or derived")
        if turbine.get("windParkId") not in park_ids:
            errors.append(f"Wind turbine {turbine.get('id')} references missing wind park {turbine.get('windParkId')}")
        if not in_germany(turbine.get("latitude"), turbine.get("longitude")):
            errors.append(f"Wind turbine {turbine.get('id')} has coordinates outside Germany bounds")
    for park in parks:
        check_quality(errors, "windParks", park)
        if park.get("dataQuality") != "derived":
            errors.append(f"Wind park {park.get('id')} must be derived")
        for turbine_id in park.get("turbineIds", []):
            if turbine_id not in turbine_ids:
                errors.append(f"Wind park {park.get('id')} references missing turbine {turbine_id}")
    for metric_item in metrics:
        check_quality(errors, "metrics", metric_item)
        if metric_item.get("subjectType") == "wind_park" and metric_item.get("subjectId") not in park_ids:
            errors.append(f"Metric {metric_item.get('id')} references missing wind park {metric_item.get('subjectId')}")
        if metric_item.get("dataQuality") not in {"estimated", "simulated", "measured", "missing"}:
            errors.append(f"Metric {metric_item.get('id')} has invalid metric quality {metric_item.get('dataQuality')}")
    expected_checksum = snapshot_checksum(snapshot)
    if metadata.get("checksumSha256") != expected_checksum:
        errors.append("snapshotMetadata.checksumSha256 does not match snapshot content")
    return errors


def check_quality(errors: list[str], collection: str, item: dict[str, Any]) -> None:
    if item.get("dataQuality") not in VALID_QUALITIES:
        errors.append(f"{collection} item {item.get('id')} has invalid dataQuality")


def pick(row: dict[str, Any], canonical: str) -> Any:
    for alias in FIELD_ALIASES[canonical]:
        if alias in row and row[alias] not in (None, ""):
            return row[alias]
    return None


def looks_like_wind(row: dict[str, Any]) -> bool:
    haystack = " ".join(str(value).lower() for value in row.values())
    return "wind" in haystack


def as_text(value: Any) -> str | None:
    if value is None:
        return None
    text = str(value).strip()
    return text or None


def parse_float(value: Any) -> float | None:
    text = as_text(value)
    if text is None:
        return None
    try:
        return float(text.replace(".", "").replace(",", ".") if "," in text else text)
    except ValueError:
        return None


def parse_int(value: Any) -> int | None:
    number = parse_float(value)
    return int(round(number)) if number is not None else None


def clean_key(key: str) -> str:
    return (
        key.strip()
        .replace("ä", "ae")
        .replace("ö", "oe")
        .replace("ü", "ue")
        .replace("Ä", "ae")
        .replace("Ö", "oe")
        .replace("Ü", "ue")
        .replace("ß", "ss")
        .replace("-", "_")
        .replace(" ", "_")
        .lower()
    )


def local_name(tag: str) -> str:
    return tag.rsplit("}", 1)[-1]


def park_group_key(turbine: dict[str, Any]) -> str:
    park_id = turbine.get("windParkId")
    if park_id:
        return f"source:{park_id}"
    park_name = turbine.get("windParkName")
    if park_name:
        return f"name:{turbine.get('municipalityId')}:{park_name.strip().lower()}"
    lat_bucket = round(float(turbine["latitude"]) * 10)
    lon_bucket = round(float(turbine["longitude"]) * 10)
    return f"fallback:{turbine.get('municipalityId')}:{lat_bucket}:{lon_bucket}"


def grouping_method(group: list[dict[str, Any]]) -> str:
    if first_present(group, "windParkId"):
        return "source_wind_park_id"
    if first_present(group, "windParkName"):
        return "wind_park_name_fallback"
    return "municipality_spatial_fallback"


def snapshot_turbine(turbine: dict[str, Any]) -> dict[str, Any]:
    result = dict(turbine)
    result.pop("windParkName", None)
    return result


def find_park_for_turbine(turbine: dict[str, Any], parks: list[dict[str, Any]]) -> str:
    for park in parks:
        if turbine["id"] in park.get("turbineIds", []):
            return park["id"]
    return parks[0]["id"]


def first_present(items: list[dict[str, Any]], key: str) -> Any:
    for item in items:
        if item.get(key):
            return item[key]
    return None


def filename_from_response(source_url: str, response: Any) -> str:
    disposition = response.headers.get("Content-Disposition", "")
    for part in disposition.split(";"):
        part = part.strip()
        if part.lower().startswith("filename="):
            return part.split("=", 1)[1].strip().strip('"')
    filename = source_url.rstrip("/").split("/")[-1]
    if filename and filename != "content":
        return filename
    return f"mastr_download_{today()}.zip"


def text_wrapper(binary_file: Any) -> Any:
    import io

    return io.TextIOWrapper(binary_file, encoding="utf-8-sig", newline="")


def in_germany(lat: Any, lon: Any) -> bool:
    if not isinstance(lat, (int, float)) or not isinstance(lon, (int, float)):
        return False
    return GERMANY_LAT_RANGE[0] <= lat <= GERMANY_LAT_RANGE[1] and GERMANY_LON_RANGE[0] <= lon <= GERMANY_LON_RANGE[1]


def stable_hash(value: str) -> str:
    return hashlib.sha1(value.encode("utf-8")).hexdigest()


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def snapshot_checksum(snapshot: dict[str, Any]) -> str:
    copy = deepcopy(snapshot)
    copy["snapshotMetadata"]["checksumSha256"] = ""
    payload = json.dumps(copy, sort_keys=True, ensure_ascii=True, separators=(",", ":"))
    return hashlib.sha256(payload.encode("utf-8")).hexdigest()


def read_jsonl(path: Path) -> Iterable[dict[str, Any]]:
    with path.open("r", encoding="utf-8") as handle:
        for line in handle:
            if line.strip():
                yield json.loads(line)


def write_json(path: Path, data: Any) -> None:
    path.write_text(
        json.dumps(data, indent=2, sort_keys=True, ensure_ascii=True) + "\n",
        encoding="utf-8",
        newline="\n",
    )


def today() -> str:
    return datetime.now(timezone.utc).date().isoformat()


def now_iso() -> str:
    return datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z")


if __name__ == "__main__":
    raise SystemExit(main())
