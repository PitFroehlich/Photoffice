# Studio-Liste der Plattform-Verwaltung: Suche, Filter und Seiten

- **Date:** 2026-10-09
- **Machine / Agent:** fin-de-nb-0061 / Claude Code (Worktree, parallel zu #46)
- **Issue / PR:** #45 / (PR folgt)
- **Branch:** feature/45-studio-liste-suche (baut auf feature/42-studio-bearbeiten auf)

## Goal
Die Studio-Liste bleibt mit vielen Studios übersichtlich: serverseitige Suche, Filter und Seiten wie bei den Kunden.

## Done
- API: `GET /platform/tenants` mit `search`, `status`, `onboarding` (`PENDING`/`FAILED`/`COMPLETED`), `page`, `size`
  → `TenantPage` (`items`, `page`, `size`, `totalElements`, `failedOnboardings`).
- Backend: `TenantManagement.search` / `countFailedOnboardings`, `TenantRepository` mit Specifications (Suche in
  Name, Kürzel, Admin-E-Mail; LIKE-Platzhalter maskiert), `OnboardingFilter`; ungültige Filterwerte → 400 mit
  deutschem Text. `findAll` entfernt.
- Frontend: `StudioList` mit `SearchField`, Auswahl Status/Onboarding, „Nur fehlgeschlagene anzeigen“,
  `mat-paginator`, `EmptyState` „Keine Treffer“, Zustand in der URL (`suche`, `status`, `onboarding`, `seite`,
  `anzahl`), Selbstaktualisierung bleibt (nur aktuelle Seite). Das Formular kehrt in dieselbe Listenansicht zurück
  (Navigation-State `listQuery`); nach dem Registrieren zeigt die Liste `?suche=<kürzel>`.
- Aufrufer geprüft: `studio-onboarding.spec.ts` nutzt jetzt `GET /platform/tenants/{id}`; `platform.spec.ts` sucht
  die Zeilen (`searchFor`), weil Test-Studios sich lokal sammeln; `PlatformTenantControllerTests` auf `items`;
  Demos 0024/0035 mit Hinweis ergänzt. `KeycloakIntegrationTests`/`SecurityRulesTests` prüfen nur Statuscodes.
- AGENTS.md „Platform area“, Demo `docs/demos/0045-studio-liste-suche.md`.
- Tests (auf main mit #8): Backend 317 (+6 `PlatformTenantListTests`), Frontend 138 Unit (+5), Playwright 44 (+2 in
  `platform-studio-list.spec.ts`).

## Open / Next steps
- Sortierung nur nach Name; keine sortierbaren Spalten.

## Pitfalls
- Query-Parameter mit `enum` generiert openapi-generator als `String` → eigenes Parsen mit deutscher 400-Meldung.
- Playwright + `mat-select`: Sofort nach der Auswahl erneut öffnen klappt das Panel während der Schließanimation
  wieder zu → warten, bis kein `listbox` mehr da ist (`choose` in `platform-studio-list.spec.ts`). Die Seitengrößen-
  Auswahl des Paginators verdeckt `.mat-mdc-paginator-touch-target`.
- Angulars TestBed nutzt `MockPlatformLocation`: `history.replaceState` erreicht `Location.getState()` nicht –
  stattdessen `TestBed.inject(Location).replaceState(path, '', state)`.
- `prettier --write` formatiert auch fremde Zeilen in den E2E-Dateien (die nicht nach Prettier formatiert sind) –
  nur eigene Dateien formatieren.
