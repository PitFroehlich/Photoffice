# 0006: Liquibase statt Flyway für Datenbankänderungen

- **Status:** Accepted
- **Date:** 2026-10-08
- **Ändert:** ADR 0003 (Punkt "Schema-Migrationen mit Flyway")

## Context
Ab jetzt arbeiten mehrere Agenten parallel an verschiedenen Branches. Mit Flyway brauchte jede Migration eine
eindeutige, aufsteigende Versionsnummer; parallele Branches mussten Zeitstempel verwenden und `out-of-order`
erlauben. Der Nutzer möchte den Verwaltungsaufwand verringern.

## Decision
- **Liquibase** verwaltet das Datenbankschema (Spring Boot `spring-boot-starter-liquibase`).
- Master-Changelog `db/changelog/db.changelog-master.yaml` bindet per `includeAll` alle Dateien aus
  `db/changelog/changes/` ein und wird für neue Änderungen **nicht** bearbeitet → keine Merge-Konflikte.
- Neue Änderungen: eine Datei `changes/<yyyyMMddHHmm>-<beschreibung>.sql` im Format *Liquibase formatted SQL*,
  Changesets mit eindeutiger ID. Liquibase führt jedes noch nicht ausgeführte Changeset aus – die Reihenfolge des
  Mergens spielt keine Rolle.
- Testdaten für die lokale Entwicklung als Changesets mit Kontext `dev` (`db/changelog/devdata/`), nur im Profil `dev`.
- Liquibase läuft wie zuvor Flyway als Schema-Besitzer, die Anwendung als eingeschränkte Rolle (RLS, ADR 0003/#5).

## Consequences
- Kein Versionsnummern-Management mehr; parallele Branches kollidieren nicht im Master-Changelog.
- Bereits ausgeführte Changesets dürfen nicht geändert werden (Prüfsumme) – Korrekturen als neues Changeset.
- Lokale Datenbanken mit Flyway-Historie müssen einmal zurückgesetzt werden: `docker compose down -v`.
