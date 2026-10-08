# Keycloak auf Deutsch: Login-Seiten und E-Mails im Photoffice-Design

- **Date:** 2026-10-08
- **Machine / Agent:** fin-de-nb-0061 / Claude Code
- **Issue / PR:** #36 / #38
- **Branch:** feature/36-keycloak-deutsch

## Goal
Keycloak-Seiten und -Mails (Einladung neuer Studio-Admins, Passwort vergessen) deutsch und im Photoffice-Design.

## Done
- Theme `infra/keycloak/themes/photoffice/` (ADR 0009):
  - `login/`: erbt von `keycloak.v2`; `resources/css/photoffice.css` (PatternFly-Variablen, Petrol/Kupfer/warmes
    Neutral, Inter aus `resources/fonts/` – dieselbe Datei wie `@fontsource-variable/inter` im Frontend, Lizenz
    `OFL-Inter.txt`, Kamera-Logo `photo_camera.svg` aus Material Symbols), `messages/messages_de.properties`,
    kopiertes `info.ftl` (Überschrift „Willkommen bei Photoffice!“, Aktion als Button). Dark Mode aus.
  - `email/`: erbt von `base`; `html/template.ftl` (Layout mit Inline-Styles, Makros für Anrede und Button),
    `executeActions` und `password-reset` als HTML + Text, `messages/messages_de.properties`.
- Realm: `internationalizationEnabled`, `supportedLocales` = `de`, `defaultLocale` = `de`, `loginTheme`/`emailTheme`
  = `photoffice`.
- `docker-compose.yml`: Theme als Volume. Tests: neuer `DevKeycloakContainer` (Realm + Theme) für
  `KeycloakIntegrationTests` und `StudioOnboardingIntegrationTests`; neuer Test
  `loginPageIsGermanAndUsesThePhotofficeTheme`; Onboarding-Test prüft deutschen Betreff, Absender, Anrede, Button.
- E2E: `studio-onboarding.spec.ts` auf deutsche Texte umgestellt; `studio-login.spec.ts` prüft die deutsche
  Anmeldeseite.
- AGENTS.md-Abschnitt "Keycloak theme", Demo `docs/demos/0036-keycloak-deutsch.md`.
- Geprüft: `./mvnw verify` grün; Seiten und Mails mit eigenem Keycloak + Mailpit (Ports 8190/8035) per Playwright
  durchgeklickt und per Screenshot begutachtet; die geänderten E2E-Schritte mit einem Wegwerf-Skript gegen diese
  Container nachgespielt. Die komplette E2E-Suite lief hier nicht (Standard-Stack war von #35 belegt).

## Open / Next steps
- Studioname in der Einladung: bräuchte ein Benutzerattribut im Declarative User Profile oder eine eigene Mail.
- Link aus „Passwort vergessen“ ist 5 Minuten gültig (Keycloak-Standard `actionTokenGeneratedByUserLifespan`) –
  ggf. verlängern.
- Account-Konsole von Keycloak ist deutsch, aber im Keycloak-Design (wird von Studios bisher nicht genutzt).
- Deployment (#28): Theme mit ausliefern; Demo #24 beschreibt noch die englischen Keycloak-Texte.

## Pitfalls
- Mit `de` **und** `en` als Sprachen zeigt Keycloak bei englischem Browser (auch Playwright-Standard `en-US`)
  Englisch → nur `de`.
- `photoffice-frontend` erzwingt PKCE: Auth-URLs für Tests brauchen `code_challenge` + `code_challenge_method=S256`,
  sonst Redirect mit Fehler statt Login-Seite.
- `kcSanitize` filtert HTML aus Message-Texten; deshalb steht das Mail-HTML in den Templates und die Messages sind
  reiner Text.
- Neue Keycloak-Realm-Einstellungen greifen lokal erst nach `docker compose up -d --force-recreate keycloak`.
