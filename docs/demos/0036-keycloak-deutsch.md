# Demo #36: Keycloak auf Deutsch – Login-Seiten und E-Mails im Photoffice-Design

- **Issue / PR:** #36 / –
- **Datum:** 2026-10-08
- **Agent / Maschine:** Claude Code / fin-de-nb-0061

## Was wurde umgesetzt
- Die Keycloak-Seiten (Anmelden, Passwort vergessen, Passwort festlegen, Einladungslink) sind **deutsch** und sehen
  aus wie Photoffice: warmer heller Hintergrund, Kamera-Logo mit „Photoffice“ in Petrol, weiße Karte mit
  Kupfer-Akzent oben, runde Petrol-Buttons, Schrift Inter (wie im Frontend).
- Die **Einladungs-Mail** für neue Studio-Admins ist freundlich und deutsch („Willkommen bei Photoffice – bitte
  richten Sie Ihren Zugang ein“), mit Button „Passwort festlegen“. Auch die Mail „Passwort vergessen“ ist neu
  formuliert. Beide Mails gibt es als HTML und als Text, Absender „Photoffice“.
- Keycloak spricht nur noch Deutsch, auch bei englischem Browser (kein Sprachumschalter).
- Theme als Code: `infra/keycloak/themes/photoffice/` (Entscheidung: ADR 0009).

## Vorbereitung
1. `docker compose up -d --force-recreate keycloak` (lädt Realm und Theme neu), danach `docker compose up -d`
2. `cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`
3. `cd frontend && npm install && npm start`

Testbenutzer: siehe AGENTS.md "Dev users" (Passwort = Benutzername). Mailpit: http://localhost:8025

## Schritt für Schritt

### Schritt 1: Anmeldeseite
- **Was tun:** http://localhost:4200 öffnen, oben rechts auf „Studio-Login“ klicken. Am besten in einem Browser,
  der auf Englisch eingestellt ist – oder einfach im normalen Browser.
- **Was du siehst:** Oben das Kamera-Symbol mit „Photoffice“ in Petrol. Darunter eine weiße Karte mit
  Kupfer-Streifen oben, Überschrift „Bei Photoffice anmelden“, Feld „E-Mail-Adresse oder Benutzername“ und ein
  runder Petrol-Button „Anmelden“. Kein englischer Text, kein Sprachumschalter.

### Schritt 2: Zweite Anmeldeseite (Passwort)
- **Was tun:** `admin-a` eingeben, „Anmelden“ klicken.
- **Was du siehst:** Dieselbe Karte, oben grau der Benutzername `admin-a` mit einem Symbol „Mit anderer
  E-Mail-Adresse anmelden“, darunter das Feld „Passwort“ und der Link „Passwort vergessen?“.

### Schritt 3: Passwort vergessen
- **Was tun:** Auf „Passwort vergessen?“ klicken, `admin@studio-a.test` eingeben, „Absenden“.
- **Was du siehst:** Vorher: Überschrift „Passwort vergessen?“, Buttons „Absenden“ und „Zurück zur Anmeldung“,
  darunter der Hinweis „Geben Sie Ihre E-Mail-Adresse ein. Wir schicken Ihnen einen Link …“. Nachher: wieder die
  Anmeldeseite mit der Meldung „Wir haben Ihnen eine E-Mail geschickt. Bitte öffnen Sie den Link darin, um ein neues
  Passwort festzulegen.“

### Schritt 4: Mail „Neues Passwort festlegen“
- **Was tun:** Mailpit (http://localhost:8025) öffnen, die neueste Mail an `admin@studio-a.test` öffnen. Oben
  zwischen „HTML“ und „Text“ wechseln.
- **Was du siehst:** Absender „Photoffice“, Betreff „Photoffice: Neues Passwort festlegen“. HTML: weiße Karte auf
  warmem Hintergrund, „Photoffice“ in Petrol, „Hallo Anna Admin,“, Petrol-Button „Neues Passwort festlegen“, darunter
  der Link zum Kopieren, „Der Link ist 5 Minuten gültig.“ und der Hinweis, dass man die Mail ignorieren kann. Die
  Text-Variante enthält dieselben Sätze mit dem Link.
- Optional: Button klicken → Seite „Passwort festlegen“ („Bitte legen Sie ein neues Passwort fest.“). Wenn du das
  Passwort änderst, setze es wieder auf `admin-a`, sonst passen die Dev-Zugangsdaten nicht mehr.

### Schritt 5: Neues Studio registrieren (löst die Einladung aus)
- **Was tun:** Im Terminal als Plattform-Betreiber ein Studio anlegen (oder über die Betreiber-Oberfläche, sobald
  #35 fertig ist):
  ```bash
  export OP=$(curl -s localhost:8180/realms/photoffice/protocol/openid-connect/token \
    -d grant_type=password -d client_id=photoffice-dev-cli -d username=operator -d password=operator \
    | python3 -c "import sys,json;print(json.load(sys.stdin)['access_token'])")
  curl -s localhost:8080/api/platform/tenants -H "Authorization: Bearer $OP" -H 'Content-Type: application/json' \
    -d '{"slug":"studio-weber","name":"Fotostudio Weber","adminEmail":"inhaber@studio-weber.test",
         "adminFirstName":"Maria","adminLastName":"Weber"}' | python3 -m json.tool
  ```
- **Was du siehst:** Das neue Studio mit `"onboardingStatus": "PENDING"`; wenige Sekunden später ist das Onboarding
  fertig und die Einladung verschickt.

### Schritt 6: Einladungs-Mail
- **Was tun:** In Mailpit die Mail an `inhaber@studio-weber.test` öffnen.
- **Was du siehst:** Absender „Photoffice“, Betreff „Willkommen bei Photoffice – bitte richten Sie Ihren Zugang
  ein“. Inhalt: Überschrift „Willkommen bei Photoffice!“, „Hallo Maria Weber,“, „für Sie wurde ein Zugang zu
  Photoffice eingerichtet, der Plattform für Fotostudios.“, „Damit Sie loslegen können, fehlt nur noch: Passwort
  festlegen, E-Mail-Adresse bestätigen. Das dauert keine Minute.“, der Button „Passwort festlegen“ und „Der Link ist
  3 Tage gültig.“ Kein Passwort in der Mail.

### Schritt 7: Einladungslink öffnen
- **Was tun:** In der Mail auf „Passwort festlegen“ klicken.
- **Was du siehst:** Keycloak-Seite im Photoffice-Design mit der Überschrift „Willkommen bei Photoffice!“, dem Text
  „Bitte schließen Sie die Einrichtung Ihres Zugangs ab: **Passwort festlegen, E-Mail-Adresse bestätigen**“ und dem
  Button „Weiter“.

### Schritt 8: Passwort festlegen
- **Was tun:** „Weiter“ klicken, zweimal `Geheim-123` eingeben („Neues Passwort“, „Passwort wiederholen“),
  „Absenden“.
- **Was du siehst:** Vorher: Überschrift „Passwort festlegen“ mit dem Hinweis „Bitte legen Sie ein Passwort für
  Ihren Photoffice-Zugang fest.“ Nachher: Überschrift „Geschafft!“, „Ihr Zugang ist bereit. Sie können sich jetzt
  bei Photoffice anmelden.“ und der Button „Weiter zu Photoffice“.

### Schritt 9: Zurück zu Photoffice und anmelden
- **Was tun:** „Weiter zu Photoffice“ klicken, dann „Studio-Login“, `inhaber@studio-weber.test` und `Geheim-123`
  eingeben (zwei Seiten, jeweils „Anmelden“).
- **Was du siehst:** Die Photoffice-Startseite, nach dem Login den Studio-Bereich von „Fotostudio Weber“ mit
  „Willkommen, Maria Weber“.

### Schritt 10: Benutzten Link noch einmal öffnen
- **Was tun:** Abmelden, dann den Link aus der Einladungs-Mail noch einmal öffnen.
- **Was du siehst:** „Es ist ein Fehler aufgetreten.“ mit dem Text „Dieser Link wurde bereits benutzt oder ist
  abgelaufen. Bitte melden Sie sich bei Photoffice an, bei Bedarf über „Passwort vergessen?“.“

## Automatisch abgesichert
- `KeycloakIntegrationTests.loginPageIsGermanAndUsesThePhotofficeTheme`: Anmeldeseite deutsch (auch bei
  `Accept-Language: en`), Überschrift „Bei Photoffice anmelden“, `photoffice.css` eingebunden.
- `StudioOnboardingIntegrationTests`: Einladungs-Mail mit deutschem Betreff, Absender „Photoffice“, Anrede, Button
  „Passwort festlegen“ und Link.
- Playwright `e2e/studio-onboarding.spec.ts`: Schritte 6–9 (deutsche Mail, „Willkommen bei Photoffice!“, „Weiter“,
  „Passwort festlegen“, „Ihr Zugang ist bereit.“, „Weiter zu Photoffice“); `e2e/studio-login.spec.ts`: Schritt 1.

## Noch offen / Einschränkungen
- Der Studioname steht nicht in der Einladung (Keycloak kennt ihn beim Versand nicht, siehe ADR 0009).
- Der Link aus „Passwort vergessen“ ist nur 5 Minuten gültig (Keycloak-Standard), die Einladung 3 Tage.
- Die Keycloak-Kontoverwaltung (`/realms/photoffice/account`) ist deutsch, aber nicht im Photoffice-Design.
- Deployment (#28): Theme-Verzeichnis mit ausliefern.

## Aufräumen
`docker compose down`, Backend und Frontend beenden.
