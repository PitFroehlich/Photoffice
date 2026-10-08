# Eigenes Konto: Profil und Passwort über die App, Keycloak-Kontoverwaltung abgeschaltet

- **Date:** 2026-10-09
- **Machine / Agent:** fin-de-nb-0061 / Claude Code
- **Issue / PR:** #46 / (PR folgt)
- **Branch:** feature/46-kontoseite

## Goal
Keycloak-Kontoseite nicht mehr anbieten; Passwort und Name ändert man über das Benutzermenü der App (Entscheidung
des Nutzers, ADR 0011). E-Mail-Adresse (= Anmeldung) darf der Benutzer nicht ändern.

## Done
- Realm `infra/keycloak/photoffice-realm.json`: Clients `account` und `account-console` ausdrücklich angelegt und
  deaktiviert (inkl. der `account`-Client-Rollen), `default-roles-photoffice` ohne `manage-account`/`view-profile`,
  Declarative User Profile unter `components` (`email`/`username` nur Admin, Hinweistext an der E-Mail).
- Theme: `login-update-password.ftl` kopiert (Überschrift „Passwort ändern“ bei `isAppInitiatedAction`), Texte
  „Profil bearbeiten“, „Pflichtfelder“, E-Mail-Hinweis; CSS für schreibgeschützte Felder, Hinweistexte und runde
  Sekundär-Buttons („Abbrechen“).
- Frontend: `AuthService.startAccountAction` (`initCodeFlow(returnUrl, {kc_action})`), nach der Rückkehr
  Ausgangsseite wiederherstellen (nur app-interne Pfade, nur nach Kontoaktionen) und `kc_action_status` auswerten;
  `layout/account-actions.ts` (Menüaktionen + Bestätigung per Snackbar); Menüeinträge in Studio- und Plattform-Shell.
- Tests: `KeycloakIntegrationTests` (+3: Kontoverwaltung 404 auch mit `admin-cli`-Token, Standardrollen, User
  Profile), `DevKeycloakContainer` Startup-Timeout 3 min; Vitest (AuthService, beide Shells); Playwright
  `e2e/account.spec.ts` mit frisch angelegten Benutzern.
- ADR 0011, Demo `docs/demos/0046-kontoseite.md`, AGENTS.md-Abschnitt "Keycloak theme" ergänzt.
- Geprüft: `./mvnw verify` grün (289 Tests), `npm test` (127) und `npm run build` grün (Initial 495,5 kB). AIA-Seiten
  per Screenshot (Desktop/Mobil). Eigener Stack (Keycloak 8191, Postgres 5446, Backend 8092, `ng serve` 4246 mit
  Wegwerf-Konfiguration): alle E2E-Specs außer `studio-onboarding.spec.ts` (fest auf 8180/8025/8080) grün, inkl.
  `account.spec.ts`. Manuell: Login, Token-Refresh, „Passwort vergessen“ (Seite weiter „Passwort festlegen“),
  manipuliertes E-Mail-Feld wird abgelehnt („Dieses Feld darf nicht editiert werden.“), Onboarding legt Benutzer an.

## Open / Next steps
- E-Mail-Adresse ändern nur per Admin (Keycloak-Admin-Konsole) – bei Bedarf eigenes Issue.
- Beobachtung (nicht geändert): `login(targetUrl)` übergibt das Ziel als OIDC-State, die App nutzt es aber nach dem
  normalen Login nicht – man landet immer auf `/studio`. Die Wiederherstellung aus #46 greift bewusst nur nach
  Kontoaktionen; für den normalen Login wäre es eine Zeile in `AuthService.init` (eigenes Issue).

## Pitfalls
- Steht `account-console` im Realm-JSON, aber nicht `account`, schlägt der Import fehl (Keycloak legt beide an, wenn
  `account` fehlt → doppelter Client). Mit `account` im JSON legt Keycloak die Kontoverwaltung gar nicht an – dann
  müssen auch die `account`-Rollen im JSON stehen. Eigene Composites für `default-roles-photoffice` auf
  `uma_authorization` scheitern beim Import (Rolle existiert dann noch nicht) → Rolle ohne Composites angeben,
  Keycloak ergänzt `offline_access`/`uma_authorization` selbst.
- Die Account-REST-API akzeptiert Tokens jedes Clients (auch `admin-cli`, Password Grant standardmäßig an) –
  deshalb Client `account` deaktivieren, nicht nur die Oberfläche.
- Abbrechen einer AIA liefert einen normalen Code plus `kc_action_status=cancelled` (kein OAuth-Fehler).
- Der Rücksprung-Pfad steht doppelt URL-kodiert im `state`; angular-oauth2-oidc dekodiert einmal, `safeTargetUrl`
  das zweite Mal.
- Eigener Frontend-Port braucht im eigenen Keycloak zusätzlich `post.logout.redirect.uris`, sonst hängt der
  Logout und der nächste Login läuft per SSO ohne Anmeldeseite.
