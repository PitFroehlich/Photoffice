# Studio-Login und Rollen mit Keycloak

- **Date:** 2026-10-08
- **Machine / Agent:** fin-de-nb-0072 / Claude Code
- **Issue / PR:** #6 / (siehe PR zu #6)
- **Branch:** feature/6-studio-login

## Goal
Studio-Benutzer melden sich über Keycloak an; Studio-Zugehörigkeit und Rolle steuern alle Zugriffe. Plattform-Endpunkte aus #5 absichern.

## Done
- Keycloak-Realm als Code (`infra/keycloak/photoffice-realm.json`): Organisations-Feature an, Realm-Rollen `platform-admin`, `studio-admin`, `photographer`, Clients `photoffice-frontend` (Code Flow + PKCE), `photoffice-api` (Audience), `photoffice-dev-cli` (nur Dev/Tests), Dev-Benutzer und Organisationen `studio-a`/`studio-b`, `fullScopeAllowed=false` mit Scope-Mappings.
- Backend-Modul `identity`: Resource Server (Issuer + Audience), Rollen-Mapping, `StudioMembership` (Organisations-Alias = Tenant-Slug, genau eine Organisation, Studio aktiv), `OrganizationTenantResolver` (bindet Mandant), `SecurityConfiguration` (Default deny), `GET /api/studio/me`.
- Profil `dev` mit Testdaten (`db/devdata/R__dev_tenants.sql`) passend zum Dev-Realm.
- Frontend: `angular-oauth2-oidc`, `AuthService`, `studioGuard`, Startseite + Studio-Bereich, Token nur für `/api/`.
- Tests: Backend 37 (u. a. 11 Zugriffsregeln mit simulierten Tokens, 6 gegen echten Keycloak-Container mit der Realm-Datei), Frontend 8, Playwright-E2E 5 (lokal gegen laufenden Stack).
- Folge-Issues: #24 Studio-Onboarding (Keycloak-Organisation automatisch), #25 E2E in CI. Abhängigkeiten und Roadmap (#21) aktualisiert.

## Open / Next steps
- Stufe 4 ist frei: #7, #13, #20, #24, #25 (parallel möglich).
- Frontend-OIDC-Konfiguration ist fest auf localhost – Laufzeitkonfiguration kommt mit dem Deployment (noch kein Issue dafür).
- Datenzugriffe zwischen Studios werden ab #7 mit echten Fachdaten getestet (RLS aus #5 + Mandant aus dem Token).

## Pitfalls
- Keycloak mit Organisationen zeigt den Login zweistufig (erst Benutzername, dann Passwort) – E2E-Test entsprechend.
- Nach dem Login bereinigt die OIDC-Bibliothek die URL per `replaceState`; `waitForURL` greift nicht, besser auf sichtbare Elemente warten.
- Playwright-Version und Browser-Build müssen passen: `npx playwright install chromium`.
- `pgrep -f`/`pkill -f` mit einem Muster, das im eigenen Befehl steht, trifft die eigene Shell → Muster wie `[j]ava -jar` verwenden.
