# Kundenverwaltung

- **Date:** 2026-10-08
- **Machine / Agent:** fin-de-nb-0072 / Claude Code
- **Issue / PR:** #7 / (siehe PR zu #7)
- **Branch:** feature/7-kundenverwaltung

## Goal
Studio verwaltet seine Endkunden (Legacy F5), mandantengetrennt.

## Done
- Liquibase: Tabelle `customer` (RLS, E-Mail eindeutig pro Studio, case-insensitive), Dev-Kunden (12 in A, 2 in B).
- API `/api/studio/customers` (Liste mit Suche/Paging, CRUD) in `api/openapi.yaml`.
- Backend-Modul `customer` (Entity, `CustomerData` mit Normalisierung, `CustomerManagement`, Controller, Event `CustomerDeleted`); globaler `ApiExceptionHandler` (Modul `web`) für Parameter-Validierung.
- Frontend: Kundenliste (Suche/Paging serverseitig, Zustand in der URL) und Formular (Anlegen/Bearbeiten/Löschen, 409 am Feld); Navigationseintrag "Kunden".
- Tests: Backend 49 (inkl. 3 Isolationstests – erfüllt das offene Kriterium aus #6), Frontend 35 Unit, 13 E2E.
- AGENTS.md: "Feature recipe" mit Kunden als Referenz. Demo #7.

## Open / Next steps
- #8 Galerien ist frei (Kunden-Zuordnung, auf `CustomerDeleted` reagieren).

## Pitfalls
- Constraint-Verletzungen an Query-Parametern (generierte `@Max` etc.) kamen als 500 – jetzt 400 über `ApiExceptionHandler`.
- LIKE-Suche: `%`/`_` aus der Eingabe escapen (`escape '\'`), sonst findet `%` alles.
