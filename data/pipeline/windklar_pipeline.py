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

PIPELINE_VERSION = "0.4.0"
SOURCE_NAME = "Marktstammdatenregister der Bundesnetzagentur"
SOURCE_URL = "https://www.marktstammdatenregister.de/MaStR/Datendownload"
ATTRIBUTION = "Quelle: Marktstammdatenregister der Bundesnetzagentur"
BKG_VG250_SOURCE_NAME = "BKG VG250 Verwaltungsgebiete"
BKG_VG250_SOURCE_URL = "https://gdz.bkg.bund.de/index.php/default/digitale-geodaten/verwaltungsgebiete.html"
VALID_QUALITIES = {"official", "measured", "derived", "estimated", "simulated", "missing"}
CATALOG_MAPPED_FIELDS = {
    "EinheitBetriebsstatus",
    "EinheitSystemstatus",
    "Energietraeger",
    "Hersteller",
    "Technologie",
    "WindAnLandOderAufSee",
}
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
PRODUCTION_ESTIMATE_YEAR = 2026
ANNUAL_WIND_TURBINE_DEGRADATION_RATE = 0.0063
MINIMUM_AGE_MULTIPLIER = 0.80

DEFAULT_ASSUMPTIONS = {
    "full_load_hours": {
        "label": "Angenommene jährliche Volllaststunden",
        "value": 2000.0,
        "unit": "h/a",
        "sourceName": "WindKlar MVP-Annahme mit Quellenabgleich",
        "sourceUrl": "https://journals.plos.org/plosone/article?id=10.1371/journal.pone.0211028",
        "sourceDate": "2026-06-18",
        "calculationNote": "Berechnung basiert auf lageabhängigen Volllaststunden der einzelnen Anlagen (Basis: Inland 1.700h, Küste 2.200h). Wenn das Inbetriebnahmejahr bekannt ist, wird ein kontinuierlicher Alterungsabschlag von 0,63% pro Betriebsjahr angesetzt und bei 80% gedeckelt.",
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
        "label": "Geschätzte kommunale Beteiligung für Windenergie an Land nach § 6 EEG",
        "value": 0.002,
        "unit": "EUR/kWh",
        "sourceName": "WindKlar MVP-Annahme",
        "sourceUrl": SOURCE_URL,
        "sourceDate": "2026-06-18",
        "calculationNote": "Schätzung für Windenergie an Land nach § 6 EEG mit 0,2 ct/kWh; keine bestätigte Auszahlung.",
    },
}

FIELD_ALIASES = {
    "id": ["id", "mastr_id", "einheitmastrnummer", "einheit_mastr_nummer", "mastrnummer"],
    "windParkId": ["windparkid", "wind_park_id", "windpark_id", "lokationmastrnummer"],
    "windParkName": ["windparkname", "wind_park_name", "namewindpark"],
    "name": ["name", "anlagenname", "einheitname", "einheitenname", "namestromerzeugungseinheit", "bezeichnung"],
    "municipalityId": ["gemeindeid", "municipalityid", "municipality_id", "ags", "gemeindeschluessel"],
    "municipalityName": ["gemeinde", "gemeindename", "municipality", "municipalityname", "ort"],
    "latitude": ["latitude", "lat", "breitengrad"],
    "longitude": ["longitude", "lon", "lng", "laengengrad", "langengrad"],
    "installedCapacityKw": ["installedcapacitykw", "nettonennleistung", "bruttonennleistung", "leistungkw"],
    "status": ["status", "betriebsstatus", "einheitbetriebsstatus"],
    "turbineType": ["turbinetype", "energietraeger", "energietrager", "technologie"],
    "manufacturer": ["manufacturer", "hersteller"],
    "model": ["model", "typ", "typenbezeichnung", "anlagenmodell"],
    "hubHeightM": ["hubheightm", "nabenhoehe", "nabenhohe"],
    "rotorDiameterM": ["rotordiameterm", "rotordurchmesser"],
    "commissioningDate": ["commissioningdate", "inbetriebnahmedatum", "inbetriebnahme"],
    "windAnLandOderAufSee": ["windanlandoderaufsee", "lage", "wind_an_land_oder_auf_see"],
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
    aggregate_parser.add_argument("--eps-km", type=float, default=1.5, help="Spatial clustering epsilon in km")

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
        return aggregate(Path(args.input), Path(args.output), args.eps_km)
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
        is_explicit_offshore = "auf see" in (turbine.get("windAnLandOderAufSee") or "").lower() or "offshore" in (turbine.get("windAnLandOderAufSee") or "").lower()
        if is_explicit_offshore:
            excluded.append(exclusion(turbine, "offshore_unit_excluded"))
            continue

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

    for turbine in kept:
        repair_physical_characteristics(turbine, warnings)

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
        is_explicit_offshore = "auf see" in (turbine.get("windAnLandOderAufSee") or "").lower() or "offshore" in (turbine.get("windAnLandOderAufSee") or "").lower()
        if is_explicit_offshore:
            excluded.append(repair_exclusion(turbine, "offshore_unit_excluded"))
            continue

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
            excluded.append(repair_exclusion(turbine, "offshore_coordinate_excluded"))
            continue

        if is_placeholder_coordinate(lat, lon):
            excluded.append(repair_exclusion(turbine, "placeholder_coordinates"))
            continue

        if len(candidates) > 1:
            excluded.append(repair_exclusion(turbine, "ambiguous_municipality_from_coordinate"))
        else:
            excluded.append(repair_exclusion(turbine, "coordinate_outside_municipality_reference"))

    for turbine in kept:
        repair_physical_characteristics(turbine, warnings)

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


def aggregate(input_path: Path, output_path: Path, eps_km: float = 1.5) -> int:
    turbines = list(read_jsonl(input_path))
    parks_by_key = group_turbines(turbines, eps_km)

    parks = []
    for key, group in sorted(parks_by_key.items()):
        lat = sum(t["latitude"] for t in group) / len(group)
        lon = sum(t["longitude"] for t in group) / len(group)
        capacity = sum(t.get("installedCapacityKw") or 0 for t in group) or None
        municipality_id, municipality_name = representative_municipality(group)
        wind_park_name = representative_park_name(group, f"Windpark {municipality_name}")
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
                "groupingMethod": grouping_method(group, key),
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
    # Load AGS district and state mapping
    ags_path = Path("data/raw/ags_landkreise.json")
    ags_map = {}
    if ags_path.exists():
        try:
            raw_ags = json.loads(ags_path.read_text(encoding="utf-8"))
            # Pad keys to 5 digits
            ags_map = {k.zfill(5): v for k, v in raw_ags.items()}
        except Exception as e:
            print(f"Warning: Failed to parse {ags_path}: {e}", file=sys.stderr)
    else:
        print(f"Warning: {ags_path} not found. Using empty mapping.", file=sys.stderr)

    STATE_NAMES = {
        "01": "Schleswig-Holstein",
        "02": "Hamburg",
        "03": "Niedersachsen",
        "04": "Bremen",
        "05": "Nordrhein-Westfalen",
        "06": "Hessen",
        "07": "Rheinland-Pfalz",
        "08": "Baden-Württemberg",
        "09": "Bayern",
        "10": "Saarland",
        "11": "Berlin",
        "12": "Brandenburg",
        "13": "Mecklenburg-Vorpommern",
        "14": "Sachsen",
        "15": "Sachsen-Anhalt",
        "16": "Thüringen"
    }

    def enrich_entity(item: dict[str, Any]) -> dict[str, Any]:
        enriched = dict(item)
        m_id = str(enriched.get("municipalityId", ""))
        if m_id.isdigit() and len(m_id) >= 5:
            dist_id = m_id[:5]
            state_id = m_id[:2]
            enriched["districtId"] = dist_id
            enriched["stateId"] = state_id
            
            # Resolve district name
            dist_entry = ags_map.get(dist_id)
            if dist_entry:
                name = dist_entry.get("name", "")
                if name.startswith("LK "):
                    enriched["districtName"] = "Landkreis " + name[3:]
                elif name.startswith("SK "):
                    enriched["districtName"] = "Stadt " + name[3:]
                else:
                    enriched["districtName"] = name
            else:
                enriched["districtName"] = f"Landkreis {dist_id}"
                
            # Resolve state name
            enriched["stateName"] = STATE_NAMES.get(state_id, "Deutschland")
        else:
            enriched["districtId"] = "unknown"
            enriched["districtName"] = "Unbekannter Landkreis"
            enriched["stateId"] = "unknown"
            enriched["stateName"] = "Unbekannt"
        return enriched

    assumptions = [
        {"id": key, **value}
        for key, value in sorted(DEFAULT_ASSUMPTIONS.items())
    ]
    metrics = build_metrics(parks, turbines)
    limitations = [
        "Die Gruppierung von Windparks beruht auf einer algorithmischen Zuordnung bei der Vorverarbeitung.",
        "Die berechneten Kennzahlen zur Klimawirkung sind Schätzwerte des MVP und keine offiziellen Messdaten.",
    ]
    limitations.extend(quality_report_limitations(quality_report))

    enriched_turbines = [enrich_entity(snapshot_turbine(turbine)) for turbine in turbines]
    enriched_parks = [enrich_entity(park) for park in parks]
    summaries = build_precomputed_summaries(enriched_parks, enriched_turbines, metrics)

    snapshot = {
        "schemaVersion": "2",
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
        "windTurbines": sorted(enriched_turbines, key=lambda item: item["id"]),
        "windParks": sorted(enriched_parks, key=lambda item: item["id"]),
        "metrics": sorted(metrics, key=lambda item: item["id"]),
        "parkOperationalSummaries": summaries["parkOperationalSummaries"],
        "regionSummaries": summaries["regionSummaries"],
        "mapSearchEntries": summaries["mapSearchEntries"],
        "nationalStatsSummary": summaries["nationalStatsSummary"],
    }
    snapshot["snapshotMetadata"]["checksumSha256"] = snapshot_checksum(snapshot)
    return snapshot


def build_metrics(parks: list[dict[str, Any]], turbines: list[dict[str, Any]]) -> list[dict[str, Any]]:
    # Map turbine ID to turbine dictionary for fast lookup
    turbine_by_id = {t["id"]: t for t in turbines}
    
    emission_factor = DEFAULT_ASSUMPTIONS["emission_factor_kg_per_kwh"]["value"]
    household_consumption = DEFAULT_ASSUMPTIONS["household_consumption_kwh"]["value"]
    municipal_rate = DEFAULT_ASSUMPTIONS["municipal_benefit_eur_per_kwh"]["value"]
    
    metrics: list[dict[str, Any]] = []
    for park in parks:
        park_turbines = [turbine_by_id[tid] for tid in park.get("turbineIds", []) if tid in turbine_by_id]

        # Calculate custom production for each turbine in the park
        annual_kwh = 0.0
        for t in park_turbines:
            capacity = t.get("installedCapacityKw") or 0

            # Determine base hours from location (municipalityId prefix)
            muni_id = t.get("municipalityId") or ""
            if muni_id.startswith(("01", "02", "03", "04", "13")):
                base_hours = 2200.0
            else:
                base_hours = 1700.0

            # Determine year factor from commissioningDate
            comm_date = t.get("commissioningDate")
            year = extract_year(comm_date)
            year_factor = wind_turbine_age_multiplier(year)

            annual_kwh += capacity * base_hours * year_factor

        note_annual = (
            "Geschätzte Jahresproduktion basierend auf lage- und altersabhängigen Volllaststunden der einzelnen Anlagen "
            "(Basis: Inland 1.700h, Küste 2.200h; Alterungsabschlag 0,63% pro Betriebsjahr, maximal 20%)."
        )

        metrics.extend(
            [
                metric(park["id"], "annual_production", annual_kwh, "kWh/a", note_annual),
                metric(park["id"], "co2_savings", annual_kwh * emission_factor, "kg CO2/a", "Geschätzte Produktion multipliziert mit dem angenommenen CO₂-Vermeidungsfaktor."),
                metric(park["id"], "household_equivalent", annual_kwh / household_consumption, "households", "Geschätzte Produktion geteilt durch den angenommenen jährlichen Haushaltsstrombedarf."),
            ]
        )
        metrics.append(
            metric(
                park["id"],
                "municipal_participation",
                annual_kwh * municipal_rate,
                "EUR/a",
                "Schätzung nach § 6 EEG für Windenergie an Land mit 0,2 ct/kWh; keine bestätigte Auszahlung.",
            )
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


def build_precomputed_summaries(
    parks: list[dict[str, Any]],
    turbines: list[dict[str, Any]],
    metrics: list[dict[str, Any]],
) -> dict[str, Any]:
    turbines_by_park: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for turbine in turbines:
        turbines_by_park[turbine["windParkId"]].append(turbine)

    metrics_by_park: dict[str, dict[str, float]] = defaultdict(dict)
    for item in metrics:
        if item.get("subjectType") == "wind_park":
            metrics_by_park[item["subjectId"]][item["metricType"]] = float(item.get("value") or 0.0)

    park_summaries = []
    visible_parks = []
    for park in parks:
        park_turbines = turbines_by_park.get(park["id"], [])
        status = park_status(park_turbines)
        valid_turbines = [t for t in park_turbines if "stillgelegt" not in str(t.get("status") or "").lower()]
        valid_capacity = sum(int(t.get("installedCapacityKw") or 0) for t in valid_turbines)
        valid_count = len(valid_turbines)
        park_summaries.append(
            {
                "windParkId": park["id"],
                "parkStatus": status,
                "validTurbineCount": valid_count,
                "validCapacityKw": valid_capacity,
            }
        )
        if status != "Stillgelegt":
            visible = dict(park)
            visible["turbineCount"] = valid_count
            visible["installedCapacityKw"] = valid_capacity
            visible_parks.append(visible)

    region_summaries = build_region_summaries(visible_parks, metrics_by_park)
    map_search_entries = build_map_search_entries(visible_parks, region_summaries)
    national_summary = build_national_stats_summary(visible_parks, turbines, metrics_by_park)

    return {
        "parkOperationalSummaries": sorted(park_summaries, key=lambda item: item["windParkId"]),
        "regionSummaries": sorted(
            region_summaries,
            key=lambda item: (item["regionType"], -item["installedCapacityKw"], item["name"], item["regionId"]),
        ),
        "mapSearchEntries": sorted(map_search_entries, key=lambda item: (item["typeRank"], item["sortName"], item["id"])),
        "nationalStatsSummary": national_summary,
    }


def park_status(turbines: list[dict[str, Any]]) -> str:
    statuses = [str(t.get("status") or "").lower() for t in turbines]
    if any("bau" in status or "errichtung" in status for status in statuses):
        return "Im Bau"
    if any(status == "" or "betrieb" in status or "aktiv" in status for status in statuses):
        return "Aktiv"
    if any("stillgelegt" in status for status in statuses):
        return "Stillgelegt"
    return "Geplant"


def build_region_summaries(
    parks: list[dict[str, Any]],
    metrics_by_park: dict[str, dict[str, float]],
) -> list[dict[str, Any]]:
    summaries = []
    for region_type, id_key, name_key, parent_keys in (
        ("city", "municipalityId", "municipalityName", ("districtName", "stateName")),
        ("district", "districtId", "districtName", ("stateName",)),
        ("state", "stateId", "stateName", ()),
    ):
        groups: dict[str, list[dict[str, Any]]] = defaultdict(list)
        for park in parks:
            region_id = str(park.get(id_key) or "").strip()
            if region_id:
                groups[region_id].append(park)

        for region_id, region_parks in groups.items():
            first = region_parks[0]
            capacity = sum(int(park.get("installedCapacityKw") or 0) for park in region_parks)
            turbines = sum(int(park.get("turbineCount") or 0) for park in region_parks)
            wind_park_count = len(region_parks)
            lat = sum(float(park["latitude"]) for park in region_parks) / wind_park_count
            lon = sum(float(park["longitude"]) for park in region_parks) / wind_park_count
            metric_sums = aggregate_metric_values(region_parks, metrics_by_park)
            parent_parts = [str(first.get(key) or "") for key in parent_keys if first.get(key)]
            context_label = representative_municipality_name(region_parks) if region_type == "district" else None
            summaries.append(
                {
                    "regionType": region_type,
                    "regionId": region_id,
                    "name": str(first.get(name_key) or region_id),
                    "contextLabel": context_label,
                    "parentName": ", ".join(parent_parts) or None,
                    "latitude": round(lat, 6),
                    "longitude": round(lon, 6),
                    "windParkCount": wind_park_count,
                    "turbineCount": turbines,
                    "installedCapacityKw": capacity,
                    "annualProductionKwh": metric_sums["annual_production"],
                    "co2SavingsKg": metric_sums["co2_savings"],
                    "householdEquivalent": metric_sums["household_equivalent"],
                    "municipalBenefitEur": metric_sums["municipal_participation"],
                }
            )
    return summaries


def aggregate_metric_values(
    parks: list[dict[str, Any]],
    metrics_by_park: dict[str, dict[str, float]],
) -> dict[str, float]:
    values = {
        "annual_production": 0.0,
        "co2_savings": 0.0,
        "household_equivalent": 0.0,
        "municipal_participation": 0.0,
    }
    for park in parks:
        metrics = metrics_by_park.get(park["id"], {})
        for key in values:
            values[key] += metrics.get(key, 0.0)
    return {key: round(value, 3) for key, value in values.items()}


def representative_municipality_name(parks: list[dict[str, Any]]) -> str | None:
    by_name: dict[str, int] = defaultdict(int)
    for park in parks:
        by_name[str(park.get("municipalityName") or "")] += int(park.get("installedCapacityKw") or 0)
    ranked = sorted(((capacity, name) for name, capacity in by_name.items() if name), reverse=True)
    return ranked[0][1] if ranked else None


def build_map_search_entries(
    parks: list[dict[str, Any]],
    region_summaries: list[dict[str, Any]],
) -> list[dict[str, Any]]:
    entries = []
    region_rank = {"state": 0, "district": 1, "city": 2}
    for summary in region_summaries:
        region_type = summary["regionType"]
        if region_type == "city" and is_redundant_municipality(summary.get("parentName") or "", summary["name"]):
            continue
        description = {
            "state": "Bundesland",
            "district": f"Landkreis in {summary.get('parentName') or 'Deutschland'}",
            "city": f"Gemeinde in {summary.get('parentName') or 'Deutschland'}",
        }.get(region_type, "Region")
        entries.append(
            map_search_entry(
                entry_id=f"{region_type}:{summary['regionId']}",
                result_type=region_type,
                target_id=summary["regionId"],
                label=summary["name"],
                description=description,
                latitude=summary["latitude"],
                longitude=summary["longitude"],
                type_rank=region_rank[region_type],
                fields=[
                    summary["regionId"],
                    summary["name"],
                    summary.get("contextLabel") or "",
                    summary.get("parentName") or "",
                ],
            )
        )

    for park in parks:
        entries.append(
            map_search_entry(
                entry_id=f"park:{park['id']}",
                result_type="park",
                target_id=park["id"],
                label=park["name"],
                description=f"{park['municipalityName']}, {park['stateName']}",
                latitude=park["latitude"],
                longitude=park["longitude"],
                type_rank=3,
                fields=[
                    park["id"],
                    park["name"],
                    park["municipalityName"],
                    park["districtName"],
                    park["stateName"],
                ],
            )
        )
    return entries


def map_search_entry(
    entry_id: str,
    result_type: str,
    target_id: str,
    label: str,
    description: str,
    latitude: float,
    longitude: float,
    type_rank: int,
    fields: list[str],
) -> dict[str, Any]:
    return {
        "id": entry_id,
        "resultType": result_type,
        "targetId": target_id,
        "label": label,
        "description": description,
        "latitude": round(float(latitude), 6),
        "longitude": round(float(longitude), 6),
        "typeRank": type_rank,
        "haystack": to_search_haystack(fields),
        "sortName": label,
    }


def to_search_haystack(fields: list[str]) -> str:
    return " ".join(normalize_search_text(field) for field in fields if field)


def normalize_search_text(value: Any) -> str:
    return str(value).strip().lower()


def is_redundant_municipality(district_name: str, municipality_name: str) -> bool:
    district = normalize_region_name(district_name)
    municipality = normalize_region_name(municipality_name)
    return bool(district) and district == municipality


def normalize_region_name(name: str) -> str:
    import re

    normalized = str(name).strip().lower()
    normalized = (
        normalized.replace("ä", "ae")
        .replace("ö", "oe")
        .replace("ü", "ue")
        .replace("ß", "ss")
    )
    normalized = re.sub(r"[^\w\s]", " ", normalized)
    normalized = re.sub(r"\s+", " ", normalized).strip()
    tokens = [
        "kreisfreie stadt",
        "kreisangehoerige stadt",
        "kreisangehörige stadt",
        "landkreis",
        "stadtkreis",
        "staedteregion",
        "staedte region",
        "regionalverband",
        "gemeinde",
        "stadt",
        "kreis",
    ]
    changed = True
    while changed:
        changed = False
        for token in tokens:
            if normalized == token:
                normalized = ""
                changed = True
                break
            if normalized.startswith(f"{token} "):
                normalized = normalized[len(token) + 1 :].strip()
                changed = True
                break
            if normalized.endswith(f" {token}"):
                normalized = normalized[: -(len(token) + 1)].strip()
                changed = True
                break
    return normalized


def build_national_stats_summary(
    parks: list[dict[str, Any]],
    turbines: list[dict[str, Any]],
    metrics_by_park: dict[str, dict[str, float]],
) -> dict[str, Any]:
    metric_sums = aggregate_metric_values(parks, metrics_by_park)
    capacities = [int(park.get("installedCapacityKw") or 0) for park in parks]
    active_turbines = [
        turbine
        for turbine in turbines
        if turbine.get("status") is None
        or "betrieb" in str(turbine.get("status") or "").lower()
        or "aktiv" in str(turbine.get("status") or "").lower()
    ]
    commissioning = commissioning_buckets(turbines)
    heights = height_buckets(turbines)
    return {
        "windParkCount": len(parks),
        "activeTurbineCount": len(active_turbines),
        "installedCapacityKw": sum(capacities),
        "annualProductionKwh": metric_sums["annual_production"],
        "co2SavingsKg": metric_sums["co2_savings"],
        "householdEquivalent": metric_sums["household_equivalent"],
        "municipalBenefitEur": metric_sums["municipal_participation"],
        "capacityClassLt5Mw": sum(1 for value in capacities if value < 5_000),
        "capacityClass5To20Mw": sum(1 for value in capacities if 5_000 <= value < 20_000),
        "capacityClass20To50Mw": sum(1 for value in capacities if 20_000 <= value < 50_000),
        "capacityClassGte50Mw": sum(1 for value in capacities if value >= 50_000),
        **commissioning,
        **heights,
    }


def commissioning_buckets(turbines: list[dict[str, Any]]) -> dict[str, int]:
    values = {
        "turbineCommissioningPre2000": 0,
        "turbineCommissioning2000To2009": 0,
        "turbineCommissioning2010To2019": 0,
        "turbineCommissioning2020Plus": 0,
        "turbineCommissioningUnknown": 0,
    }
    for turbine in turbines:
        year = turbine.get("commissioningYear")
        if year is None:
            values["turbineCommissioningUnknown"] += 1
        elif year < 2000:
            values["turbineCommissioningPre2000"] += 1
        elif year <= 2009:
            values["turbineCommissioning2000To2009"] += 1
        elif year <= 2019:
            values["turbineCommissioning2010To2019"] += 1
        else:
            values["turbineCommissioning2020Plus"] += 1
    return values


def height_buckets(turbines: list[dict[str, Any]]) -> dict[str, int]:
    values = {
        "turbineHeightLt80m": 0,
        "turbineHeight80To120m": 0,
        "turbineHeight120To160m": 0,
        "turbineHeightGte160m": 0,
        "turbineHeightUnknown": 0,
    }
    for turbine in turbines:
        height = turbine.get("hubHeightM")
        if height is None:
            values["turbineHeightUnknown"] += 1
        elif height < 80.0:
            values["turbineHeightLt80m"] += 1
        elif height < 120.0:
            values["turbineHeight80To120m"] += 1
        elif height < 160.0:
            values["turbineHeight120To160m"] += 1
        else:
            values["turbineHeightGte160m"] += 1
    return values


def wind_turbine_age_multiplier(commissioning_year: int | None) -> float:
    if commissioning_year is None:
        return 1.0
    operating_years = max(0, PRODUCTION_ESTIMATE_YEAR - commissioning_year)
    multiplier = 1.0 - operating_years * ANNUAL_WIND_TURBINE_DEGRADATION_RATE
    return max(MINIMUM_AGE_MULTIPLIER, multiplier)


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
            catalog_values = load_catalog_values(archive)
            for name in source_member_names(archive.namelist()):
                lower_name = name.lower()
                if lower_name.endswith(".csv"):
                    with archive.open(name) as zipped_file:
                        text = text_wrapper(zipped_file)
                        yield from csv.DictReader(text)
                elif lower_name.endswith(".xml"):
                    with archive.open(name) as zipped_file:
                        for row in iter_xml_rows(zipped_file):
                            yield apply_catalog_values(row, catalog_values)
    else:
        raise ValueError(f"Unsupported input type: {input_path}")


def source_member_names(names: list[str]) -> list[str]:
    supported = [
        name
        for name in names
        if not name.endswith("/") and name.lower().endswith((".csv", ".xml"))
    ]
    wind_units = [
        name
        for name in supported
        if name.rsplit("/", 1)[-1].lower() == "einheitenwind.xml"
    ]
    if wind_units:
        return wind_units
    wind_members = [
        name
        for name in supported
        if "wind" in name.rsplit("/", 1)[-1].lower()
    ]
    return wind_members or supported


def load_catalog_values(archive: zipfile.ZipFile) -> dict[str, str]:
    catalog_name = next(
        (name for name in archive.namelist() if name.rsplit("/", 1)[-1].lower() == "katalogwerte.xml"),
        None,
    )
    if catalog_name is None:
        return {}

    values: dict[str, str] = {}
    with archive.open(catalog_name) as handle:
        for event, elem in ET.iterparse(handle, events=("end",)):
            if local_name(elem.tag) != "Katalogwert":
                continue
            row = {local_name(child.tag): (child.text or "").strip() for child in list(elem)}
            catalog_id = row.get("Id")
            value = row.get("Wert")
            if catalog_id and value:
                values[catalog_id] = value
            elem.clear()
    return values


def apply_catalog_values(row: dict[str, Any], catalog_values: dict[str, str]) -> dict[str, Any]:
    if not catalog_values:
        return row
    resolved = dict(row)
    for key in CATALOG_MAPPED_FIELDS:
        value = as_text(resolved.get(key))
        if value and value in catalog_values:
            resolved[key] = catalog_values[value]
    return resolved


def iter_xml_rows(input_source: Any) -> Iterable[dict[str, Any]]:
    for event, elem in ET.iterparse(input_source, events=("end",)):
        children = list(elem)
        if not children:
            continue
        row = {local_name(child.tag): (child.text or "").strip() for child in children}
        text = " ".join(str(value) for value in row.values()).lower()
        if any(row.values()) and ("wind" in text or "wind" in local_name(elem.tag).lower()):
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
        "commissioningDate": as_text(pick(normalized_keys, "commissioningDate")),
        "sourceName": SOURCE_NAME,
        "sourceUrl": SOURCE_URL,
        "sourceUpdatedAt": today(),
        "dataQuality": "official",
        "windAnLandOderAufSee": as_text(pick(normalized_keys, "windAnLandOderAufSee")),
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


def repair_physical_characteristics(turbine: dict[str, Any], warnings: list[dict[str, Any]]) -> None:
    hub_height = turbine.get("hubHeightM")
    if hub_height is not None:
        if hub_height <= 0.0 or hub_height > 300.0:
            warnings.append(
                warning(
                    "invalid_hub_height",
                    turbine,
                    originalValue=hub_height,
                )
            )
            turbine["hubHeightM"] = None
            turbine["dataQuality"] = "derived"

    rotor_diameter = turbine.get("rotorDiameterM")
    if rotor_diameter is not None:
        if rotor_diameter <= 0.0 or rotor_diameter > 250.0:
            warnings.append(
                warning(
                    "invalid_rotor_diameter",
                    turbine,
                    originalValue=rotor_diameter,
                )
            )
            turbine["rotorDiameterM"] = None
            turbine["dataQuality"] = "derived"


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
    groups = group_turbines(turbines)
    for key, group in sorted(groups.items()):
        municipality_ids = {turbine.get("municipalityId") for turbine in group if turbine.get("municipalityId")}
        if len(municipality_ids) <= 1:
            continue
        representative_id, representative_name = representative_municipality(group)
        wind_park_id = first_present(group, "windParkId")
        if not wind_park_id:
            wind_park_id = "wp_" + stable_hash(key)[:12]
        wind_park_name = representative_park_name(group, f"Windpark {representative_name}")
        warnings.append(
            {
                "reasonCode": "mixed_municipality_wind_park",
                "windParkKey": key,
                "windParkId": wind_park_id,
                "windParkName": wind_park_name,
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
        excluded_count = summary.get("excludedAfterRepairCount") or 0
        limitations = []
        if repaired_count:
            limitations.append(
                f"{repaired_count} Windanlagen wurden bei der Vorverarbeitung aus Koordinaten-Kontext abgeleitet repariert."
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
    required = [
        "schemaVersion",
        "snapshotMetadata",
        "assumptions",
        "windTurbines",
        "windParks",
        "metrics",
        "parkOperationalSummaries",
        "regionSummaries",
        "mapSearchEntries",
        "nationalStatsSummary",
    ]
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
    park_summaries = snapshot["parkOperationalSummaries"]
    region_summaries = snapshot["regionSummaries"]
    search_entries = snapshot["mapSearchEntries"]
    national_summary = snapshot["nationalStatsSummary"]
    if not turbines:
        errors.append("Snapshot must contain at least one wind turbine")
    if not parks:
        errors.append("Snapshot must contain at least one wind park")
    if not metrics:
        errors.append("Snapshot must contain at least one metric")
    if not park_summaries:
        errors.append("Snapshot must contain park operational summaries")
    if not region_summaries:
        errors.append("Snapshot must contain region summaries")
    if not search_entries:
        errors.append("Snapshot must contain map search entries")
    if not isinstance(national_summary, dict):
        errors.append("Snapshot must contain nationalStatsSummary object")

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

    summary_park_ids = {item.get("windParkId") for item in park_summaries if isinstance(item, dict)}
    if summary_park_ids != park_ids:
        errors.append("parkOperationalSummaries must contain exactly one row for every wind park")
    valid_region_types = {"city", "district", "state"}
    present_region_types = {item.get("regionType") for item in region_summaries if isinstance(item, dict)}
    missing_region_types = valid_region_types - present_region_types
    if missing_region_types:
        errors.append(f"regionSummaries missing region types: {sorted(missing_region_types)}")
    for item in region_summaries:
        if item.get("regionType") not in valid_region_types:
            errors.append(f"Region summary {item.get('regionId')} has invalid regionType {item.get('regionType')}")
        for key in ("regionId", "name", "latitude", "longitude", "windParkCount", "turbineCount", "installedCapacityKw"):
            if item.get(key) in (None, ""):
                errors.append(f"Region summary {item.get('regionId')} missing {key}")
    present_search_types = {item.get("resultType") for item in search_entries if isinstance(item, dict)}
    missing_search_types = {"state", "district", "city", "park"} - present_search_types
    if missing_search_types:
        errors.append(f"mapSearchEntries missing result types: {sorted(missing_search_types)}")
    for item in search_entries:
        if item.get("resultType") == "park" and item.get("targetId") not in park_ids:
            errors.append(f"Map search entry {item.get('id')} references missing wind park {item.get('targetId')}")
        for key in ("id", "resultType", "targetId", "label", "haystack", "sortName"):
            if item.get(key) in (None, ""):
                errors.append(f"Map search entry {item.get('id')} missing {key}")
    if isinstance(national_summary, dict):
        if not isinstance(national_summary.get("windParkCount"), int) or national_summary.get("windParkCount") <= 0:
            errors.append("nationalStatsSummary.windParkCount must be positive")
        if national_summary.get("installedCapacityKw") is None:
            errors.append("nationalStatsSummary.installedCapacityKw is required")

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


def extract_year(date_str: Any) -> int | None:
    if not date_str:
        return None
    date_str = str(date_str).strip()
    import re
    match = re.search(r'\b(19\d{2}|20\d{2})\b', date_str)
    if match:
        return int(match.group(1))
    return None


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


def spatial_clustering(turbines: list[dict[str, Any]], eps_km: float = 2.0) -> list[list[dict[str, Any]]]:
    if not turbines:
        return []
    cell_size = eps_km / 111.32
    buckets = defaultdict(list)
    for t in turbines:
        lat = t["latitude"]
        lon = t["longitude"]
        lat_cell = int(lat / cell_size)
        lon_cell = int(lon / cell_size)
        buckets[(lat_cell, lon_cell)].append(t)
        
    def get_distance(t1, t2):
        lat1, lon1 = t1["latitude"], t1["longitude"]
        lat2, lon2 = t2["latitude"], t2["longitude"]
        ref_lat = radians((lat1 + lat2) / 2)
        dx = (lon1 - lon2) * 111.32 * cos(ref_lat)
        dy = (lat1 - lat2) * 111.32
        return sqrt(dx * dx + dy * dy)
        
    visited = set()
    clusters = []
    for t in turbines:
        t_id = t["id"]
        if t_id in visited:
            continue
        cluster = []
        queue = [t]
        visited.add(t_id)
        while queue:
            current = queue.pop(0)
            cluster.append(current)
            lat = current["latitude"]
            lon = current["longitude"]
            lat_cell = int(lat / cell_size)
            lon_cell = int(lon / cell_size)
            for d_lat in (-1, 0, 1):
                for d_lon in (-1, 0, 1):
                    neighbor_cell = (lat_cell + d_lat, lon_cell + d_lon)
                    for candidate in buckets[neighbor_cell]:
                        cand_id = candidate["id"]
                        if cand_id not in visited:
                            if get_distance(current, candidate) <= eps_km:
                                visited.add(cand_id)
                                queue.append(candidate)
        clusters.append(cluster)
    return clusters


def normalize_wind_park_name(name: str | None) -> str:
    if not name:
        return ""
    name = name.lower().strip()
    import re
    words_to_remove = ["windpark", "wp", "wka", "windkraftanlage", "windenergieanlage", "wea", "gmbh", "co", "kg", "mb-h", "mbh"]
    for w in words_to_remove:
        name = re.sub(rf'\b{w}\b', '', name)
    return "".join(c for c in name if c.isalnum())


def get_name_tokens(name: str | None) -> set[str]:
    if not name:
        return set()
    name = name.lower()
    for char in ["-", "_", ",", ".", "(", ")", "/"]:
        name = name.replace(char, " ")
    tokens = set(name.split())
    stopwords = {"windpark", "wp", "wea", "windenergie", "windenergieanlage", "buergerwindpark", "energiepark", "windfarm", "wind", "park", "gmbh", "co", "kg", "anlagen", "unbekannt"}
    return tokens - stopwords


def names_share_meaningful_token(name1: str | None, name2: str | None) -> bool:
    tokens1 = get_name_tokens(name1)
    tokens2 = get_name_tokens(name2)
    if not tokens1 or not tokens2:
        return False
    return len(tokens1 & tokens2) > 0


def names_conflict(name1: str | None, name2: str | None) -> bool:
    n1 = normalize_wind_park_name(name1)
    n2 = normalize_wind_park_name(name2)
    if not n1 or not n2:
        return False
    if len(n1) < 3 or len(n2) < 3:
        return False
    return n1 != n2


def group_turbines(turbines: list[dict[str, Any]], eps_km: float = 1.5) -> dict[str, list[dict[str, Any]]]:
    # Option B: Attribut-basiertes Clustering (MaStR + Name/Gemeinde)
    parent = {t["id"]: t["id"] for t in turbines}
    
    def find(i):
        path = []
        while parent[i] != i:
            path.append(i)
            i = parent[i]
        for node in path:
            parent[node] = i
        return i
        
    def union(i, j):
        root_i = find(i)
        root_j = find(j)
        if root_i != root_j:
            parent[root_i] = root_j
            
    # Stufe 1: Gruppierung nach windParkId
    pids = defaultdict(list)
    for t in turbines:
        pid = t.get("windParkId")
        if pid:
            pids[pid].append(t["id"])
            
    for pid, ids in pids.items():
        for i in range(1, len(ids)):
            union(ids[0], ids[i])
            
    # Stufe 2: Gruppierung nach windParkName + municipalityId
    name_mun_groups = defaultdict(list)
    for t in turbines:
        name = t.get("windParkName")
        mun = t.get("municipalityId")
        cname = normalize_wind_park_name(name)
        if cname and len(cname) >= 3 and mun and mun != "unknown":
            key = (cname, mun)
            name_mun_groups[key].append(t["id"])
            
    for key, ids in name_mun_groups.items():
        for i in range(1, len(ids)):
            union(ids[0], ids[i])
            
    # Zusammenbauen
    grouped_turbines = defaultdict(list)
    for t in turbines:
        root_id = find(t["id"])
        grouped_turbines[root_id].append(t)
        
    # Schlüsselgenerierung mit Typen-Präfix
    grouped = {}
    for root_id, cluster_turbines in grouped_turbines.items():
        sorted_ids = sorted(t["id"] for t in cluster_turbines)
        key_hash = stable_hash("_".join(sorted_ids))[:12]
        
        has_malo = any(t.get("windParkId") for t in cluster_turbines)
        if has_malo:
            prefix = "malo"
        elif len(cluster_turbines) > 1:
            prefix = "name_mun"
        else:
            prefix = "singleton"
            
        grouped[f"{prefix}:{key_hash}"] = cluster_turbines
        
    return grouped



def representative_park_name(group: list[dict[str, Any]], default_name: str) -> str:
    names = [t.get("windParkName").strip() for t in group if t.get("windParkName") and t.get("windParkName").strip()]
    if not names:
        return default_name
    from collections import Counter
    return Counter(names).most_common(1)[0][0]


def grouping_method(group: list[dict[str, Any]], key: str) -> str:
    if key.startswith("malo:"):
        return "malo_id_grouping"
    if key.startswith("name_mun:"):
        return "name_municipality_grouping"
    return "singleton_fallback"


def snapshot_turbine(turbine: dict[str, Any]) -> dict[str, Any]:
    result = dict(turbine)
    result.pop("windParkName", None)
    comm_date = result.pop("commissioningDate", None)
    result["commissioningYear"] = extract_year(comm_date)
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
