#!/usr/bin/env python3
"""Seed default setups from CSVs into the f1setups database."""

from __future__ import annotations

import argparse
import csv
import re
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Dict, Iterable, List, Optional, Tuple

USER_ID = 1
TEAM_ID = 16
ANNOTATION = "Setup predeterminado"
CONTROLLER_TYPE = "GAMEPAD"
IS_WET_WEATHER = 0

CSV_FILES = [
    (2022, "F1 game setups (22-25) - F1 22.csv"),
    (2023, "F1 game setups (22-25) - F1 23.csv"),
    (2024, "F1 game setups (22-25) - F1 24.csv"),
    (2025, "F1 game setups (22-25) - F1 25 .csv"),
]

INSERT_COLUMNS = [
    "user_id",
    "game_version_id",
    "track_id",
    "team_id",
    "title",
    "annotation",
    "session_type",
    "controller_type",
    "is_wet_weather",
    "front_wing",
    "rear_wing",
    "diff_on_throttle",
    "diff_off_throttle",
    "engine_braking",
    "front_camber",
    "rear_camber",
    "front_toe",
    "rear_toe",
    "front_suspension",
    "rear_suspension",
    "front_anti_roll_bar",
    "rear_anti_roll_bar",
    "front_ride_height",
    "rear_ride_height",
    "brake_pressure",
    "brake_bias",
    "front_right_pressure",
    "front_left_pressure",
    "rear_right_pressure",
    "rear_left_pressure",
]

SKIP_CIRCUIT_KEYWORDS = [
    "faq",
    "legend",
    "alert",
    "tire",
    "temperatures",
    "wet-weather",
    "tips",
    "type of issue",
    "baseline",
    "do not",
    "setups are final",
    "rest in piss",
    "full wet",
    "intermediate",
]

ALIAS_MAP = {
    "bahrain": "bahrain gp",
    "saudi arabia": "saudi arabian gp",
    "japan": "japanese gp",
    "australia": "australian gp",
    "china": "chinese gp",
    "azerbaijan": "azerbaijan gp",
    "miami": "miami gp",
    "spain": "spanish gp",
    "monaco": "monaco gp",
    "united states": "united states gp",
    "canada": "canadian gp",
    "austria": "austrian gp",
    "great britain": "british gp",
    "britain": "british gp",
    "france": "french gp",
    "hungary": "hungarian gp",
    "belgium": "belgian gp",
    "netherlands": "dutch gp",
    "monza": "italian gp",
    "italy": "italian gp",
    "mexico": "mexican gp",
    "brazil": "brazilian gp",
    "las vegas": "las vegas gp",
    "singapore": "singapore gp",
    "qatar": "qatar gp",
    "abu dhabi": "abu dhabi gp",
    "imola": "imola gp",
}

TIRE_PRESSURE_BOUNDS = {
    2022: {"front": {"min": 22.5, "max": 25.0}, "rear": {"min": 20.5, "max": 23.0}},
    2023: {"front": {"min": 22.0, "max": 25.0}, "rear": {"min": 20.0, "max": 23.0}},
    2024: {"front": {"min": 22.5, "max": 29.5}, "rear": {"min": 20.5, "max": 26.5}},
    2025: {"front": {"min": 22.5, "max": 29.5}, "rear": {"min": 20.5, "max": 26.5}},
}

GEOMETRY_BOUNDS = {
    2022: {
        "front_camber": {"min": -3.5, "max": -2.5},
        "rear_camber": {"min": -2.0, "max": -1.0},
        "front_toe": {"min": 0.05, "max": 0.15},
        "rear_toe": {"min": 0.20, "max": 0.50},
    },
    2023: {
        "front_camber": {"min": -3.5, "max": -2.5},
        "rear_camber": {"min": -2.0, "max": -1.0},
        "front_toe": {"min": 0.00, "max": 0.10},
        "rear_toe": {"min": 0.10, "max": 0.30},
    },
    2024: {
        "front_camber": {"min": -3.5, "max": -2.5},
        "rear_camber": {"min": -2.2, "max": -0.70},
        "front_toe": {"min": 0.00, "max": 0.50},
        "rear_toe": {"min": 0.00, "max": 0.50},
    },
    2025: {
        "front_camber": {"min": -3.5, "max": -2.5},
        "rear_camber": {"min": -2.0, "max": -1.0},
        "front_toe": {"min": 0.00, "max": 0.20},
        "rear_toe": {"min": 0.10, "max": 0.25},
    },
}


@dataclass
class ParsedRow:
    game_year: int
    track_id: int
    track_name: str
    aero_q: Tuple[int, int]
    aero_r: Tuple[int, int]
    diff_q: List[int]
    diff_r: List[int]
    geometry: Tuple[float, float, float, float]
    suspension: Tuple[int, int, int, int, int, int]
    brakes_q: Tuple[int, int]
    brakes_r: Tuple[int, int]
    tires_q: Tuple[float, float]
    tires_r: Tuple[float, float]


def normalize_key(text: str) -> str:
    return re.sub(r"[^a-z0-9]+", "", text.strip().lower())


def parse_init_sql(init_sql_path: Path) -> Tuple[Dict[int, int], Dict[str, int]]:
    content = init_sql_path.read_text(encoding="utf-8")

    game_match = re.search(r"INSERT INTO game .*?VALUES\s*(.+?);", content, re.S | re.I)
    if not game_match:
        raise ValueError("Could not parse game insert statement")
    game_values = re.findall(r"\((\d{4})\)", game_match.group(1))
    game_ids = {int(year): idx + 1 for idx, year in enumerate(game_values)}

    track_match = re.search(r"INSERT INTO track .*?VALUES\s*(.+?);", content, re.S | re.I)
    if not track_match:
        raise ValueError("Could not parse track insert statement")

    track_entries = re.findall(r"\('([^']+)'\s*,\s*'[^']+'\)", track_match.group(1))
    track_ids = {normalize_key(name): idx + 1 for idx, name in enumerate(track_entries)}

    return game_ids, track_ids


def should_skip_circuit(circuit: str) -> bool:
    lower = circuit.strip().lower()
    if not lower:
        return True
    if re.search(r"\bold\b", lower):
        return True
    if re.search(r"\b2\b", lower):
        return True
    for keyword in SKIP_CIRCUIT_KEYWORDS:
        if keyword in lower:
            return True
    return False


def resolve_track_id(circuit: str, track_ids: Dict[str, int]) -> Optional[int]:
    normalized = normalize_key(circuit)
    for alias, target in ALIAS_MAP.items():
        if normalized == normalize_key(alias):
            normalized = normalize_key(target)
            break
    return track_ids.get(normalized)


def parse_pair_with_qr(text: str) -> Tuple[Tuple[int, int], Tuple[int, int]]:
    cleaned = text.replace("*", " ")
    cleaned = cleaned.replace("/", " ")
    cleaned = cleaned.replace("(", " ").replace(")", " ")
    pairs = re.findall(r"(\d+(?:\.\d+)?)[-](\d+(?:\.\d+)?)([qr])?", cleaned, re.I)
    if not pairs:
        raise ValueError(f"Could not parse aero pair from '{text}'")

    base = None
    q_pair = None
    r_pair = None
    for first, second, tag in pairs:
        pair = (int(float(first)), int(float(second)))
        if tag:
            if tag.lower() == "q":
                q_pair = pair
            elif tag.lower() == "r":
                r_pair = pair
        else:
            if base is None:
                base = pair

    if base:
        if q_pair is None:
            q_pair = base
        if r_pair is None:
            r_pair = base

    if q_pair is None and r_pair is not None:
        q_pair = r_pair
    if r_pair is None and q_pair is not None:
        r_pair = q_pair

    if q_pair is None or r_pair is None:
        raise ValueError(f"Aero pair missing Q/R values in '{text}'")

    return q_pair, r_pair


def parse_segment_qr(segment: str) -> Tuple[Optional[float], Optional[float]]:
    seg = segment.strip().lower().replace("*", "")
    if not seg:
        return None, None

    if "/" in seg:
        parts = [part.strip() for part in seg.split("/") if part.strip()]
        q_val = r_val = None
        if parts and not any("q" in part or "r" in part for part in parts) and len(parts) >= 2:
            q_val = float(parts[0])
            r_val = float(parts[1])
        else:
            for part in parts:
                match = re.match(r"(-?\d+(?:\.\d+)?|\.\d+)([qr])?", part)
                if not match:
                    continue
                value = float(match.group(1))
                tag = match.group(2)
                if tag == "q":
                    q_val = value
                elif tag == "r":
                    r_val = value
                else:
                    if q_val is None:
                        q_val = value
                    if r_val is None:
                        r_val = value
        return q_val, r_val

    tokens = re.findall(r"(-?\d+(?:\.\d+)?|\.\d+)([qr])?", seg)
    q_val = None
    r_val = None
    untagged: List[float] = []
    for value, tag in tokens:
        parsed = float(value)
        if tag == "q":
            q_val = parsed
        elif tag == "r":
            r_val = parsed
        else:
            untagged.append(parsed)

    if untagged:
        if len(untagged) == 1:
            if q_val is None:
                q_val = untagged[0]
            if r_val is None:
                r_val = untagged[0]
        else:
            if q_val is None:
                q_val = untagged[0]
            if r_val is None:
                r_val = untagged[1]

    if q_val is None and r_val is not None:
        q_val = r_val
    if r_val is None and q_val is not None:
        r_val = q_val

    return q_val, r_val


def parse_differential(text: str, game_year: int) -> Tuple[List[int], List[int]]:
    expected = 3 if game_year == 2024 else 2
    cleaned = text.replace("*", " ")
    cleaned = cleaned.replace("(", " ").replace(")", " ")
    segments = [segment.strip() for segment in re.split(r"\s*-\s*", cleaned) if segment.strip()]

    if len(segments) < expected:
        raise ValueError(f"Differential '{text}' does not have {expected} segments")

    q_values: List[int] = []
    r_values: List[int] = []
    for idx in range(expected):
        q_val, r_val = parse_segment_qr(segments[idx])
        if q_val is None or r_val is None:
            raise ValueError(f"Missing Q/R differential values in '{text}'")
        q_values.append(int(round(q_val)))
        r_values.append(int(round(r_val)))

    return q_values, r_values


def parse_geometry(text: str, game_year: int) -> Tuple[float, float, float, float]:
    tokens = re.findall(r"(L|R|-?\d+\.\d+|-?\d+|\.\d+)", text, re.I)
    if len(tokens) < 4:
        raise ValueError(f"Suspension geometry '{text}' missing values")

    bounds = GEOMETRY_BOUNDS[game_year]
    fields = ["front_camber", "rear_camber", "front_toe", "rear_toe"]
    values: List[float] = []

    for token, field in zip(tokens[:4], fields):
        upper = token.upper()
        if upper == "L":
            values.append(bounds[field]["min"])
        elif upper == "R":
            values.append(bounds[field]["max"])
        else:
            values.append(float(token))

    return values[0], values[1], values[2], values[3]


def parse_suspension(text: str) -> Tuple[int, int, int, int, int, int]:
    numbers = re.findall(r"\d+", text)
    if len(numbers) < 6:
        raise ValueError(f"Suspension '{text}' missing values")
    values = [int(value) for value in numbers[:6]]
    return values[0], values[1], values[2], values[3], values[4], values[5]


def parse_brakes(text: str) -> Tuple[Tuple[int, int], Tuple[int, int]]:
    cleaned = text.replace("*", " ")
    cleaned = re.sub(r"\([^)]*\)", " ", cleaned)
    if "/" in cleaned:
        parts = [part.strip() for part in re.split(r"\s*/\s*", cleaned, maxsplit=1) if part.strip()]
    else:
        parts = [part.strip() for part in re.split(r"\s*-\s*", cleaned, maxsplit=1) if part.strip()]
    if len(parts) < 2:
        raise ValueError(f"Brakes '{text}' missing pressure/bias")

    pressure = int(float(parts[0]))
    bias_text = re.sub(r"[^0-9qr./ -]", " ", parts[1].lower())
    bias_q, bias_r = parse_segment_qr(bias_text)
    if bias_q is None or bias_r is None:
        raise ValueError(f"Brakes '{text}' missing Q/R bias")

    return (pressure, int(round(bias_q))), (pressure, int(round(bias_r)))


def parse_tire_pair(text: str, game_year: int, q_pair: Optional[Tuple[float, float]] = None) -> Tuple[float, float]:
    cleaned = text.strip().lower().replace("*", "")
    if "same" in cleaned:
        if q_pair is None:
            raise ValueError("Tire pair marked as same but no Q pair provided")
        return q_pair

    compact = cleaned.replace(" ", "")
    if compact in ("minall", "maxall"):
        choice = "min" if "min" in compact else "max"
        return (
            TIRE_PRESSURE_BOUNDS[game_year]["front"][choice],
            TIRE_PRESSURE_BOUNDS[game_year]["rear"][choice],
        )

    if "-" in cleaned:
        parts = [part.strip() for part in cleaned.split("-") if part.strip()]
    else:
        parts = [part.strip() for part in cleaned.split() if part.strip()]

    if len(parts) == 1 and parts[0] in ("min", "max"):
        choice = parts[0]
        return (
            TIRE_PRESSURE_BOUNDS[game_year]["front"][choice],
            TIRE_PRESSURE_BOUNDS[game_year]["rear"][choice],
        )

    if len(parts) < 2:
        raise ValueError(f"Tire pair '{text}' missing values")

    def parse_value(value: str, axis: str) -> float:
        if value == "min":
            return TIRE_PRESSURE_BOUNDS[game_year][axis]["min"]
        if value == "max":
            return TIRE_PRESSURE_BOUNDS[game_year][axis]["max"]
        return float(value)

    front_val = parse_value(parts[0], "front")
    rear_val = parse_value(parts[1], "rear")
    return front_val, rear_val


def load_csv_rows(
    base_dir: Path, game_year: int, filename: str, track_ids: Dict[str, int]
) -> Tuple[List[ParsedRow], List[str]]:
    csv_path = base_dir / filename
    if not csv_path.exists():
        raise FileNotFoundError(f"CSV not found: {csv_path}")

    parsed_rows: List[ParsedRow] = []
    skipped: List[str] = []

    with csv_path.open(newline="", encoding="utf-8") as handle:
        reader = csv.reader(handle)
        for idx, row in enumerate(reader):
            if not row:
                continue
            circuit = row[0].strip() if len(row) > 0 else ""
            if idx == 0 and circuit.lower() == "circuit":
                continue
            if should_skip_circuit(circuit):
                continue
            has_numeric = any(
                re.search(r"\d", cell) for cell in row[1:8] if isinstance(cell, str)
            )
            if not has_numeric:
                continue

            track_id = resolve_track_id(circuit, track_ids)
            if track_id is None:
                if re.search(r"[0-9:/]", circuit):
                    continue
                skipped.append(circuit)
                continue

            try:
                aero_q, aero_r = parse_pair_with_qr(row[1])
                diff_q, diff_r = parse_differential(row[2], game_year)
                geometry = parse_geometry(row[3], game_year)
                suspension = parse_suspension(row[4])
                brakes_q, brakes_r = parse_brakes(row[5])
                tires_q = parse_tire_pair(row[6], game_year)
                tires_r = parse_tire_pair(row[7], game_year, tires_q)
            except Exception as exc:
                raise ValueError(f"Failed parsing '{circuit}' ({filename}): {exc}") from exc

            parsed_rows.append(
                ParsedRow(
                    game_year=game_year,
                    track_id=track_id,
                    track_name=circuit.strip(),
                    aero_q=aero_q,
                    aero_r=aero_r,
                    diff_q=diff_q,
                    diff_r=diff_r,
                    geometry=geometry,
                    suspension=suspension,
                    brakes_q=brakes_q,
                    brakes_r=brakes_r,
                    tires_q=tires_q,
                    tires_r=tires_r,
                )
            )

    return parsed_rows, skipped


def build_insert_rows(
    parsed_rows: Iterable[ParsedRow], game_ids: Dict[int, int]
) -> Tuple[List[Tuple], Dict[int, int]]:
    inserts: List[Tuple] = []
    counts: Dict[int, int] = {}

    for row in parsed_rows:
        game_id = game_ids.get(row.game_year)
        if game_id is None:
            raise ValueError(f"Missing game id for year {row.game_year}")

        counts[row.game_year] = counts.get(row.game_year, 0) + 2

        q_title = f"{row.track_name} predeterminado - Clasificacion"
        r_title = f"{row.track_name} predeterminado - Carrera"

        q_engine = row.diff_q[2] if row.game_year == 2024 else 0
        r_engine = row.diff_r[2] if row.game_year == 2024 else 0

        inserts.append(
            (
                USER_ID,
                game_id,
                row.track_id,
                TEAM_ID,
                q_title,
                ANNOTATION,
                "QUALIFYING",
                CONTROLLER_TYPE,
                IS_WET_WEATHER,
                row.aero_q[0],
                row.aero_q[1],
                row.diff_q[0],
                row.diff_q[1],
                q_engine,
                row.geometry[0],
                row.geometry[1],
                row.geometry[2],
                row.geometry[3],
                row.suspension[0],
                row.suspension[1],
                row.suspension[2],
                row.suspension[3],
                row.suspension[4],
                row.suspension[5],
                row.brakes_q[0],
                row.brakes_q[1],
                row.tires_q[0],
                row.tires_q[0],
                row.tires_q[1],
                row.tires_q[1],
            )
        )

        inserts.append(
            (
                USER_ID,
                game_id,
                row.track_id,
                TEAM_ID,
                r_title,
                ANNOTATION,
                "RACE",
                CONTROLLER_TYPE,
                IS_WET_WEATHER,
                row.aero_r[0],
                row.aero_r[1],
                row.diff_r[0],
                row.diff_r[1],
                r_engine,
                row.geometry[0],
                row.geometry[1],
                row.geometry[2],
                row.geometry[3],
                row.suspension[0],
                row.suspension[1],
                row.suspension[2],
                row.suspension[3],
                row.suspension[4],
                row.suspension[5],
                row.brakes_r[0],
                row.brakes_r[1],
                row.tires_r[0],
                row.tires_r[0],
                row.tires_r[1],
                row.tires_r[1],
            )
        )

    return inserts, counts


def sql_escape(value: object) -> str:
    if value is None:
        return "NULL"
    if isinstance(value, (int, float)):
        return str(value)
    return "'" + str(value).replace("'", "''") + "'"


def write_sql(sql_path: Path, rows: List[Tuple]) -> None:
    with sql_path.open("w", encoding="utf-8") as handle:
        handle.write("USE f1setups;\n")
        handle.write(
            f"INSERT INTO setup ({', '.join(INSERT_COLUMNS)}) VALUES\n"
        )
        values = []
        for row in rows:
            rendered = ", ".join(sql_escape(value) for value in row)
            values.append(f"({rendered})")
        handle.write(",\n".join(values))
        handle.write(";\n")


def insert_rows(host: str, port: int, user: str, password: str, database: str, rows: List[Tuple]) -> None:
    try:
        import mysql.connector
    except ImportError as exc:
        raise SystemExit(
            "mysql-connector-python is required for DB inserts. Install requirements first."
        ) from exc

    connection = mysql.connector.connect(
        host=host,
        port=port,
        user=user,
        password=password,
        database=database,
    )

    insert_sql = (
        f"INSERT INTO setup ({', '.join(INSERT_COLUMNS)}) VALUES "
        f"({', '.join(['%s'] * len(INSERT_COLUMNS))})"
    )

    with connection:
        cursor = connection.cursor()
        cursor.executemany(insert_sql, rows)
        connection.commit()


def build_arg_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Seed default setups from CSV files.")
    parser.add_argument("--init-sql", default="database/init.sql", help="Path to init.sql")
    parser.add_argument("--dry-run", action="store_true", help="Parse and summarize without inserting")
    parser.add_argument("--sql-out", help="Write SQL inserts to a file instead of inserting")
    parser.add_argument("--host", default="127.0.0.1", help="MySQL host")
    parser.add_argument("--port", type=int, default=3306, help="MySQL port")
    parser.add_argument("--user", default="root", help="MySQL user")
    parser.add_argument("--password", default="password", help="MySQL password")
    parser.add_argument("--database", default="f1setups", help="Target database")
    return parser


def main(argv: Optional[List[str]] = None) -> int:
    args = build_arg_parser().parse_args(argv)

    repo_root = Path(__file__).resolve().parents[1]
    init_sql_path = (repo_root / args.init_sql).resolve()

    game_ids, track_ids = parse_init_sql(init_sql_path)

    all_rows: List[ParsedRow] = []
    all_skipped: List[str] = []

    for game_year, filename in CSV_FILES:
        rows, skipped = load_csv_rows(repo_root, game_year, filename, track_ids)
        all_rows.extend(rows)
        all_skipped.extend(skipped)

    insert_rows_list, counts = build_insert_rows(all_rows, game_ids)

    print("Parsed setups:")
    for year in sorted(counts.keys()):
        print(f"  {year}: {counts[year]} records")
    print(f"  total: {len(insert_rows_list)} records")

    if all_skipped:
        print("Skipped circuits (missing mapping):")
        for circuit in sorted(set(all_skipped)):
            print(f"  - {circuit}")

    if args.dry_run:
        print("Dry-run mode: no inserts executed.")
        return 0

    if args.sql_out:
        sql_path = Path(args.sql_out).resolve()
        write_sql(sql_path, insert_rows_list)
        print(f"SQL written to {sql_path}")
        return 0

    insert_rows(
        host=args.host,
        port=args.port,
        user=args.user,
        password=args.password,
        database=args.database,
        rows=insert_rows_list,
    )
    print("Insert completed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
