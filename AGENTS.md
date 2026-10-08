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
| What a finished issue looks like | `docs/demos/` (one demo per issue) |

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
7. Several agents work in parallel – follow "Parallel work" below.

### Parallel work
- **Database changes (Liquibase):** add a new file `backend/src/main/resources/db/changelog/changes/<yyyyMMddHHmm>-<description>.sql`
  (Liquibase formatted SQL, header `--liquibase formatted sql`, changesets `--changeset photoffice:<unique-id>`).
  The master changelog picks it up automatically (`includeAll`) – **don't edit the master changelog**.
  Liquibase runs every changeset not yet executed, so merge order doesn't matter.
  Never change a changeset that is already on `main` – add a new one.
- **Shared files** – keep changes small and local, rebase on `main` right before opening the PR and again
  if `main` moved: `api/openapi.yaml` (add paths/schemas in your own block, don't reorder), `SecurityConfiguration`
  (one `requestMatchers` line per endpoint group), `AGENTS.md` (own subsection), `app.routes.ts`, `pom.xml`,
  `package.json` (don't upgrade unrelated dependencies).
- **Backend modules:** one Spring Modulith module per domain (e.g. `customer`, `gallery`); don't edit another
  module's internals – use its public API or events, or ask in the other issue.
- **Before the PR:** `git fetch && git rebase origin/main`, run backend + frontend tests again.
- Stay inside the scope of your issue; create a new issue for anything else you find.

### Demo after every finished issue (mandatory)
When an issue is done, the agent gives the user a **guided demo through the running application – step by step,
no screenshots**:
1. Start the app locally with test data (`docker compose up -d`, backend with profile `dev`, `npm start`) and
   check that it is reachable.
2. Write `docs/demos/<issue-nr>-<short-topic>.md` from the template in `docs/demos/README.md`: what was implemented,
   start instructions, test users, and a **walkthrough**: numbered steps, each with *what to do* (where to click,
   what to enter) and *what you should see*. Backend-only issues: steps with `curl` calls and the expected output.
3. Before presenting it, verify the walkthrough yourself (ideally as Playwright E2E test in `frontend/e2e/`).
4. In the conversation: lead the user through the walkthrough **one step at a time** – describe the step, wait
   until the user has done it (or asks questions), then continue. At the end summarise what changed and what is
   still open.
5. Link the demo in the PR description and in the closing comment of the issue.
6. Stop everything after the demo (backend, `ng serve`, `docker compose down`).

### At the end of a session
1. Write a journal entry `docs/journal/YYYY-MM-DD-<hostname>-<topic>.md` (see `docs/journal/README.md`).
2. If the issue is finished: create the demo (see above).
3. Commit and push your branch; open/update the PR.
4. Update the issue (comment with status; remove `in-progress` if you stop working on it unfinished).
5. Stop everything you started locally (backend, `ng serve`, `docker compose down`).

---

## 1. Repository Layout

| Path | Content |
|------|---------|
| `api/openapi.yaml` | REST API contract – **source of truth** (API-first). Change the API here first. |
| `backend/` | Java 25 + Spring Boot 4 + Spring Modulith (Maven wrapper) |
| `frontend/` | Angular 22 SPA (npm) |
| `docker-compose.yml` | Local infrastructure: PostgreSQL, SeaweedFS (S3), Keycloak, imgproxy, Mailpit |
| `infra/` | Configuration for the local infrastructure (Keycloak realm, S3 credentials) |
| `docs/` | Analysis, ADRs, journal, demos |
| `legacy/` | Old PHP 5.6 application – reference only, see `legacy/AGENTS.md` |

Decisions behind the stack: `docs/decisions/0003-tech-stack.md`, `0004-local-s3-seaweedfs.md`, `0005-ui-angular-material.md`, `0006-liquibase.md`, `0007-studio-onboarding-keycloak.md`.

---

## 2. Local Development

### Requirements
- JDK 25 (e.g. `sdk env` in `backend/` with sdkman; `backend/.sdkmanrc`). **Not** the system Java 21 – class file version errors otherwise.
- Node.js 24 + npm
- Docker with Compose

### Start
```bash
docker compose up -d                       # infrastructure
cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev   # http://localhost:8080, Liquibase + dev data (studios A/B)
cd frontend && npm install && npm start    # http://localhost:4200, proxies /api to :8080
```

### Local services (development credentials only)
| Service | URL | Credentials |
|---------|-----|-------------|
| PostgreSQL | `localhost:5432`, DB `photoffice` | `photoffice` / `photoffice` |
| SeaweedFS S3 API | http://localhost:8333, bucket `photoffice` | `photoffice` / `photoffice-dev-secret` |
| Keycloak | http://localhost:8180 (realm `photoffice`) | admin console: `admin` / `admin` |
| imgproxy | http://localhost:8081 (signed URLs only; key/salt in `docker-compose.yml`) | – |
| Mailpit (catches all e-mails) | http://localhost:8025 (SMTP `localhost:1025`) | – |

The backend reads `DB_URL`, `DB_USER`, `DB_PASSWORD`, `OIDC_ISSUER_URI` (defaults match the compose setup).
**Running the backend jar while building:** `./mvnw verify/package` overwrites `target/*.jar`; a backend started
from that file then fails with `NoClassDefFoundError` (looks like "login broken" in the UI). For demos start from a
**fresh copy per start** and record the real Java PID – `nohup … &` as its own statement, not at the end of an
`&&` chain (then `$!` is the PID of a subshell):
```bash
JAR=/tmp/backend-$(date +%s).jar; cp backend/target/photoffice-backend-*.jar "$JAR"
nohup java -jar "$JAR" --spring.profiles.active=dev > /tmp/backend.log 2>&1 &
echo $! > /tmp/backend.pid      # stop with: kill "$(cat /tmp/backend.pid)"
```
Check with `ss -ltnp | grep :8080` that the PID really owns the port. Don't `pkill -f` with patterns from your own command.

Profile `dev` adds the studios matching the Keycloak dev realm (Liquibase context `dev`); never enable it in production.
Local database still from the Flyway era? Reset it once: `docker compose down -v`.

### Dev users (Keycloak realm `photoffice`, password = username)
| User | Role | Studio (organization) |
|------|------|-----------------------|
| `operator` | `platform-admin` | – |
| `admin-a` | `studio-admin` | `studio-a` |
| `foto-a` | `photographer` | `studio-a` |
| `admin-b` | `studio-admin` | `studio-b` |

Token for API experiments: `curl -d grant_type=password -d client_id=photoffice-dev-cli -d username=admin-a -d password=admin-a http://localhost:8180/realms/photoffice/protocol/openid-connect/token`

---

## 3. Testing

```bash
cd backend && ./mvnw verify                # JUnit + Testcontainers (Docker required), Modulith structure check
cd frontend && npm test -- --watch=false   # Vitest
cd frontend && npm run build
```
CI (`.github/workflows/ci.yml`) runs the same commands on every PR and on `main`.

### End-to-end tests (Playwright, local only for now)
Start the full stack (compose, backend with profile `dev`, `npm start`), then:
```bash
cd frontend && npx playwright install chromium   # once per machine / Playwright version
cd frontend && npm run e2e
```
Keycloak with organizations asks for username and password on two separate pages (see `e2e/studio-login.spec.ts`).

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
- Database: schema only via Liquibase changesets in `backend/src/main/resources/db/changelog/changes/`
  (see "Parallel work", ADR 0006); Hibernate runs with `ddl-auto: validate`. Use `splitStatements:false` for
  changesets containing `$$` function bodies. Test data for local development: `db/changelog/devdata/` with `context:dev`.
- Money: never floating point.

### Multi-tenancy (module `tenant`, issue #5)
- A studio is a **tenant**. Every business table has a `tenant_id UUID NOT NULL` column and is secured in its
  changeset with `SELECT enable_tenant_isolation('<table>');` (PostgreSQL row-level security).
  Global tables are the exception and must be listed in `TenantIsolationCoverageTests.GLOBAL_TABLES` – that test
  fails for any unsecured table.
- At runtime the app connects via `TenantAwareDataSource`: every connection runs as role `photoffice_app`
  (not owner, not superuser → RLS applies) with `app.tenant_id` set from `TenantContext`. Liquibase runs as owner.
- Bind the tenant with `TenantContext.runAs(tenantId, ...)` / `callAs(...)` **before** a transaction starts.
  Without a bound tenant no tenant rows are visible and inserts fail. For HTTP requests the
  `TenantContextFilter` binds the tenant from registered `TenantResolver` beans.
- The binding is not inherited by other threads: asynchronous event listeners take the `TenantId` from the event.
- Still set `tenant_id` explicitly when writing (`TenantContext.require()`); RLS is the safety net, not the only check.
- Tests: `TenantIsolationTests` shows how to verify isolation with a schema-owner connection.

### Authentication & authorization (module `identity`, issue #6)
- Keycloak issues the tokens; the backend is an OAuth2 resource server (issuer + audience `photoffice-api` checked).
- Realm roles → authorities: `platform-admin` → `ROLE_PLATFORM_ADMIN`, `studio-admin` → `ROLE_STUDIO_ADMIN`,
  `photographer` → `ROLE_PHOTOGRAPHER` (constants in `Roles`).
- **Studio = Keycloak organization; organization alias = tenant slug.** A studio user must belong to exactly one
  organization whose tenant is `ACTIVE`; that tenant is bound to the request (`OrganizationTenantResolver`).
- URL rules in `SecurityConfiguration`: `/api/platform/**` platform admin, `/api/studio/**` active studio member,
  public endpoints explicitly listed, **everything else is denied** – new endpoint groups must be added there.
- Realm configuration is code: `infra/keycloak/photoffice-realm.json` (dev realm, imported by compose and by
  `KeycloakIntegrationTests`). The client `photoffice-dev-cli` (password grant) exists for development and tests only.
- Tests: `TestTokens` builds Keycloak-like tokens for MockMvc.

### Feature recipe (reference: customers, issue #7)
Copy the structure of the customer feature for new business features:
1. **API:** add paths/schemas to `api/openapi.yaml` in an own `# --- <feature> (#nr) ---` block under `/studio/...`;
   list endpoints take `search`, `page`, `size` and return `{items, page, size, totalElements}`.
2. **Database:** changeset `changes/<timestamp>-<feature>.sql` with `tenant_id UUID NOT NULL REFERENCES tenant(id)`
   and `SELECT enable_tenant_isolation('<table>');`; dev data in `devdata/` for studio A/B (fixed UUIDs).
3. **Backend module** `de.photoffice.<feature>`: entity (sets `tenant_id` from `TenantContext.require()`),
   repository (no tenant filter needed – RLS), `<Feature>Management` service (public API of the module),
   controller implementing the generated `*Api` with module-specific `@ExceptionHandler`s (problem details,
   German `detail` texts shown to users), domain events for other modules (e.g. `CustomerDeleted`).
   Generic errors (parameter constraint violations) are handled in `de.photoffice.web.ApiExceptionHandler`.
4. **Backend tests:** `<Feature>ApiTests` (CRUD, validation, conflicts, 404) and `<Feature>IsolationTests`
   (studio B can neither see nor change studio A's data) with `TestTokens`.
5. **Frontend:** list page (`PageHeader`, `SearchField`, table, paginator, `EmptyState`, state in the URL) and
   form page (create/edit, `FieldError`, 409 → error at the field, `ConfirmService` before delete);
   **validate the format of every field** (not only required/length) with the same rules in frontend and backend
   (see `customer-validators.ts` / `CustomerData.java`), `trimmedPattern(...)` + field-specific `[patternHint]`;
   errors show while typing (`ShowOnDirtyErrorStateMatcher` in the studio defaults);
   child routes under `studio` + navigation entry.
6. **Frontend tests:** unit tests with `HttpTestingController`; Playwright spec in `frontend/e2e/` incl. a
   cross-studio check; then the guided demo.

### Studio onboarding (module `identity`, issue #24, ADR 0007)
- Registering a studio publishes `TenantRegistered`; `StudioOnboarding` creates the Keycloak organization
  (name = alias = slug, studio name as description), the first studio admin (role `studio-admin`, member) and sends
  Keycloak's "set password" e-mail. `TenantResponse.onboardingStatus` is `COMPLETED` afterwards.
- Suspend/reactivate: `POST /api/platform/tenants/{id}/suspend|reactivate` → `TenantStatusChanged` → organization
  disabled/enabled in Keycloak.
- Keycloak Admin API only via `KeycloakAdminClient` (service account client `photoffice-backend`, settings
  `photoffice.keycloak.*`). Listeners on module events must be **idempotent**: failed deliveries are resubmitted by
  `EventResubmission` (`photoffice.events.resubmission.*`).
- Tests run with `photoffice.onboarding.enabled=false` (`src/test/resources/application.properties`);
  `StudioOnboardingIntegrationTests` switches it on against Keycloak + Mailpit containers.

### Frontend
- Standalone components, signals, new control flow (`@if`, `@for`); zoneless as generated by Angular CLI 22.
  Components only re-render on signal/input changes – derive template state from signals (see `FieldError`).
- Login via `angular-oauth2-oidc` (code flow + PKCE, `src/app/auth/`); the access token is attached to `/api/` calls.
  OIDC settings are in `auth.config.ts` (localhost values for now).
- Unit tests: import `iconTesting` and use `fakeAuthService()` from `src/testing/test-providers.ts`; `settle()` from
  `src/testing/settle.ts` waits for API promises.

### Frontend UI building blocks (Angular Material, issue #27, ADR 0005)
- **Design:** theme in `src/styles.scss` (palettes in `src/styles/_theme-colors.scss`, generated with
  `ng generate @angular/material:theme-color`): primary petrol `#1F4E5F`, tertiary copper `#C8794A`, warm neutral,
  font Inter (self-hosted). Use the system tokens (`var(--mat-sys-primary)`, `var(--mat-sys-title-large)`, …) –
  no hard-coded colours or font sizes. Buttons: `matButton="filled"` (main action), `matButton="outlined"`/`matButton`
  (secondary), `matIconButton` with `aria-label`.
- **Icons:** `<mat-icon svgIcon="<name>" />` with names from Material Symbols outlined
  (`node_modules/@material-symbols/svg-400/outlined`); each icon is a small SVG loaded on demand. No icon font.
- **Layout:** public pages live under `PublicShell`, studio pages are child routes of `studio` (rendered in
  `StudioShell`: top bar, user menu, side navigation). New studio page = child route in `app.routes.ts` +
  entry in `layout/studio-navigation.ts`. The current user/studio: `inject(StudioSession).user()`.
- **Shared blocks** (`src/app/shared/ui`, import from `../shared/ui`):
  | Block | Use for |
  |-------|---------|
  | `PageHeader` | Page title, subtitle, page actions (`[actions]` slot) – every page starts with it |
  | `SearchField` | Search above lists (debounced `search` output) – search server-side, reset to page 0 |
  | `mat-table` + `mat-paginator` in `<div class="table-scroll">` | Lists (German paginator labels are provided by `StudioShell`) |
  | `EmptyState` | No entries / no search results |
  | `LoadingIndicator` | While loading |
  | `FieldError` | `<mat-error><app-field-error [control]="form.controls.x" /></mat-error>` – German validation messages |
  | `ConfirmService.confirm({...destructive: true})` | Before deleting or other irreversible actions |
  | `NotificationService.success()/error(err)` | Feedback after actions; `error()` shows the backend's problem detail |
  | `apiErrorMessage(err)` | German message for a failed API call |
- **Reference page:** `/studio/ui-bausteine` (dev builds only, `studio/ui-showcase/`) shows all blocks in a
  realistic list + form – copy from there.
- **Accessibility:** skip link, visible focus, labels on icon buttons, `aria-label` on landmarks; check keyboard use.
- **Bundle:** keep heavy Material modules out of public pages (they are lazy via the studio route); the
  production build warns above 500 kB initial.

### Language
- Code identifiers in **English**; domain documentation, ADRs, journal, and issues in German.
- Avoid copying legacy patterns (see `docs/analysis/legacy-features.md`, section 5).
