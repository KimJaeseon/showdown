# Showdown

Tournament operation platform with a Spring Boot backend, Next.js frontend, and PostgreSQL schema assets.

## Project Structure

```text
backend/       Spring Boot API server
frontend/      Next.js web application
database/sql/  PostgreSQL schema and seed scripts
docs/design/   Architecture and database design documents
docs/testing/  Test scenarios and test reports
local/         Local-only notes, logs, and command files ignored by Git
```

## Local-Only Files

Files under `local/`, `.env*`, logs, dependency folders, and command files are excluded from version control.
Do not commit database passwords, API keys, tokens, or machine-specific connection information.
