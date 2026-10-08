# Demo #6: Studio-Login und Rollen mit Keycloak

- **Issue / PR:** #6 / #26
- **Datum:** 2026-10-08 (nachträglich erstellt mit #29)
- **Agent / Maschine:** Claude Code / fin-de-nb-0072

## Was wurde umgesetzt
- Studio-Mitarbeiter melden sich über Keycloak an. Das Studio ergibt sich aus der Keycloak-Organisation
  (Alias = Kürzel des Studios); alle Daten einer Anfrage sind auf dieses Studio beschränkt.
- Rollen: Plattform-Betreiber, Studio-Admin, Fotograf. Plattform-Funktionen nur für den Betreiber,
  Studio-Funktionen nur für Mitglieder genau eines aktiven Studios, alles andere wird verweigert.
- Studio-Bereich zeigt Studio, Benutzer und Rolle; Abmelden.

## Vorbereitung
1. `docker compose up -d`
2. `cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`
3. `cd frontend && npm start`

## Schritt für Schritt

### Schritt 1: Startseite öffnen
- **Was tun:** http://localhost:4200 im Browser öffnen.
- **Was du siehst:** Startseite "Ihre Bilder. Ihre Kunden. Ein Ort.", unten "Backend: photoffice-backend 0.0.1-SNAPSHOT", oben rechts der Button "Studio-Login". (Design seit #27.)

### Schritt 2: Geschützten Bereich ohne Login aufrufen
- **Was tun:** In der Adresszeile http://localhost:4200/studio aufrufen.
- **Was du siehst:** Du wirst zur Keycloak-Anmeldeseite "Sign in to your account" umgeleitet – der Studio-Bereich ist nur mit Login erreichbar.

### Schritt 3: Als Studio-Admin von Studio A anmelden
- **Was tun:** Benutzername `admin-a` eingeben, "Sign In" klicken; auf der nächsten Seite Passwort `admin-a` eingeben, "Sign In".
- **Was du siehst:** Studio-Bereich: oben "Photoffice | Studio A", Seite "Übersicht" mit "Willkommen, Anna Admin" und der Rolle "Studio-Administrator", oben rechts "Anna Admin".

### Schritt 4: Abmelden
- **Was tun:** Oben rechts auf "Anna Admin" klicken (Benutzermenü), dann "Abmelden".
- **Was du siehst:** Startseite, oben rechts wieder "Studio-Login".

### Schritt 5: Als Fotograf anmelden
- **Was tun:** "Studio-Login" → `foto-a` / `foto-a`.
- **Was du siehst:** "Studio A", "Willkommen, Felix Foto", Rolle "Fotograf". Danach abmelden.

### Schritt 6: Anderes Studio
- **Was tun:** "Studio-Login" → `admin-b` / `admin-b`.
- **Was du siehst:** Oben "Studio B" – der Benutzer sieht nur sein eigenes Studio. Danach abmelden.

### Schritt 7: Plattform-Betreiber ist kein Studio-Benutzer
- **Was tun:** "Studio-Login" → `operator` / `operator`.
- **Was du siehst:** "Kein Zugriff – Ihr Benutzerkonto ist keinem aktiven Studio zugeordnet." Danach oben rechts "Abmelden".

### Schritt 8 (optional): Rechte über die API prüfen
- **Was tun:** Im Terminal:
  ```bash
  TOKEN=$(curl -s localhost:8180/realms/photoffice/protocol/openid-connect/token \
    -d grant_type=password -d client_id=photoffice-dev-cli -d username=admin-a -d password=admin-a \
    | python3 -c "import sys,json;print(json.load(sys.stdin)['access_token'])")
  curl -s -o /dev/null -w "%{http_code}\n" localhost:8080/api/platform/tenants -H "Authorization: Bearer $TOKEN"
  ```
- **Was du siehst:** `403` – ein Studio-Admin darf keine Studios verwalten. Mit `username=operator`/`password=operator` liefert derselbe Aufruf `200`.

## Automatisch abgesichert
- Schritte 1–7: Playwright-Tests `frontend/e2e/studio-login.spec.ts`.
- Backend: 11 Zugriffsregel-Tests, 6 Tests gegen echten Keycloak-Container.

## Noch offen / Einschränkungen
- Studios und Keycloak-Organisationen werden noch manuell bzw. über den Dev-Realm angelegt (#24).
- OIDC-Einstellungen im Frontend fest auf localhost (#28). E2E noch nicht in CI (#25).

## Aufräumen
Backend und Frontend beenden (Strg+C), `docker compose down`.
