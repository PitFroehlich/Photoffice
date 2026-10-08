# 0011: Keycloak-Kontoverwaltung abgeschaltet – Profil und Passwort über das Benutzermenü der App

- **Status:** Accepted
- **Date:** 2026-10-09

## Context
Keycloak bringt eine eigene Kontoverwaltung mit (`/realms/photoffice/account`). Seit #36 ist sie deutsch, sieht aber
nach Standard-Keycloak aus (#46). Sie ist seit Keycloak 26 eine React-Anwendung; ein Photoffice-Design ginge nur
begrenzt per CSS/Logo oder mit einem eigenen Build (Keycloakify). Außerdem bietet sie mehr als Photoffice braucht oder
erlaubt:
- Die **E-Mail-Adresse** ist in Photoffice die Anmeldung und mit dem Studio verbunden (Organisation, Einladung #24).
  Im Standard-User-Profile darf der Benutzer sie selbst ändern – sowohl in der Konsole als auch über die
  Account-REST-API. Die REST-API akzeptiert dabei Tokens beliebiger Clients des Realms, z. B. von `admin-cli`
  (Password Grant ist dort standardmäßig an) – Abschalten der Oberfläche allein reicht also nicht.
- Sitzungen, Anwendungen, Organisationen, verknüpfte Konten: für Studios ohne Nutzen und erklärungsbedürftig.

Gebraucht werden nur **Passwort ändern** und **Name ändern**. Keycloak bietet dafür Application-Initiated Actions
(AIA): Die App startet den normalen Login-Code-Flow mit `kc_action=UPDATE_PASSWORD` bzw. `UPDATE_PROFILE`; Keycloak
zeigt nur die eine Seite (Login-Theme, also schon im Photoffice-Design) und leitet danach – auch bei „Abbrechen“ – mit
`kc_action_status=success|cancelled|error` und neuem Code zur App zurück.

## Decision
- **Kontoverwaltung aus:** Im Realm sind die Clients `account` und `account-console` deaktiviert
  (`/realms/photoffice/account` → 404 „account management not enabled“, auch für die REST-API). Die Standardrolle
  `default-roles-photoffice` enthält nicht mehr `manage-account`/`view-profile` (zusätzliche Absicherung, falls der
  Client wieder eingeschaltet wird). Beide Clients stehen dafür ausdrücklich in `photoffice-realm.json` – sonst legt
  Keycloak sie beim Import mit Standardwerten an.
- **Benutzermenü** in Studio- und Plattform-Bereich: „Profil bearbeiten“ (`UPDATE_PROFILE`) und „Passwort ändern“
  (`UPDATE_PASSWORD`) über `AuthService.startAccountAction` (`initCodeFlow` mit `kc_action`). Die aktuelle Seite geht
  als OIDC-State mit und wird nach der Rückkehr wiederhergestellt (nur app-interne Pfade); der neue Token enthält den
  geänderten Namen. Bei Erfolg zeigt die App eine kurze Bestätigung, bei Abbruch nichts.
- **Declarative User Profile im Realm:** `email` und `username` darf nur `admin` ändern (Benutzer sehen die E-Mail
  schreibgeschützt mit Hinweis), Vor- und Nachname Benutzer und Admin. Der Service-Account des Backends
  (`photoffice-backend`, Rolle `manage-users`) zählt als Admin – Studio-Onboarding (#24) legt Benutzer weiter mit
  E-Mail und Namen an.
- **Keycloak-Seiten:** Login-Theme `photoffice`. Kopiert wird zusätzlich `login-update-password.ftl`, damit die
  Seite beim Aufruf aus der App „Passwort ändern“ heißt (Einladung und „Passwort vergessen“: weiter „Passwort
  festlegen“); „Profil bearbeiten“, „Pflichtfelder“ und der E-Mail-Hinweis kommen aus `messages_de.properties`.

## Consequences
- Studios sehen nirgends Standard-Keycloak-Oberflächen; was Benutzer ändern dürfen, steht im Realm als Code.
- Änderung der E-Mail-Adresse geht nur über einen Admin (Keycloak-Admin-Konsole oder später eine Funktion im
  Plattform-/Studio-Bereich). Wird das gebraucht, ist es ein eigenes Issue (Login, Organisation und
  Einladungsweg hängen daran).
- Weitere Selbstbedienung (z. B. 2FA einrichten `CONFIGURE_TOTP`, eigene Sitzungen abmelden) wird bei Bedarf
  ebenfalls als AIA bzw. in der App gebaut, nicht über die Kontoverwaltung.
- `UPDATE_PASSWORD` verlangt bei einer länger zurückliegenden Anmeldung (Keycloak-Standard: 5 Minuten) zuerst das
  aktuelle Passwort – gewollt.
- Keycloak-Updates: die kopierte `login-update-password.ftl` mit `keycloak.v2/login/login-update-password.ftl`
  vergleichen (wie `info.ftl`, ADR 0009). Links von Keycloak auf die Kontoverwaltung (z. B. nach
  `send-verify-email` ohne `client_id`) führen ins Leere – Admin-API-Aufrufe immer mit `client_id`/`redirect_uri`
  der App (wie `KeycloakAdminClient.sendActionsEmail`).
- Lokal: `docker compose up -d --force-recreate keycloak`, damit der geänderte Realm importiert wird.
