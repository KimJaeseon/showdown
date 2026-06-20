# Showdown Backend

Spring Boot, Spring Data JPA, Spring Security, Swagger UI, PostgreSQL 기반 REST API입니다.

## 실행

PostgreSQL 준비 후 `back` 폴더에서 실행합니다.

```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/showdown_tournament"
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="<local password>"
.\mvnw.cmd spring-boot:run
```

Swagger UI:

```text
http://localhost:8080/swagger-ui.html
```

## 기본 롤

개발 기본 계정은 환경 변수로 바꿀 수 있습니다.

- 관리자: `admin / admin1234`, ROLE_ADMIN
- 심판/기록원: `referee / referee1234`, ROLE_REFEREE
- 선수: `player / player1234`, ROLE_PLAYER

## API 권한 구조

- `GET /api/public/**`: 비로그인 공개 조회
- `/api/admin/**`: 관리자 전용 CRUD
- `/api/scoring/**`: 심판 또는 관리자 점수 입력
- `/api/player/**`: 선수 또는 관리자 조회

## 주요 엔드포인트

- `POST /api/admin/tournaments`
- `GET /api/admin/tournaments`
- `POST /api/admin/tournaments/{tournamentId}/divisions`
- `POST /api/admin/tournaments/{tournamentId}/players`
- `POST /api/admin/tournaments/{tournamentId}/officials`
- `POST /api/admin/tournaments/{tournamentId}/stages`
- `POST /api/admin/tournaments/{tournamentId}/groups`
- `POST /api/admin/tournaments/{tournamentId}/matches`
- `PUT /api/scoring/matches/{matchId}/sets`
