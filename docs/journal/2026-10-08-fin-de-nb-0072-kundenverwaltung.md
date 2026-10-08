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

- Nach Feedback des Nutzers in der Demo: Formatprüfung aller Felder (Name, E-Mail mit Domain-Endung, Telefon, PLZ, Ort, Straße) in Frontend (`customer-validators.ts`) und Backend (`CustomerData`), feldspezifische Hinweise, Fehler schon beim Tippen (`ShowOnDirtyErrorStateMatcher`).
- Zweite Claude-Session (Hintergrund, eigener Worktree unter `.claude/worktrees/`, in `.gitignore`) bearbeitet #13.

- Nutzer-Feedback: Straße muss eine Hausnummer enthalten (4, 4a, 1/2, 10-12; Ziffern im Straßennamen wie "Straße des 17. Juni 135" erlaubt).

## Open / Next steps
- #8 Galerien ist frei (Kunden-Zuordnung, auf `CustomerDeleted` reagieren).

## Pitfalls
- `cmd && … && nohup java … &` schickt die ganze Kette in den Hintergrund – `$!` ist dann nicht die Java-PID, das alte Backend lief weiter und das neue scheiterte am belegten Port. Start als eigene Anweisung, Port-Besitzer prüfen.
- Während der Demo `./mvnw verify` ausgeführt → laufendes Backend-JAR überschrieben → `NoClassDefFoundError`, im UI wie "Login kaputt". Backend für Demos aus einer Kopie der JAR starten (AGENTS.md).
- Nur Pflichtfeld-/Längenprüfung reicht nicht – der Nutzer erwartet Formatprüfung pro Feld (jetzt im Feature-Rezept).
- `Validators.email` akzeptiert `a@b`; eigene Regel mit Domain-Endung verwenden.
- Prozess-PID beim Start in eine Datei schreiben und darüber beenden; `pgrep -f` trifft sonst die eigene Shell, wenn der Startbefehl im selben Aufruf steht.
- Constraint-Verletzungen an Query-Parametern (generierte `@Max` etc.) kamen als 500 – jetzt 400 über `ApiExceptionHandler`.
- LIKE-Suche: `%`/`_` aus der Eingabe escapen (`escape '\'`), sonst findet `%` alles.
