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
from pathlib import Path
from typing import Any, Iterable

PIPELINE_VERSION = "0.1.0"
SOURCE_NAME = "Marktstammdatenregister der Bundesnetzagentur"
SOURCE_URL = "https://www.marktstammdatenregister.de/MaStR/Datendownload"
ATTRIBUTION = "Quelle: Marktstammdatenregister der Bundesnetzagentur"
VALID_QUALITIES = {"official", "measured", "derived", "estimated", "simulated", "missing"}
GERMANY_LAT_RANGE = (47.0, 55.2)
GERMANY_LON_RANGE = (5.5, 15.5)

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


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="WindKlar MaStR snapshot pipeline")
    subparsers = parser.add_subparsers(dest="command", required=True)

    fetch_parser = subparsers.add_parser("fetch", help="Download an official MaStR export")
    fetch_parser.add_argument("--source-url", required=True)
    fetch_parser.add_argument("--output-dir", default="data/raw")

    normalize_parser = subparsers.add_parser("normalize", help="Normalize source rows to turbine JSONL")
    normalize_parser.add_argument("--input", required=True)
    normalize_parser.add_argument("--output", default="data/intermediate/wind_turbines.jsonl")

    aggregate_parser = subparsers.add_parser("aggregate", help="Build derived wind park aggregates")
    aggregate_parser.add_argument("--input", required=True)
    aggregate_parser.add_argument("--output", default="data/intermediate/wind_parks.json")

    calculate_parser = subparsers.add_parser("calculate", help="Build a complete app snapshot")
    calculate_parser.add_argument("--turbines", required=True)
    calculate_parser.add_argument("--parks", required=True)
    calculate_parser.add_argument("--output", required=True)
    calculate_parser.add_argument("--mastr-export-date", default=today())

    validate_parser = subparsers.add_parser("validate", help="Validate an app snapshot")
    validate_parser.add_argument("snapshot")

    smoke_parser = subparsers.add_parser("smoke", help="Write a tiny valid smoke snapshot")
    smoke_parser.add_argument("--output", default="data/snapshots/windklar_snapshot_smoke.json")

    args = parser.parse_args(argv)
    if args.command == "fetch":
        return fetch(args.source_url, Path(args.output_dir))
    if args.command == "normalize":
        return normalize(Path(args.input), Path(args.output))
    if args.command == "aggregate":
        return aggregate(Path(args.input), Path(args.output))
    if args.command == "calculate":
        return calculate(Path(args.turbines), Path(args.parks), Path(args.output), args.mastr_export_date)
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
        municipality_id = first_present(group, "municipalityId") or "unknown"
        municipality_name = first_present(group, "municipalityName") or "Unbekannte Gemeinde"
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


def calculate(turbines_path: Path, parks_path: Path, output_path: Path, mastr_export_date: str) -> int:
    turbines = list(read_jsonl(turbines_path))
    parks = json.loads(parks_path.read_text(encoding="utf-8"))
    park_by_id = {park["id"]: park for park in parks}
    for turbine in turbines:
        if not turbine.get("windParkId") or turbine["windParkId"] not in park_by_id:
            turbine["windParkId"] = find_park_for_turbine(turbine, parks)

    snapshot = build_snapshot(turbines, parks, mastr_export_date)
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
) -> dict[str, Any]:
    assumptions = [
        {"id": key, **value}
        for key, value in sorted(DEFAULT_ASSUMPTIONS.items())
    ]
    metrics = build_metrics(parks)
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
            "limitations": [
                "Die Gruppierung von Windparks beruht auf einer algorithmischen Zuordnung bei der Vorverarbeitung.",
                "Die berechneten Kennzahlen zur Klimawirkung sind Schätzwerte des MVP und keine offiziellen Messdaten.",
            ],
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
        if turbine.get("dataQuality") != "official":
            errors.append(f"Wind turbine {turbine.get('id')} must be official")
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
