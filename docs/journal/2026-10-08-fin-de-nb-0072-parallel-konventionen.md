# Konventionen für parallele Arbeit, Demo-Pflicht, neue Issues

- **Date:** 2026-10-08
- **Machine / Agent:** fin-de-nb-0072 / Claude Code
- **Issue / PR:** #29 / (siehe PR zu #29)
- **Branch:** chore/29-parallel-konventionen

## Goal
Vorbereitung auf mehrere parallel arbeitende Agenten; Entscheidungen des Nutzers festhalten.

## Done
- Nutzerentscheidungen: Angular Material mit eigenem Design (ADR 0005), ab jetzt mehrere Agenten, Deployment-Issue anlegen, Demo nach jedem Issue.
- Neue Issues: #27 UI-Grundlage (blockiert #7, #13, #20), #28 Deployment, #29 (dieses).
- Abhängigkeiten und Stufen-Labels neu berechnet (alte Stufen-Labels entfernt), Roadmap #21 neu geschrieben.
- AGENTS.md: "Parallel work", "Demo after every finished issue", Ende-der-Session-Schritte (Demo, alles lokal beenden).
- Flyway `out-of-order: true`; neue Migrationen mit Zeitstempel.
- `docs/demos/` mit Vorlage, Demo #6 (nachträglich) und Demo #29; `frontend/scripts/screenshot.mjs`.

## Open / Next steps
- Stufe 4 frei: **#27 zuerst** (blockiert die UI-Issues), parallel #24, #25, #28.

## Pitfalls
- Beim Neuberechnen der Stufen müssen alte `stufe:*`-Labels entfernt werden, sonst trägt ein Issue mehrere.
