# Demo #24: Studio-Onboarding – Keycloak-Organisation und erster Studio-Admin automatisch

- **Issue / PR:** #24 / (siehe PR zu #24)
- **Datum:** 2026-10-08
- **Agent / Maschine:** Claude Code / fin-de-nb-0061

## Was wurde umgesetzt
- Der Plattform-Betreiber registriert ein Studio zusammen mit der E-Mail des ersten Studio-Admins. Danach entsteht
  automatisch alles Weitere: die Keycloak-Organisation (Alias = Kürzel), der Benutzer mit der Rolle Studio-Admin
  als Mitglied und eine Einladungs-Mail mit Link zum Setzen des Passworts. Es wird kein Passwort per Mail verschickt.
- Die Studio-Liste zeigt den Onboarding-Status (`PENDING` → `COMPLETED`). Ist Keycloak gerade nicht erreichbar, wird
  das Onboarding automatisch wiederholt.
- Der Betreiber kann ein Studio sperren und wieder freischalten. Gesperrte Studios haben keinen Zugriff, und ihre
  Organisation wird in Keycloak deaktiviert.
- Lokal fängt **Mailpit** alle E-Mails ab: http://localhost:8025

Die Plattform-Funktionen haben noch keine Oberfläche, deshalb laufen die Schritte des Betreibers per `curl`.
Die Schritte des neuen Studio-Admins laufen im Browser.

## Vorbereitung
1. `docker compose up -d` (startet jetzt auch Mailpit). Wer Keycloak schon vorher laufen hatte, lädt den
   geänderten Dev-Realm einmal neu: `docker compose up -d --force-recreate keycloak`
2. `cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`
3. `cd frontend && npm install && npm start`

Testbenutzer: siehe AGENTS.md "Dev users" (Passwort = Benutzername). Für die Demo brauchst du `operator`.

## Schritt für Schritt

### Schritt 1: Als Plattform-Betreiber ein Token holen
- **Was tun:** Im Terminal:
  ```bash
  export OP=$(curl -s localhost:8180/realms/photoffice/protocol/openid-connect/token \
    -d grant_type=password -d client_id=photoffice-dev-cli -d username=operator -d password=operator \
    | python3 -c "import sys,json;print(json.load(sys.stdin)['access_token'])")
  curl -s localhost:8080/api/platform/tenants -H "Authorization: Bearer $OP" | python3 -m json.tool
  ```
- **Was du siehst:** Die Dev-Studios „Studio A“ und „Studio B“, jeweils mit `"status": "ACTIVE"` und
  `"onboardingStatus": "COMPLETED"`.

### Schritt 2: Neues Studio registrieren
- **Was tun:**
  ```bash
  curl -s localhost:8080/api/platform/tenants -H "Authorization: Bearer $OP" -H 'Content-Type: application/json' \
    -d '{"slug":"studio-mueller","name":"Fotostudio Müller","adminEmail":"inhaber@studio-mueller.test",
         "adminFirstName":"Maria","adminLastName":"Müller"}' | python3 -m json.tool
  ```
- **Was du siehst:** Das neue Studio mit `"status": "ACTIVE"` und `"onboardingStatus": "PENDING"`, weil das
  Onboarding im Hintergrund läuft. Kopiere die `id`, du brauchst sie in Schritt 8:
  `export ID=<id aus der Antwort>`

### Schritt 3: Onboarding abgeschlossen
- **Was tun:** Den Listenaufruf aus Schritt 1 (zweite Zeile) wiederholen.
- **Was du siehst:** „Fotostudio Müller“ hat jetzt `"onboardingStatus": "COMPLETED"`.
  Optional in der Keycloak-Admin-Konsole (http://localhost:8180, `admin`/`admin`, Realm „photoffice“ →
  *Organizations*): Die Organisation `studio-mueller` mit der Beschreibung „Fotostudio Müller“ ist da, unter
  *Members* steht `inhaber@studio-mueller.test`.

### Schritt 4: Einladungs-Mail ansehen
- **Was tun:** http://localhost:8025 (Mailpit) öffnen und die neueste Mail an `inhaber@studio-mueller.test` anklicken.
- **Was du siehst:** Betreff „Update Your Account“: „…update your Photoffice account by performing the following
  action(s): Update Password, Verify Email“, darunter ein Link. In der Mail steht **kein** Passwort.

### Schritt 5: Link öffnen
- **Was tun:** In der Mail auf den Link klicken.
- **Was du siehst:** Keycloak-Seite „Perform the following action(s): Update Password, Verify Email“ mit dem Link
  „» Click here to proceed“. Darauf klicken.

### Schritt 6: Passwort setzen
- **Was tun:** Im Formular „Update password“ zweimal ein Passwort eingeben, z. B. `Geheim-123`, dann „Submit“.
- **Was du siehst:** „Account updated – Your account has been updated.“ und der Link „« Back to Application“.
  Mit dem Klick darauf landest du auf der Photoffice-Startseite (http://localhost:4200).

### Schritt 7: Als neuer Studio-Admin anmelden
- **Was tun:** Oben rechts auf „Studio-Login“ klicken. Als Benutzername `inhaber@studio-mueller.test` eingeben,
  „Sign In“ klicken; auf der nächsten Seite das Passwort aus Schritt 6 eingeben, „Sign In“.
- **Was du siehst:** Den Studio-Bereich: links oben „Fotostudio Müller“, Überschrift „Übersicht“,
  „Willkommen, Maria Müller“ und der Chip „Studio-Administrator“.

### Schritt 8: Studio sperren
- **Was tun:** Im Terminal:
  ```bash
  curl -s -X POST localhost:8080/api/platform/tenants/$ID/suspend -H "Authorization: Bearer $OP" | python3 -m json.tool
  ```
  Dann im Browser die Seite neu laden (F5).
- **Was du siehst:** Die Antwort enthält `"status": "SUSPENDED"`. Im Browser steht „Kein Zugriff – Ihr Benutzerkonto
  ist keinem aktiven Studio zugeordnet.“ Optional in der Keycloak-Admin-Konsole: Die Organisation `studio-mueller`
  ist jetzt deaktiviert.

### Schritt 9: Studio wieder freischalten
- **Was tun:**
  ```bash
  curl -s -X POST localhost:8080/api/platform/tenants/$ID/reactivate -H "Authorization: Bearer $OP" | python3 -m json.tool
  ```
  Dann die Seite im Browser neu laden.
- **Was du siehst:** `"status": "ACTIVE"`, im Browser wieder „Fotostudio Müller“ mit „Willkommen, Maria Müller“.

### Schritt 10 (optional): Fehlerfälle
- **Was tun:** Schritt 2 noch einmal ausführen (gleiches Kürzel). Danach dasselbe ohne `adminEmail` mit einem anderen
  Kürzel.
- **Was du siehst:** Erst `"status": 409` („A tenant with slug 'studio-mueller' already exists“), dann
  `"status": 400`, weil die E-Mail des ersten Admins Pflicht ist.

## Automatisch abgesichert
- Schritte 2–9: Playwright-Test `frontend/e2e/studio-onboarding.spec.ts` (Mail aus Mailpit, Passwort setzen, Login,
  Sperren, Freischalten).
- Backend: `StudioOnboardingIntegrationTests` gegen echte Keycloak- und Mailpit-Container (Organisation, Rolle,
  Mitgliedschaft, Mail mit Link, Login mit echtem Token, Fortsetzen nach halb fertigem Onboarding, automatische
  Wiederholung nach einem Fehler, Sperren deaktiviert die Organisation). `PlatformTenantControllerTests` für die API
  (Pflichtfelder, Sperren/Freischalten, 404, nur Betreiber).

## Noch offen / Einschränkungen
- Keine Oberfläche für den Plattform-Betreiber; Registrieren und Sperren nur über die API.
- Die Keycloak-Seiten und die Mail sind noch englisch und nicht im Photoffice-Design (eigenes Theme/Locale fehlt).
- Gehört die Admin-E-Mail schon zu einem anderen Studio, bleibt das Onboarding auf `PENDING` und wird wiederholt
  (Fehlermeldung nur im Log).
- Produktion braucht SMTP im Realm, ein echtes Client-Secret und die richtige Frontend-URL (#28).

## Aufräumen
Backend und Frontend beenden (Strg+C), `docker compose down`.
