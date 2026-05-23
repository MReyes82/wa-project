# Default Setup Seeder

Seeds the `setup` table with default setups from the CSV files in the repo root.

## What it does

- Reads all four CSVs (F1 22-25).
- Expands each circuit into two records: qualifying and race.
- Uses the token rules in `token-equivalnces.md`.
- Skips unmapped circuits and "old/alt" entries.

## Requirements

- Python 3.10+ recommended.
- MySQL server (Docker container in `database/docker-compose.yml`).

## Install dependencies

```bash
python3 -m venv .venv
source .venv/bin/activate
pip install -r database/requirements.txt
```

## Dry run (recommended first)

```bash
python3 database/seed_setups.py --dry-run
```

## Write SQL without inserting

```bash
python3 database/seed_setups.py --sql-out database/seed_setups.sql
```

## Insert into MySQL (root)

```bash
python3 database/seed_setups.py --host 127.0.0.1 --port 3306 --user root --password password --database f1setups
```

## Defaults used

- `user_id`: 1
- `team_id`: 16
- `controller_type`: `GAMEPAD`
- `is_wet_weather`: `FALSE`
- `annotation`: `Setup predeterminado`
- `title`: `{track} predeterminado - Clasificacion` / `{track} predeterminado - Carrera`
- `engine_braking`: only set for F1 24 (third transmission value)

