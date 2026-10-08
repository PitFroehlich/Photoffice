# UI-Grundlage mit Angular Material

- **Date:** 2026-10-08
- **Machine / Agent:** fin-de-nb-0072 / Claude Code
- **Issue / PR:** #27 / (siehe PR zu #27)
- **Branch:** feature/27-ui-grundlage

## Goal
Einheitliche Oberfläche und Bausteine, bevor mehrere Agenten parallel Masken bauen (ADR 0005).

## Done
- Angular Material 22 + eigenes Theme (`src/styles.scss`, Paletten generiert): Petrol `#1F4E5F`, Kupfer `#C8794A`, warmes Neutral, Inter (self-hosted), High-Contrast-Overrides, Dunkelmodus vorbereitet.
- Icons: Material Symbols als einzelne SVGs (`@material-symbols/svg-400`, Resolver in `provideUiDefaults`) statt 4-MB-Icon-Font.
- `PublicShell`, `StudioShell` (Kopfzeile, Benutzermenü, responsive Seitennavigation, Skip-Link), `StudioSession`, Titel-Strategie, deutsche Locale.
- Bausteine in `shared/ui`: PageHeader, SearchField, EmptyState, LoadingIndicator, FieldError, ConfirmService, NotificationService, apiErrorMessage, deutscher Paginator.
- Referenzseite `/studio/ui-bausteine` (nur Dev-Build).
- Tests: 26 Unit, 9 E2E. AGENTS.md "Frontend UI building blocks". Demo #27, Demo #6 an neues UI angepasst.

## Open / Next steps
- #7, #13, #20 sind nach dem Merge frei (nutzen die Bausteine).
- Dunkelmodus-Umschalter bei Bedarf als eigenes Issue.

## Pitfalls
- Angular 22: Komponenten rendern nur bei Signal-/Input-Änderungen neu – Getter auf Formularzustand aktualisieren sich nicht (E2E fand das bei `FieldError`; jetzt über `statusChanges` → Signal).
- Globale Provider mit Material-Tokens (Paginator, Form-Field, Snackbar) ziehen die Module ins Initial-Bundle (797 kB). Lösung: Studio-Defaults als Provider der lazy geladenen `StudioShell`, Snackbar-Optionen direkt im Service → 486 kB.
- Unit-Tests ohne `provideUiDefaults`: `iconTesting` importieren, sonst Icon-Requests; deutschen Paginator ggf. selbst bereitstellen.
- `src/testing/**` ist aus `tsconfig.app.json` ausgeschlossen (nutzt Vitest-Globals).
