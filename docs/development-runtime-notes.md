# Development Runtime Notes

## Local Toolchain

- Frontend Node.js is managed with `nvm`. Use Node `22.12.0` or newer before running Vite, Vitest or ESLint.
- Backend Java uses JDK `D:\software\jdk-25.0.2`.

## Database Runtime

- The shared PostgreSQL instance for Sprint 1 and Sprint 2 integration is available at `192.168.3.2:5432`.
- Database name: `heimdallr_monitor`.
- Default user/password follow `deploy/.env.example`: `monitor` / `monitor_pass`.
- The local compose baseline is `deploy/docker-compose.yml`, with PostgreSQL pinned to `postgres:15-alpine`.

## Backend Profiles

- Default profile uses the in-memory fixture data and is intended for fast local tests.
- `db` profile uses PostgreSQL and runs the Flyway migrations from `backend/db-migration`.

Example:

```powershell
$env:JAVA_HOME='D:\software\jdk-25.0.2'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
cd backend\heimdallr-api
mvn "-Dspring-boot.run.profiles=db" spring-boot:run
```
