# Photoffice Developer Guidelines

This document contains essential project-specific information for developers working on the Photoffice codebase.
It is the single source of truth for all coding agents (Claude Code, Junie, ...) and humans, on every machine.

---

## 0. Collaboration Protocol (multiple agents, multiple machines)

Agents on different machines do **not** share local memory. Everything other agents need to know
must live in this repository or on GitHub.

### Repository layout
- `origin` = https://github.com/PitFroehlich/Photoffice (our fork, default branch `main`)
- `upstream` = https://github.com/stargazer74/Photoffice (original, branch `master`)
- Use `main`, never `master`, for our own work. Sync from the original with
  `git fetch upstream && git merge upstream/master`.

### Where knowledge lives
| What | Where |
|------|-------|
| Stable rules, setup, conventions | `AGENTS.md` (this file) |
| Who is working on what (tasks) | GitHub Issues + label `in-progress` |
| What was done in a session and why | `docs/journal/` (one file per session) |
| Architecture decisions | `docs/decisions/` (ADRs) |

### At the start of a session
1. `git fetch --all --prune` and `git pull` on the current branch.
2. `gh issue list` – check what is open and what is `in-progress` (someone else is on it – don't touch).
   The pinned **Roadmap issue (#21)** shows the order of work. Only pick an issue whose
   *Blockiert durch* issues are all closed; issues with the same `stufe:N` label can be worked on in parallel.
3. Read the latest entries in `docs/journal/` and any ADRs relevant to your task.

### While working
1. Every task has a GitHub Issue. Create one if missing (`gh issue create`). For new issues, set
   GitHub dependencies ("blocked by"), add the *Abhängigkeiten* section and update the Roadmap issue.
2. Claim it: `gh issue edit <nr> --add-label in-progress --add-assignee @me` and comment which
   machine/agent is working on it (`gh issue comment <nr> --body "..."`).
3. Work on a branch `feature/<nr>-short-title` (or `fix/...`, `chore/...`) created from `main`.
4. Never push directly to `main`; open a PR with `Closes #<nr>` in the description.
5. If blocked, set label `blocked` and explain why in an issue comment.
6. Significant architecture/technology decisions get an ADR in `docs/decisions/`.

### At the end of a session
1. Write a journal entry `docs/journal/YYYY-MM-DD-<hostname>-<topic>.md` (see `docs/journal/README.md`).
2. Commit and push your branch; open/update the PR.
3. Update the issue (comment with status; remove `in-progress` if you stop working on it unfinished).

---

## 1. Repository Layout

| Path | Content |
|------|---------|
| `api/openapi.yaml` | REST API contract – **source of truth** (API-first). Change the API here first. |
| `backend/` | Java 25 + Spring Boot 4 + Spring Modulith (Maven wrapper) |
| `frontend/` | Angular 22 SPA (npm) |
| `docker-compose.yml` | Local infrastructure: PostgreSQL, SeaweedFS (S3), Keycloak, imgproxy |
| `infra/` | Configuration for the local infrastructure (Keycloak realm, S3 credentials) |
| `docs/` | Analysis, ADRs, journal |
| `legacy/` | Old PHP 5.6 application – reference only, see `legacy/AGENTS.md` |

Decisions behind the stack: `docs/decisions/0003-tech-stack.md`, `0004-local-s3-seaweedfs.md`.

---

## 2. Local Development

### Requirements
- JDK 25 (e.g. `sdk env` in `backend/` with sdkman; `backend/.sdkmanrc`). **Not** the system Java 21 – class file version errors otherwise.
- Node.js 24 + npm
- Docker with Compose

### Start
```bash
docker compose up -d                       # infrastructure
cd backend && ./mvnw spring-boot:run       # http://localhost:8080 (Flyway migrates on start)
cd frontend && npm install && npm start    # http://localhost:4200, proxies /api to :8080
```

### Local services (development credentials only)
| Service | URL | Credentials |
|---------|-----|-------------|
| PostgreSQL | `localhost:5432`, DB `photoffice` | `photoffice` / `photoffice` |
| SeaweedFS S3 API | http://localhost:8333, bucket `photoffice` | `photoffice` / `photoffice-dev-secret` |
| Keycloak | http://localhost:8180 (realm `photoffice`) | admin console: `admin` / `admin` |
| imgproxy | http://localhost:8081 (signed URLs only; key/salt in `docker-compose.yml`) | – |

The backend reads `DB_URL`, `DB_USER`, `DB_PASSWORD` (defaults match the compose setup).

---

## 3. Testing

```bash
cd backend && ./mvnw verify                # JUnit + Testcontainers (Docker required), Modulith structure check
cd frontend && npm test -- --watch=false   # Vitest
cd frontend && npm run build
```
CI (`.github/workflows/ci.yml`) runs the same commands on every PR and on `main`.

Rules:
- Every feature comes with tests. Integration tests use **Testcontainers** (`TestcontainersConfiguration`), not mocks of the database.
- `ModularityTests` verifies the Spring Modulith module structure – keep it green; it also writes module docs to `backend/target/spring-modulith-docs`.

---

## 4. Architecture & Conventions

### API-first
1. Change `api/openapi.yaml`.
2. Backend: `./mvnw compile` generates interfaces/models into `de.photoffice.api` (`target/generated-sources`); controllers implement the generated `*Api` interfaces.
3. Frontend: `npm run generate:api` (runs automatically before `start`/`build`/`test`) generates the client into `src/app/api/` (git-ignored). Call endpoints via `inject(Api).invoke(<fn>)`.
Never edit generated code.

### Backend (Spring Modulith)
- Base package `de.photoffice`; **each direct sub-package is a module** (e.g. `system`, later `tenant`, `gallery`, `order`, …). Sub-packages of a module are internal.
- Modules talk to each other through their public API (types in the module's root package) or **domain events** (`ApplicationEventPublisher` + `@ApplicationModuleListener`). Events are persisted in `event_publication`.
- `de.photoffice.api` (generated) is an open module.
- Database: schema only via Flyway migrations in `backend/src/main/resources/db/migration` (`V<n>__<description>.sql`); Hibernate runs with `ddl-auto: validate`.
- Money: never floating point.

### Frontend
- Standalone components, signals, new control flow (`@if`, `@for`); zoneless as generated by Angular CLI 22.

### Language
- Code identifiers in **English**; domain documentation, ADRs, journal, and issues in German.
- Avoid copying legacy patterns (see `docs/analysis/legacy-features.md`, section 5).
