# Plattform-Oberfläche: Studios registrieren, sperren, Onboarding-Fehler sehen

- **Date:** 2026-10-08
- **Machine / Agent:** fin-de-nb-0061 / Claude Code (parallel zu #36 in eigenem Worktree)
- **Issue / PR:** #35 / #37
- **Branch:** feature/35-plattform-oberflaeche

## Goal
Der Plattform-Betreiber verwaltet Studios in einer Oberfläche statt per `curl`, sieht fehlgeschlagene Onboardings
mit Grund, und eine leere Studio-ID im Pfad liefert kein leeres 401 mehr.

## Done
- API: `TenantResponse.onboardingError`/`onboardingFailedAt`, `POST /platform/tenants/{id}/onboarding/retry` (202),
  400-Antworten an suspend/reactivate.
- Backend: Changeset `202610081700-tenant-onboarding-error.sql`; `TenantManagement.recordOnboardingFailure`
  (`REQUIRES_NEW`) und `retryOnboarding` (`FailedEventPublications.resubmit` mit Filter); `StudioOnboarding` speichert
  einen deutschen Grund und wirft weiter; deutsche Meldungen für vergebenes/ungültiges Kürzel, unbekannte und
  ungültige Studio-ID; `ProblemDetailRequestRejectedHandler` + `ERROR`-Dispatch erlaubt.
- Frontend: Bereich `/plattform` (`platformGuard`, `PlatformShell`, `platform/studio-list`, `platform/studio-form`),
  Rollen aus dem Access-Token im `AuthService`, `studioGuard` leitet den Betreiber weiter, „Plattform-Login“ in der
  öffentlichen Shell.
- ADR 0008, AGENTS.md-Abschnitt "Platform area", Demo `docs/demos/0035-plattform-oberflaeche.md`.
- Tests: Backend 119 (neu: 4 Controller-Tests, `TenantOnboardingRetryTests` 3, `StudioOnboardingFailureReasonTests` 4,
  erweitert `StudioOnboardingIntegrationTests`), Frontend 62 Unit, Playwright 20 (neu `platform.spec.ts` mit 5 Tests;
  `studio-login.spec.ts`: Betreiber landet in der Plattform-Verwaltung; `loginAs` akzeptiert `/plattform`).

## Open / Next steps
- Suche/Seiten in der Studio-Liste, sobald es viele Studios gibt (API liefert alle).
- Studios bearbeiten/löschen gibt es nicht; E2E-Test-Studios bleiben in der lokalen DB.
- Abgelaufenes Token liefert weiterhin 401 ohne Body (nur `WWW-Authenticate`); das Frontend zeigt „Bitte melden Sie
  sich erneut an.“

## Pitfalls
- Ursache des leeren 401 bei `//`: `StrictHttpFirewall` → `HttpStatusRequestRejectedHandler` macht `sendError(400)`
  → Fehlerseite `/error` läuft als `ERROR`-Dispatch ohne Authentifizierung in `denyAll` → 401.
- MockMvc mit URI-Template (`post("/a//b")`) macht aus `//` ein `/` – für solche Tests `post(URI.create(...))`.
- Eine Exception-Handler-Methode für `IllegalArgumentException` im Controller fängt auch
  `MethodArgumentTypeMismatchException` (über die Ursache) – deshalb eigener Handler für den Typ.
- Das generierte `TenantResponse` serialisiert fehlende optionale Felder als `null`.
- `vi.useFakeTimers()` nach dem Start eines `setTimeout` greift nicht; `vi.useFakeTimers({ shouldAdvanceTime: true })`
  vor dem Rendern verwenden, dann funktioniert auch `settle()`.
- `DatePipe` in einer lazy Seite zog Angulars Common-Pipes ins Initial-Bundle (+12 kB, über das 500-kB-Budget);
  Datumsformat deshalb mit `Intl.DateTimeFormat` (`studio-list.ts`). Prüfen mit `ng build --stats-json`.
- Im Worktree weigert sich die Sandbox bei verschachtelten Shell-Konstrukten (`cat > … <<EOF` mit `cd ..`,
  `export X=$(…)`); einfache Befehle bzw. kleine Python-Skripte im Scratchpad verwenden.
