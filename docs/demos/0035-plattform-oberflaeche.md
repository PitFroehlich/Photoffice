# Demo #35: Plattform-Oberfläche – Studios registrieren, sperren und Onboarding-Status sehen

- **Issue / PR:** #35 / #37
- **Datum:** 2026-10-08
- **Agent / Maschine:** Claude Code / fin-de-nb-0061

## Was wurde umgesetzt
- Der Plattform-Betreiber hat einen eigenen Bereich **Plattform-Verwaltung** (http://localhost:4200/plattform) statt
  `curl`. Nach dem Login landet er automatisch dort; Studio-Benutzer landen wie bisher im Studio-Bereich und kommen
  nicht in die Plattform-Verwaltung.
- **Studio-Liste:** Name, Kürzel, Status (Aktiv/Gesperrt), Onboarding (Läuft/Abgeschlossen/Fehlgeschlagen),
  angelegt am. Solange ein Onboarding läuft, aktualisiert sich die Liste von selbst.
- **Studio registrieren:** Formular mit Name, Kürzel (wird aus dem Namen vorgeschlagen, gleiche Formatprüfung wie im
  Backend), E-Mail sowie Vor- und Nachname des ersten Studio-Admins. Ein vergebenes Kürzel steht direkt am Feld.
- **Sperren / Freischalten** mit Rückfrage und Rückmeldung.
- **Fehlgeschlagenes Onboarding ist sichtbar:** Das Backend merkt sich den letzten Fehler (z. B. „Die E-Mail-Adresse …
  gehört bereits zu Studio „Studio A“ …“), die Liste zeigt ihn an. Mit „Erneut versuchen“ startet der Betreiber den
  nächsten Versuch sofort, statt auf die automatische Wiederholung (alle 5 Minuten) zu warten.
- **Backend-Kleinigkeit:** Eine leere Studio-ID im Pfad (`/api/platform/tenants//suspend`) liefert jetzt 400 mit
  Problem Detail statt eines leeren 401; eine ungültige ID (`abc`) 400 mit deutschem Text.

## Vorbereitung
1. `docker compose up -d`
2. `cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev` (Liquibase ergänzt zwei Spalten in `tenant`)
3. `cd frontend && npm install && npm start`

Testbenutzer: siehe AGENTS.md "Dev users" (Passwort = Benutzername). Für die Demo brauchst du `operator` und
`admin-a`.

Hinweis: Die Keycloak-Anmeldeseiten werden in #36 eingedeutscht; die Schritte nennen deshalb nur die Felder.

## Schritt für Schritt

### Schritt 1: Plattform-Login
- **Was tun:** http://localhost:4200 öffnen und oben rechts auf **„Plattform-Login“** klicken (links neben
  „Studio-Login“).
- **Was du siehst:** Die Keycloak-Anmeldeseite mit dem Feld für den Benutzernamen.

### Schritt 2: Als Betreiber anmelden
- **Was tun:** Benutzername `operator` eingeben und bestätigen; auf der zweiten Seite das Passwort `operator`
  eingeben und anmelden.
- **Was du siehst:** Die Plattform-Verwaltung (URL `/plattform/studios`): oben „Photoffice | Plattform-Verwaltung“,
  rechts „Paula Plattform“, links die Navigation „Studios“. Die Überschrift „Studios“ mit der Anzahl darunter und
  die Tabelle mit „Studio A“ (`studio-a`) und „Studio B“ (`studio-b`), beide „Aktiv“ und Onboarding
  „Abgeschlossen“. Rechts in jeder Zeile ein rotes Schloss zum Sperren.

### Schritt 3: Benutzermenü
- **Was tun:** Oben rechts auf „Paula Plattform“ klicken, dann mit Esc schließen.
- **Was du siehst:** Das Menü mit „Paula Plattform“, „Plattform-Betreiber“ und „Abmelden“.

### Schritt 4: Formular „Studio registrieren“
- **Was tun:** Oben rechts auf **„Studio registrieren“** klicken. Bei „Name“ `Fotostudio Sonnenschein` eingeben.
  Danach im Feld „Kürzel“ den Text durch `Sonnenschein!` ersetzen.
- **Was du siehst:** Beim Tippen des Namens füllt sich das Kürzel mit `fotostudio-sonnenschein`. Nach der Änderung
  wird das Kürzel rot: „3–63 Zeichen: Kleinbuchstaben, Ziffern und Bindestriche, nicht am Anfang oder Ende“.

### Schritt 5: Vergebenes Kürzel
- **Was tun:** Kürzel `studio-a`, E-Mail `inhaber@sonnenschein.test` eingeben und **„Registrieren“** klicken.
- **Was du siehst:** Unter dem Kürzel: „Das Kürzel „studio-a“ ist bereits vergeben.“ Das Formular bleibt offen.

### Schritt 6: Studio registrieren
- **Was tun:** Kürzel `fotostudio-sonnenschein`, Vorname `Sonja`, Nachname `Schein` eingeben und
  **„Registrieren“** klicken. (Gibt es das Kürzel schon aus einem früheren Durchlauf, hänge `-2` an.)
- **Was du siehst:** Zurück in der Liste (seit #45 gefiltert auf das neue Kürzel), unten rechts die Meldung „„Fotostudio Sonnenschein“ wurde registriert.
  inhaber@sonnenschein.test erhält eine Einladung, sobald das Onboarding abgeschlossen ist.“ In der Zeile
  „Fotostudio Sonnenschein“ steht kurz „Läuft“, nach wenigen Sekunden – ohne Neuladen – „Abgeschlossen“.
  Optional: In Mailpit (http://localhost:8025) liegt die Einladung an `inhaber@sonnenschein.test`.

### Schritt 7: Studio sperren
- **Was tun:** In der Zeile „Fotostudio Sonnenschein“ auf das rote Schloss (Tooltip „Sperren“) klicken.
- **Was du siehst:** Rückfrage „Studio sperren?“ – „Die Benutzer von „Fotostudio Sonnenschein“ können sich danach
  nicht mehr anmelden …“. Mit **„Sperren“** bestätigen: Die Zeile zeigt „Gesperrt“, die Meldung „„Fotostudio
  Sonnenschein“ ist gesperrt.“ erscheint, statt des Schlosses gibt es jetzt ein offenes Schloss.

### Schritt 8: Studio freischalten
- **Was tun:** Auf das offene Schloss (Tooltip „Freischalten“) klicken und **„Freischalten“** bestätigen.
- **Was du siehst:** Die Zeile zeigt wieder „Aktiv“, Meldung „„Fotostudio Sonnenschein“ ist wieder freigeschaltet.“

### Schritt 9: Onboarding schlägt fehl
- **Was tun:** **„Studio registrieren“** – Name `Konflikt-Studio`, Kürzel `konflikt-studio`, E-Mail
  `admin@studio-a.test` (gehört schon dem Admin von Studio A) – **„Registrieren“**.
- **Was du siehst:** Nach wenigen Sekunden steht in der Zeile „Konflikt-Studio“ rot „Fehlgeschlagen“ und darunter:
  „Die E-Mail-Adresse admin@studio-a.test gehört bereits zu Studio „Studio A“ (studio-a). Ein Benutzer kann nur zu
  einem Studio gehören.“ sowie „Letzter Versuch <Datum>, <Uhrzeit> Uhr – wird automatisch wiederholt.“ Die Überschrift zählt
  „davon 1 mit fehlgeschlagenem Onboarding“ (bzw. mehr, falls schon E2E-Tests liefen).

### Schritt 10: Erneut versuchen
- **Was tun:** In der Zeile „Konflikt-Studio“ auf den runden Pfeil (Tooltip „Onboarding erneut versuchen“) klicken.
- **Was du siehst:** Die Meldung „Das Onboarding von „Konflikt-Studio“ wird erneut versucht.“ Weil der Konflikt
  besteht, bleibt es bei „Fehlgeschlagen“; die Zeit bei „Letzter Versuch“ springt auf jetzt.
  (Würde man den Benutzer in Keycloak aus Studio A entfernen, wäre das Onboarding danach „Abgeschlossen“.)

### Schritt 11: Studio-Benutzer haben keinen Zugang
- **Was tun:** Oben rechts über das Benutzermenü **„Abmelden“**. Dann „Studio-Login“ mit `admin-a`/`admin-a` und
  anschließend in der Adresszeile http://localhost:4200/plattform aufrufen.
- **Was du siehst:** Du landest wieder im Studio-Bereich von „Studio A“ (Übersicht), nicht in der
  Plattform-Verwaltung.

### Schritt 12: Leere und ungültige Studio-ID (API)
- **Was tun:** Im Terminal:
  ```bash
  export OP=$(curl -s localhost:8180/realms/photoffice/protocol/openid-connect/token \
    -d grant_type=password -d client_id=photoffice-dev-cli -d username=operator -d password=operator \
    | python3 -c "import sys,json;print(json.load(sys.stdin)['access_token'])")
  curl -s -i -X POST localhost:8080/api/platform/tenants//suspend -H "Authorization: Bearer $OP"
  curl -s -X POST localhost:8080/api/platform/tenants/abc/suspend -H "Authorization: Bearer $OP"
  ```
- **Was du siehst:** Erst `HTTP/1.1 400`, `Content-Type: application/problem+json` und
  `"detail":"Die Anfrage-URL ist ungültig (z. B. ein leerer Pfadabschnitt wie „//“)."` (früher: 401 ohne Inhalt).
  Dann `"status":400` mit `"detail":"Ungültige Studio-ID „abc“ – erwartet wird eine UUID."`

## Automatisch abgesichert
- Playwright `frontend/e2e/platform.spec.ts`: Plattform-Login → Liste, Studio-Login des Betreibers → Plattform,
  Studio-Benutzer kommen nicht in `/plattform`, Kürzelprüfung, vergebenes Kürzel, Registrieren bis „Abgeschlossen“,
  Sperren/Freischalten, fehlgeschlagenes Onboarding mit Grund und „Erneut versuchen“ (Schritte 1–11).
  `studio-login.spec.ts` prüft jetzt, dass der Betreiber in der Plattform-Verwaltung landet.
- Backend: `PlatformTenantControllerTests` (leere/ungültige ID als Problem Detail, Fehler sichtbar und nach Erfolg
  gelöscht, Retry-Endpunkt, deutsche Meldung bei vergebenem Kürzel), `TenantOnboardingRetryTests` (nur das Event des
  Studios wird erneut zugestellt), `StudioOnboardingFailureReasonTests`, `StudioOnboardingIntegrationTests` (Fehler
  wird trotz Rollback gespeichert und nach erfolgreicher Wiederholung gelöscht).
- Frontend-Unit-Tests: `studio-list.spec.ts`, `studio-form.spec.ts`, `platform-shell.spec.ts`, Guards,
  `auth.service.spec.ts`, `public-shell.spec.ts`.

## Noch offen / Einschränkungen
- Studios lassen sich nicht löschen; Test-Studios aus E2E-Läufen sammeln sich in der lokalen Datenbank
  (Zurücksetzen: `docker compose down -v`). Fehlgeschlagene Test-Studios werden alle 5 Minuten erneut versucht und
  schreiben dabei eine Warnung ins Log.
- Die Liste hat noch keine Suche/Seiten (die API liefert alle Studios).
- Studio-Name und Admin-Daten lassen sich nach dem Registrieren nicht ändern.
- Keycloak-Seiten und Mails auf Deutsch: #36.

## Aufräumen
Backend und Frontend beenden (Strg+C), `docker compose down`.
