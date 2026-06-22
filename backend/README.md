# Showdown Backend

Spring Boot REST API for the Showdown tournament platform.

## Run Locally

Run these commands from the `backend` folder after PostgreSQL is ready:

```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/showdown_tournament"
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="<local password>"
.\mvnw.cmd spring-boot:run
```

Local helper command files live under `local/scripts/` and are ignored by Git.

Swagger UI:

```text
http://localhost:8080/swagger-ui.html
```

## Default Development Accounts

Development credentials are supplied through environment variables in normal use.

- Admin: `ADMIN_USER`, `ADMIN_PASSWORD`
- Referee: `REFEREE_USER`, `REFEREE_PASSWORD`
- Player: `PLAYER_USER`, `PLAYER_PASSWORD`

## Main Endpoints

- `GET /api/public/**`
- `/api/admin/**`
- `/api/scoring/**`
- `/api/player/**`
