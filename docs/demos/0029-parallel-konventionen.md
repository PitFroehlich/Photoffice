# Demo #29: Konventionen für parallele Arbeit, Liquibase, geführte Demos

- **Issue / PR:** #29 / #30
- **Datum:** 2026-10-08
- **Agent / Maschine:** Claude Code / fin-de-nb-0072

## Was wurde umgesetzt
- **Liquibase statt Flyway** (ADR 0006): neue Datenbankänderungen sind einfach neue Dateien in
  `db/changelog/changes/`; keine Versionsnummern, keine Konflikte im Master-Changelog. Testdaten über den Kontext `dev`.
- **AGENTS.md:** Regeln für parallele Arbeit (gemeinsame Dateien, Modulgrenzen, Rebase) und die **geführte Demo**
  nach jedem Issue (Schritt für Schritt durch die laufende Anwendung, keine Screenshots).
- **ADR 0005:** Angular Material mit eigenem Design.
- Roadmap #21 neu geordnet: #27 (UI-Grundlage) blockiert die Oberflächen-Issues; #28 (Deployment) neu.

Dieses Issue ändert keine Oberfläche – die Demo zeigt die neue Datenbankverwaltung.

## Vorbereitung
1. Einmalig alte lokale Datenbank (Flyway) verwerfen: `docker compose down -v`
2. `docker compose up -d postgres keycloak`
3. `cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`

## Schritt für Schritt

### Schritt 1: Liquibase hat das Schema angelegt
- **Was tun:**
  ```bash
  docker compose exec -T postgres psql -U photoffice -c "select id, filename from databasechangelog order by orderexecuted"
  ```
- **Was du siehst:** 7 Einträge – `event-publication`, fünf `tenant-*`-Changesets und `dev-tenants` aus `devdata/`.

### Schritt 2: Testdaten nur im Profil dev
- **Was tun:**
  ```bash
  docker compose exec -T postgres psql -U photoffice -c "select slug, name from tenant"
  ```
- **Was du siehst:** `studio-a` und `studio-b`. Ohne Profil `dev` (z. B. in den Tests) wird `dev-tenants` nicht ausgeführt.

### Schritt 3: Anwendung läuft wie zuvor
- **Was tun:** `curl -s localhost:8080/actuator/health`
- **Was du siehst:** `{"groups":["liveness","readiness"],"status":"UP"}`. Die Anmeldung aus Demo #6 funktioniert unverändert.

### Schritt 4: So legt ein Agent künftig eine Tabelle an
- **Was tun:** `backend/src/main/resources/db/changelog/db.changelog-master.yaml` öffnen.
- **Was du siehst:** `includeAll` auf `changes/` – eine neue Datei `changes/<zeitstempel>-<name>.sql` wird automatisch übernommen, der Master bleibt unverändert.

## Automatisch abgesichert
- Backend-Suite (37 Tests) läuft mit Liquibase, inkl. Mandantentrennung und Prüfung, dass jede Tabelle abgesichert ist.

## Noch offen / Einschränkungen
- Andere Rechner mit lokaler Flyway-Datenbank müssen einmal `docker compose down -v` ausführen.

## Aufräumen
Backend beenden (Strg+C), `docker compose down`.
