# Projekt-Grundgerüst für den Neubau

- **Date:** 2026-10-08
- **Machine / Agent:** fin-de-nb-0072 / Claude Code
- **Issue / PR:** #4 / (siehe PR zu #4)
- **Branch:** feature/4-projekt-grundgeruest

## Goal
Lauffähiges Grundgerüst gemäß ADR 0003: Monorepo, lokale Infrastruktur, CI, aktualisierte AGENTS.md.

## Done
- Legacy-PHP-Code nach `legacy/` verschoben (inkl. eigener `legacy/AGENTS.md`, Compose-Projektname `photoffice-legacy`).
- `api/openapi.yaml` als API-Vertrag (API-first), Beispiel-Endpunkt `GET /api/system/info`.
- `backend/`: Spring Boot 4.1.1, Java 25, Spring Modulith 2.1.1 (JDBC-Event-Registry), Flyway (V1 = Event-Publication-Tabelle), OpenAPI-Generator (Interfaces), Tests: Kontext mit Testcontainers-Postgres, Modulith-Strukturtest, Controller-Test.
- `frontend/`: Angular 22 (zoneless, Vitest), Client per `ng-openapi-gen` (generiert, git-ignoriert), Startseite zeigt Backend-Version, Proxy auf :8080.
- `docker-compose.yml`: PostgreSQL 18, SeaweedFS 4.48 (+ Bucket-Init), Keycloak 26.8 (Realm `photoffice` mit Frontend-Client), imgproxy v3 (S3-Quelle, signierte URLs).
- CI: `.github/workflows/ci.yml` (Backend `mvnw verify`, Frontend Test + Build).
- ADR 0004: SeaweedFS statt MinIO lokal. AGENTS.md Abschnitte 1–4 auf den neuen Stack umgeschrieben.

Verifiziert lokal: Backend 4/4 Tests, Frontend 2/2 Tests + Build, Backend gegen Compose-Postgres (Health UP, Flyway angewendet), Bild nach S3 hochgeladen und über signierte imgproxy-URL abgerufen (unsigniert → 403).

## Open / Next steps
- #5 Mandanten-Modell kann starten.
- Konvention "Code-Bezeichner auf Englisch" vom Nutzer bestätigt; Lizenz des Neubaus wird später entschieden.
- Playwright (E2E) wird mit der ersten echten Oberfläche eingerichtet.

## Pitfalls
- System-Java ist 21 → mit JDK 25 bauen/starten (`backend/.sdkmanrc`), sonst `UnsupportedClassVersionError`.
- Spring Initializr schreibt `4.1.1.RELEASE` als Boot-Version in die pom.xml – Maven-Version heißt `4.1.1`.
- MinIO-Images nicht mehr verfügbar (ADR 0004).
- `weed shell` braucht Filer-Adresse; Bucket daher per S3-API (curl `--aws-sigv4`) anlegen.
- Lokales `~/.m2/settings.xml` hat ein zusätzliches GitLab-Repository (liefert 401 für unbekannte Artefakte) – stört nicht, solange Versionen korrekt sind.
- Angular-Tests: API-Aufrufe über Promises brauchen vor `whenStable()` einen Makrotask (`settle()` in `app.spec.ts`).
