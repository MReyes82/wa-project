# F1Setups Hub

F1Setups Hub es una aplicación web para consultar, crear y administrar configuraciones de autos para los juegos F1 22, F1 23, F1 24 y F1 25.

La aplicación permite registrar usuarios, iniciar sesión, consultar setups predeterminados, explorar setups de la comunidad y administrar setups propios. El servidor expone una API HTTP bajo `/api` y utiliza MySQL como base de datos.

## Funcionalidades

- Registro e inicio de sesión de usuarios.
- Inicio de sesión con token de autenticación.
- Consulta de setups predeterminados por juego y pista.
- Consulta y búsqueda de setups publicados por otros usuarios.
- Administración de setups propios: crear, consultar, editar y eliminar.
- Filtros por versión del juego y circuito.
- Interfaz web construida con HTML, CSS y JavaScript vanilla.

## Tecnologías

- Frontend: HTML, CSS y JavaScript.
- Servidor: Java 21.
- Build tool: Maven.
- API HTTP: `com.sun.net.httpserver.HttpServer`.
- Serialización JSON: Gson.
- Base de datos: MySQL 8.
- Pool de conexiones: HikariCP.
- Acceso a datos: JDBC con prepared statements.
- Hash de contraseñas: BCrypt.
- Entorno local de base de datos: Docker Compose.

## Estructura del Proyecto

```text
.
+-- backend/
|   +-- pom.xml
|   +-- src/
|   |   +-- main/java/com/f1setups/
|   |   |   +-- Main.java
|   |   |   +-- DTO/
|   |   |   +-- controllers/
|   |   |   +-- dao/
|   |   |   +-- models/
|   |   |   +-- services/
|   |   +-- test/
+-- database/
|   +-- docker-compose.yml
|   +-- init.sql
|   +-- requirements.txt
|   +-- run_seed.py
|   +-- seed_setups.py
+-- frontend/
|   +-- css/
|   +-- js/
|   +-- pages/
|   +-- index.html
+-- README.md
```

## Arquitectura del Servidor

El servidor está organizado por paquetes:

- `Main.java`: crea el servidor HTTP en `localhost:8080`, registra los contextos `/api/auth` y `/api/setups`, e inicializa controladores, servicios y DAOs.
- `controllers`: traducen peticiones HTTP a llamadas de servicio y devuelven respuestas JSON.
- `services`: aplican reglas de negocio, autenticación, validación y ownership.
- `dao`: ejecuta consultas SQL con JDBC y prepared statements.
- `models`: contiene las entidades principales del dominio.
- `DTO`: contiene objetos de request/response usados entre controladores y servicios.

### Controladores

- `AuthController`: maneja `/api/auth/register` y `/api/auth/login`.
- `SetupController`: maneja rutas públicas de setups, rutas de comunidad y rutas autenticadas del usuario.

### Servicios

- `AuthService`: coordina login, registro, verificación de contraseña y generación de tokens.
- `AuthTokenService`: genera y valida tokens bearer firmados con HMAC-SHA256.
- `SetupService`: valida filtros, payloads de setup y reglas de ownership.

### DAOs

- `UserDAO`: consulta y guarda usuarios en `users`.
- `SetupDao`: consulta, guarda, actualiza y elimina setups en `setup`.
- `DatabaseUtil`: configura el pool HikariCP y las conexiones a MySQL.

## Reglas de Autenticación

El login usa email y contraseña:

```json
{
  "email": "usuario@example.com",
  "password": "secret123"
}
```

Si el login es correcto, el servidor responde:

```json
{
  "success": true,
  "message": "Login successful",
  "userId": 2,
  "token": "<token>"
}
```

Las rutas privadas deben enviar el token así:

```text
Authorization: Bearer <token>
```

El token tiene este formato:

```text
base64url(userId:expiresAt).base64url(hmacSignature)
```

Detalles:

- Algoritmo HMAC: `HmacSHA256`.
- Duración del token: 24 horas.
- El secreto viene de `F1SETUPS_AUTH_SECRET`.
- En local se usa un secreto de desarrollo si no existe la variable.
- La comparación de firmas usa comparación constante para no filtrar información.

## Reglas de Contraseña

El registro recibe:

```json
{
  "username": "usuario",
  "email": "usuario@example.com",
  "password": "secret123"
}
```

Reglas:

- No se permiten emails duplicados.
- La contraseña debe tener al menos 6 caracteres.
- Se genera un salt con BCrypt.
- Se guarda `BCrypt(password + salt)`.
- El salt también se guarda por separado en la tabla `users`.

## Modelo de Setup

Los setups se devuelven en JSON con nombres `camelCase`:

```text
id
userId
gameVersionId
trackId
teamId
title
annotation
sessionType
controllerType
isWetWeather
createdAt
frontWing
rearWing
diffOnThrottle
diffOffThrottle
engineBraking
frontCamber
rearCamber
frontToe
rearToe
frontSuspension
rearSuspension
frontAntiRollBar
rearAntiRollBar
frontRideHeight
rearRideHeight
brakePressure
brakeBias
frontRightPressure
frontLeftPressure
rearRightPressure
rearLeftPressure
```

`createdAt` usa el formato de `LocalDateTime.toString()`:

```text
2026-05-21T14:05:35
```

## Reglas de Setups

- Los setups predeterminados pertenecen al usuario reservado `user_id = 1`.
- Las rutas de comunidad excluyen `user_id = 1`.
- Las búsquedas usan `LOWER(title) LIKE %query%`.
- Los resultados de comunidad devuelven objetos `{ setup, username }`.
- Las rutas `/setups/me/*` siempre toman el usuario autenticado desde el token.
- Cualquier `userId` enviado desde el cliente se ignora en rutas autenticadas.
- La actualización y eliminación de setups validan ownership por `setupId` y usuario autenticado.

## Respuestas de Error

Errores de autenticación:

```json
{
  "success": false,
  "message": "Login unsuccessful",
  "userId": -1,
  "token": ""
}
```

Errores de registro:

```json
{
  "success": false,
  "message": "Register unsuccessful",
  "userId": -1
}
```

Errores de setups:

```json
{
  "error": "Unauthorized"
}
```

Mapeo general:

- `400`: parámetros faltantes, body inválido o validación fallida.
- `401`: token faltante o inválido.
- `404`: recurso no encontrado.
- `405`: método no permitido.
- `500`: error inesperado del servidor.

## API

URL base:

```text
http://localhost:8080/api
```

| Método | Ruta | Auth | Descripción |
| --- | --- | --- | --- |
| `POST` | `/auth/login` | No | Inicia sesión. |
| `POST` | `/auth/register` | No | Registra un usuario. |
| `GET` | `/setups/defaults?gameId=&trackId=` | No | Devuelve todos los setups predeterminados para juego y pista. |
| `GET` | `/setups/default?gameId=&trackId=` | No | Devuelve el primer setup predeterminado para juego y pista. |
| `GET` | `/setups?gameId=&trackId=` | No | Alias de compatibilidad para setup predeterminado. |
| `GET` | `/setups/community?gameId=&trackId=` | No | Lista setups de comunidad por juego y pista. |
| `GET` | `/setups/community/search?gameId=&query=` | No | Busca setups de comunidad por título. |
| `GET` | `/setups/me?gameId=&trackId=` | Sí | Lista setups propios por juego y pista. |
| `GET` | `/setups/me/search?gameId=&query=` | Sí | Busca setups propios por título. |
| `POST` | `/setups/me` | Sí | Crea un setup propio. |
| `GET` | `/setups/me/{setupId}` | Sí | Consulta un setup propio. |
| `PUT` | `/setups/me/{setupId}` | Sí | Actualiza un setup propio. |
| `DELETE` | `/setups/me/{setupId}` | Sí | Elimina un setup propio. |
| `OPTIONS` | `/api/*` | No | Preflight CORS. |

## Base de Datos

El esquema está en:

```text
database/init.sql
```

Base de datos usada por la aplicación:

```text
f1setups
```

Tablas principales:

- `users`: usuarios, email, contraseña y salt.
- `game`: versiones de F1.
- `track`: catálogo de circuitos.
- `team`: catálogo de equipos.
- `setup`: setups guardados.

## Ejecutar Localmente

### 1. Iniciar MySQL

```bash
cd /home/mreyes/Desktop/wa-project/database
docker compose up -d
```

### 2. Compilar el Servidor

```bash
cd /home/mreyes/Desktop/wa-project/backend
mvn -q -DskipTests compile
```

### 3. Ejecutar el Servidor

```bash
cd /home/mreyes/Desktop/wa-project/backend
mvn -q -DskipTests compile exec:java
```

El servidor queda disponible en:

```text
http://localhost:8080/api
```

### 4. Ejecutar el Frontend

```bash
cd /home/mreyes/Desktop/wa-project
python3 -m http.server 5173 --directory frontend
```

Abrir:

```text
http://localhost:5173
```

## Variables de Entorno

Variable usada para firmar tokens:

```text
F1SETUPS_AUTH_SECRET
```

Si no se define, el servidor usa un secreto local de desarrollo.

## Cargar Setups Predeterminados

Instalar dependencias del seeder:

```bash
cd /home/mreyes/Desktop/wa-project
python3 -m venv database/.venv
source database/.venv/bin/activate
pip install -r database/requirements.txt
```

Ejecutar prueba sin insertar:

```bash
python3 database/seed_setups.py --dry-run
```

Insertar en MySQL:

```bash
python3 database/seed_setups.py \
  --host 127.0.0.1 \
  --port 3306 \
  --user root \
  --password password \
  --database f1setups
```

Los setups predeterminados seran parseados desde los archivos `.csv` en `database/spreadsheet` y guardados con `user_id = 1`. Por lo que será necesario tener un usuario con `id = 1` en la tabla `users` para que el proceso funcione correctamente.

## Pruebas Rápidas

Consultar setups predeterminados:

```bash
curl -i 'http://localhost:8080/api/setups/defaults?gameId=4&trackId=1'
```

Registrar usuario:

```bash
curl -i -H 'Content-Type: application/json' \
  -d '{"username":"test","email":"test@example.com","password":"secret123"}' \
  http://localhost:8080/api/auth/register
```

Iniciar sesión:

```bash
curl -i -H 'Content-Type: application/json' \
  -d '{"email":"test@example.com","password":"secret123"}' \
  http://localhost:8080/api/auth/login
```

## Pruebas del Servidor

Pruebas unitarias:

```bash
cd /home/mreyes/Desktop/wa-project/backend
mvn test
```

Pruebas de integración con Testcontainers:

```bash
cd /home/mreyes/Desktop/wa-project/backend
mvn -DskipITs=false verify
```

## Solución de Problemas

Si el frontend no conecta con la API, revisar que el servidor esté escuchando en:

```text
http://localhost:8080/api
```

Si la API no conecta con MySQL, revisar el contenedor:

```bash
cd /home/mreyes/Desktop/wa-project/database
docker compose ps
```

Si no aparecen setups predeterminados, confirmar que la base de datos `f1setups` existe y volver a ejecutar el seeder.

Si el puerto `8080` está ocupado, detener el proceso que lo usa o cambiar el puerto del servidor y actualizar `frontend/js/api.js`.
