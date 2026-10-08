# Studio bearbeiten und Onboarding mit korrigierten Admin-Daten wiederholen

- **Date:** 2026-10-09
- **Machine / Agent:** fin-de-nb-0061 / Claude Code (Worktree, parallel zu #46)
- **Issue / PR:** #42 / #47
- **Branch:** feature/42-studio-bearbeiten

## Goal
Der Plattform-Betreiber kann Name und – bis zum Abschluss des Onboardings – den ersten Studio-Admin eines Studios
ändern; ein fehlgeschlagenes Onboarding läuft danach mit den korrigierten Daten.

## Done
- API: `GET`/`PATCH /platform/tenants/{tenantId}` (`UpdateTenantRequest`), `TenantResponse.adminEmail/
  adminFirstName/adminLastName`.
- Changeset `202610091042-tenant-initial-admin.sql`: Spalten `admin_*` und `version`; Übernahme der Admin-Daten aus
  offenen `TenantRegistered`-Zustellungen; `admin` aus abgeschlossenen Zustellungen entfernt.
- Modul `tenant`: `Tenant.initialAdmin()`/`changeInitialAdmin`/`rename`, `@Version`; `TenantManagement.update`;
  Event `TenantRenamed`; `TenantRegistered` ohne Admin-Daten (Feld bleibt für alte Zustellungen);
  `OnboardingCompletedException` (409); deutsche Meldungen für leeren Namen und ungültige Admin-E-Mail.
- Modul `identity`: `StudioOnboarding` liest die Admin-Daten vom Studio (Rückfall: Event), gleicht die
  Organisationsbeschreibung an, Listener für `TenantRenamed`; `KeycloakAdminClient.setOrganizationDescription`.
- Frontend: `StudioForm` für Registrieren und Bearbeiten (`/plattform/studios/:id`), „Bearbeiten“ und
  „Daten korrigieren“ in der Studio-Liste.
- ADR 0012 (0007/0010 als teilweise geändert markiert), AGENTS.md „Platform area“, Demo
  `docs/demos/0042-studio-bearbeiten.md`.
- Tests (auf main mit #8): Backend 311 (+15: 6 Controller-Tests, `StudioOnboardingAdminDataTests` 5,
  `TenantRegisteredCompatibilityTests` 2, 2 Szenarien in `StudioOnboardingIntegrationTests`), Frontend 133 Unit
  (+5), Playwright 42 (+2 in `platform.spec.ts`).

## Open / Next steps
- Keycloak-Benutzer, die ein früherer Versuch mit falscher E-Mail angelegt hat, bleiben bestehen (ADR 0012).
- Suche/Seiten der Studio-Liste: #45 (baut auf diesem Branch auf).

## Pitfalls
- Hibernate schreibt ohne `@DynamicUpdate` die ganze Zeile: Ein gleichzeitiges Umbenennen hätte `onboarded_at`
  wieder auf `NULL` setzen können → `@Version`. In `StudioOnboardingIntegrationTests` (Wiederholung alle 2 s) kann
  das `PATCH` deshalb selten 409 bekommen; der Test wiederholt es.
- Jackson-Format der Events: `{"tenantId":{"value":"…"},…,"admin":{"email":…}}` – darauf baut die Übernahme im
  Changeset auf (fehlerhafte Einträge werden übersprungen).
- Playwright: `studio-profile.spec.ts` schlug sporadisch fehl („legal text with live preview …“, einmal
  „photographers see profile …“ mit zwei `h1`) – nicht von #42 berührt; jeweils zwei Läufe danach grün.
- ADR-Nummer: 0010 war reserviert, main hat aber inzwischen die Plattform-ADR auf 0010 umnummeriert → 0012.
- Sandbox im Worktree: verschachtelte Shell-Konstrukte (Heredoc nach `cd`, Schleifen mit Variablen) werden
  abgelehnt; Dateien mit Write/Edit anlegen, Befehle einzeln ausführen.
