# Studio-Onboarding: Keycloak-Organisation und erster Studio-Admin automatisch

- **Date:** 2026-10-08
- **Machine / Agent:** fin-de-nb-0061 / Claude Code
- **Issue / PR:** #24 / #33
- **Branch:** feature/24-studio-onboarding

## Goal
Beim Registrieren eines Studios entsteht automatisch alles, was es zum Arbeiten braucht: Keycloak-Organisation,
erster Studio-Admin mit Einladung (kein Passwort per Mail), Konsistenz zwischen DB und Keycloak, Sperren/Freischalten.

## Done
- API (`api/openapi.yaml`): `CreateTenantRequest` + `adminEmail` (Pflicht), `adminFirstName`, `adminLastName`;
  `TenantResponse.onboardingStatus` (`PENDING`/`COMPLETED`); `POST /platform/tenants/{tenantId}/suspend|reactivate`.
- Modul `tenant`: Events `TenantRegistered`, `TenantStatusChanged`; `InitialStudioAdmin`; `suspend`, `reactivate`,
  `markOnboarded`; Spalte `tenant.onboarded_at` (Changeset `202610081500-tenant-onboarding.sql`, Dev-Studios per
  `devdata` als onboarded markiert).
- Modul `identity`: `KeycloakAdminClient` (RestClient + Client Credentials), `StudioOnboarding` (idempotente
  `@ApplicationModuleListener`), `StudioOnboardingConfiguration` (abschaltbar mit `photoffice.onboarding.enabled`).
- Modul `system`: `EventResubmission` stellt fehlgeschlagene Event-Zustellungen alle 5 Minuten erneut zu;
  `spring.modulith.events.staleness.*` gesetzt.
- Dev-Realm: Client `photoffice-backend` (Service-Account, `realm-management`-Rollen per Scope-Mapping), SMTP auf Mailpit.
  `docker-compose.yml`: Mailpit (http://localhost:8025).
- ADR 0007, AGENTS.md-Abschnitt "Studio onboarding", Demo `docs/demos/0024-studio-onboarding.md`.
- Tests: Backend 47 (neu u. a. `StudioOnboardingIntegrationTests` mit Keycloak + Mailpit, 5 neue Controller-Tests),
  Frontend 26 Unit, Playwright 10 (neu `studio-onboarding.spec.ts`; `loginAs` hat einen optionalen Passwort-Parameter).

## Open / Next steps
- Oberfläche für den Plattform-Betreiber (Studios registrieren, sperren, Onboarding-Status, Onboarding-Fehler sichtbar, 404 statt leerem 401 bei ungültiger ID) – #35.
- Keycloak-Theme/Locale Deutsch für Login-Seiten und Mails – #36.
- Produktion: SMTP im Realm, Client-Secret, `FRONTEND_URL`, `KEYCLOAK_URL` (#28).

## Pitfalls
- Keycloak verlangt **eindeutige Organisationsnamen**; parallele Anlage mit gleichem Namen endet sogar in HTTP 500.
  Deshalb Name = Slug, Studioname in `description`.
- Organisationssuche: `search=` sucht nur in Name/Domain, nicht im Alias. `q=alias:<slug>` funktioniert (exakt).
- Mit `fullScopeAllowed=false` fehlen die `realm-management`-Rollen im Service-Account-Token (403) → `clientScopeMappings`.
- Organisations-Admin-API braucht `manage-realm` (ohne: 403).
- `@ApplicationModuleListener` ist `@Async`: ein direkter Aufruf im Test wirft keine Exception. Fehler stattdessen über
  `event_publication` (`completion_attempts`, `status`) prüfen.
- Fehlen Vor-/Nachname, fragt Keycloak sie nach dem Passwort ab (Required Action "Update Account Information").
- Deaktivierte Organisation: Login funktioniert, aber das Token enthält kein `organization`-Claim.
- Auf dieser Maschine gibt es kein `25.0.2-tem` aus `.sdkmanrc`; `JAVA_HOME=~/.sdkman/candidates/java/25.0.2-oracle` geht.
- `KeycloakAdminClient` cacht das Service-Account-Token. Nach einem Neuanlegen des Dev-Keycloak (neue Schlüssel) kam
  401 → bei 401 wird das Token verworfen und der Aufruf einmal wiederholt. `logout-all` reproduziert das **nicht**
  (Client-Credentials-Tokens bleiben gültig); der Test löscht deshalb den RSA-Key-Provider.
