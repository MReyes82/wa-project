# Seeder de Setups Predeterminados

Carga la tabla `setup` con setups predeterminados a partir de los archivos CSV ubicados en la raíz del repositorio.

## Qué Hace

- Lee los cuatro CSV de F1 22 a F1 25.
- Convierte cada circuito en dos registros: clasificación y carrera.
- Usa las reglas de equivalencias definidas para interpretar los datos de los CSV.
- Omite circuitos no mapeados y entradas antiguas o alternativas.

## Requisitos

- Python 3.10 o superior recomendado.
- Servidor MySQL, normalmente el contenedor Docker definido en `database/docker-compose.yml`.

## Instalar Dependencias

```bash
python3 -m venv .venv
source .venv/bin/activate
pip install -r database/requirements.txt
```

## Prueba sin Insertar Datos

Recomendado antes de escribir en MySQL:

```bash
python3 database/seed_setups.py --dry-run
```

## Generar SQL sin Insertar

```bash
python3 database/seed_setups.py --sql-out database/seed_setups.sql
```

## Insertar en MySQL

```bash
python3 database/seed_setups.py --host 127.0.0.1 --port 3306 --user root --password password --database f1setups
```

## Valores Usados por Defecto

- `user_id`: 1
- `team_id`: 16
- `controller_type`: `GAMEPAD`
- `is_wet_weather`: `FALSE`
- `annotation`: `Setup predeterminado`
- `title`: `{track} predeterminado - Clasificacion` / `{track} predeterminado - Carrera`
- `engine_braking`: solo se llena para F1 24, donde existe el tercer valor de transmisión.
