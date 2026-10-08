# 0007: Studio-Onboarding in Keycloak über Events und Service-Account

- **Status:** Accepted
- **Date:** 2026-10-08

## Context
Registriert der Plattform-Betreiber ein Studio (`POST /api/platform/tenants`), braucht das Studio eine
Keycloak-Organisation (Alias = Slug, siehe #6) und einen ersten Studio-Admin. Bisher wurde beides von Hand angelegt.
Datenbank und Keycloak lassen sich nicht in einer gemeinsamen Transaktion ändern. Keycloak kann beim Registrieren
nicht erreichbar sein. Passwörter dürfen nicht per E-Mail verschickt werden (Issue #24).

## Decision
- **Event statt direktem Aufruf:** `TenantManagement.register` speichert das Studio und veröffentlicht
  `TenantRegistered` (mit E-Mail und Name des ersten Admins). `StudioOnboarding` im Modul `identity` reagiert darauf
  als `@ApplicationModuleListener`, also asynchron und erst nach dem Commit. Spring Modulith speichert jede Zustellung
  in `event_publication`.
- **Idempotent und wiederholbar:** Jeder Schritt prüft zuerst, was schon existiert: Organisation (Suche per
  `q=alias:<slug>`), Benutzer (per E-Mail), Rolle, Mitgliedschaft (409 = schon Mitglied). Erst am Ende wird
  `tenant.onboarded_at` gesetzt, und die API meldet `onboardingStatus` `PENDING`/`COMPLETED`. Schlägt ein Schritt fehl,
  setzt Modulith die Zustellung auf `FAILED`. `EventResubmission` (Modul `system`) stellt fehlgeschlagene Zustellungen
  alle 5 Minuten erneut zu. Zustellungen, die zu lange hängen (`spring.modulith.events.staleness.*`, 10 Minuten),
  gelten ebenfalls als fehlgeschlagen.
- **Einladung statt Passwort:** Der Benutzer wird ohne Passwort angelegt, mit den Required Actions `UPDATE_PASSWORD`
  und `VERIFY_EMAIL`. Keycloak verschickt die Mail mit dem Link (`execute-actions-email`, 72 h gültig, danach zurück
  zum Frontend). Wer nach einem Fehler noch kein Passwort gesetzt hat, bekommt die Mail bei der Wiederholung erneut.
- **Organisationsname = Slug:** Keycloak verlangt eindeutige Organisationsnamen, Studionamen sind aber nicht eindeutig.
  Der Studioname steht deshalb in der Beschreibung der Organisation.
- **Ein Benutzer, ein Studio:** Gehört die E-Mail schon einem Mitglied eines anderen Studios, schlägt das Onboarding
  fehl (Log-Meldung, Status bleibt `PENDING`) und wird wiederholt, bis der Konflikt gelöst ist.
- **Sperren:** `POST /api/platform/tenants/{id}/suspend` bzw. `/reactivate` ändern den Status und veröffentlichen
  `TenantStatusChanged`. Das Backend lehnt gesperrte Studios ohnehin ab (#6). Zusätzlich wird die Organisation in
  Keycloak deaktiviert: Neue Tokens enthalten dann kein `organization`-Claim mehr.
- **Service-Account:** Vertraulicher Client `photoffice-backend` mit den `realm-management`-Rollen `manage-users`,
  `view-users`, `query-users` und `manage-realm`. `manage-realm` ist nötig, weil die Organisations-API von Keycloak
  26 es verlangt (geprüft: 403 ohne). Wegen `fullScopeAllowed=false` stehen die Rollen per Scope-Mapping im Token.
  Aufgerufen wird die Admin-API mit Springs `RestClient` (`KeycloakAdminClient`), ohne die Bibliothek
  `keycloak-admin-client` und ohne zusätzliche Abhängigkeit.
- **Lokal:** Mailpit (`docker-compose.yml`, http://localhost:8025) fängt alle Mails ab; der Dev-Realm sendet dorthin.

## Consequences
- Registrieren bleibt schnell und funktioniert auch, wenn Keycloak gerade nicht erreichbar ist. Das Studio wird
  nachgezogen, sobald Keycloak wieder läuft.
- Listener auf diese Events müssen idempotent bleiben, weil sie mehrfach zugestellt werden können.
- `manage-realm` ist weitreichend. Das Client-Secret ist in Produktion ein echtes Geheimnis
  (`KEYCLOAK_ADMIN_CLIENT_SECRET`, siehe #28).
- Produktion braucht einen SMTP-Server im Realm und die richtige Frontend-URL (`FRONTEND_URL`) für den Link (#28).
- Studios, deren Onboarding dauerhaft scheitert, sind nur über Log und `onboardingStatus` sichtbar. Eine
  Betreiber-Oberfläche dafür gibt es noch nicht.
