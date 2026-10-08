# Mandanten-Modell und Row-Level Security

- **Date:** 2026-10-08
- **Machine / Agent:** fin-de-nb-0072 / Claude Code
- **Issue / PR:** #5 / (siehe PR zu #5)
- **Branch:** feature/5-mandanten-modell

## Goal
Studios als Mandanten; Mandantentrennung zentral in der Anwendung und zusätzlich per PostgreSQL Row-Level Security.

## Done
- Modul `tenant` (`backend/src/main/java/de/photoffice/tenant`):
  - `Tenant` (Studio: id, slug, name, status, createdAt), `TenantManagement` (registrieren, auflisten), Plattform-Endpunkte `GET/POST /api/platform/tenants` (in `api/openapi.yaml`).
  - `TenantContext` (Java-25-`ScopedValue`), `TenantResolver` (Schnittstelle, Implementierung folgt in #6), `TenantContextFilter`.
  - `TenantAwareDataSource`: jede Verbindung `SET ROLE photoffice_app` + `app.tenant_id`; Flyway nutzt per Customizer die unverpackte DataSource (Schema-Besitzer).
- Migration `V2__tenant.sql`: Rolle `photoffice_app`, Grants/Default-Privileges, Funktionen `current_tenant_id()` und `enable_tenant_isolation(regclass)`, Tabelle `tenant`.
- Tests: `TenantIsolationTests` (7, Probe-Tabelle, Abfragen ohne Filter), `TenantIsolationCoverageTests` (jede Tabelle abgesichert oder explizit global), `PlatformTenantControllerTests`, `TenantContextTests`.
  Gegenprobe: ohne RLS schlagen 6/7 Isolationstests fehl; eine ungesicherte Tabelle lässt den Coverage-Test mit klarer Meldung scheitern.
- AGENTS.md: Abschnitt "Multi-tenancy" mit den Regeln für neue Tabellen und den Mandantenkontext.

## Open / Next steps
- #6: `TenantResolver` aus dem Keycloak-Token implementieren; `/api/platform/**` auf die Rolle des Plattform-Betreibers beschränken (aktuell ungeschützt, TODO im `PlatformTenantController`).
- Plattweite Hintergrundjobs (z. B. Speicherverbrauch aller Studios) brauchen später einen bewussten Weg über alle Mandanten – bisher absichtlich nicht vorhanden.

## Pitfalls
- Spring Modulith legt `event_publication` standardmäßig selbst an – als App-Rolle schlägt das fehl. Deshalb `spring.modulith.events.jdbc.schema-initialization.enabled=false`, Schema nur aus Flyway.
- Superuser (lokal und in Testcontainers) umgehen RLS immer – daher der Rollenwechsel per `SET ROLE`.
- Session-Einstellungen werden bei der Verbindungsausgabe im Autocommit gesetzt; innerhalb einer Transaktion würde ein Rollback sie zurücksetzen.
- `pkill -f <muster>` beendet auch die eigene Shell, wenn das Muster im Befehl vorkommt.
