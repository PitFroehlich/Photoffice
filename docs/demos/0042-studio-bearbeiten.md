# Demo #42: Studio bearbeiten und Onboarding mit korrigierten Admin-Daten wiederholen

- **Issue / PR:** #42 / (PR folgt)
- **Datum:** 2026-10-09
- **Agent / Maschine:** Claude Code / fin-de-nb-0061

## Was wurde umgesetzt
- **Studio bearbeiten:** In der Studio-Liste hat jede Zeile einen Stift „Bearbeiten“. Das Formular ist dasselbe wie
  beim Registrieren (gleiche Prüfregeln). Der **Name** lässt sich immer ändern, das **Kürzel** nie. Die Daten des
  **ersten Studio-Admins** (E-Mail, Vor-/Nachname) lassen sich ändern, solange das Onboarding nicht abgeschlossen ist.
- **Fehlgeschlagenes Onboarding reparieren:** Unter der Fehlermeldung in der Liste steht „Daten korrigieren“. Das
  Formular zeigt oben den Fehlergrund. Nach dem Speichern wird das Onboarding sofort **mit den neuen Daten** erneut
  versucht – auch „Erneut versuchen“ und die automatische Wiederholung verwenden jetzt die korrigierten Daten.
- **Name auch in Keycloak:** Ein neuer Name landet in der Beschreibung der Keycloak-Organisation (der
  Organisationsname bleibt das Kürzel).
- **Datensparsamkeit:** Die Admin-Daten stehen nur bis zum Abschluss des Onboardings am Studio und werden danach
  gelöscht. Neue Registrierungs-Events enthalten keine personenbezogenen Daten mehr (ADR 0012).
- **Alte Studios:** Studios, deren Onboarding schon vor dieser Änderung hing (z. B. „Konflikt-Studio“ aus der Demo
  #35), bekommen ihre Admin-Daten beim Start des Backends aus dem gespeicherten Event und lassen sich genauso
  korrigieren.

## Vorbereitung
1. `docker compose up -d` (nach einem Pull mit Realm-Änderungen einmal `docker compose up -d --force-recreate keycloak`)
2. `cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev` (Liquibase ergänzt Spalten in `tenant`)
3. `cd frontend && npm install && npm start`

Testbenutzer: siehe AGENTS.md "Dev users" (Passwort = Benutzername). Für die Demo brauchst du `operator`.
Gibt es das Kürzel `korrektur-studio` schon aus einem früheren Durchlauf, hänge überall `-2` an.

## Schritt für Schritt

### Schritt 1: Als Betreiber anmelden
- **Was tun:** http://localhost:4200 öffnen, oben rechts **„Plattform-Login“**, Benutzername `operator`, Passwort
  `operator`.
- **Was du siehst:** Die Studio-Liste der Plattform-Verwaltung. Rechts in jeder Zeile steht jetzt zuerst ein Stift
  (Tooltip „Bearbeiten“), dann das Schloss zum Sperren.

### Schritt 2: Studio mit falscher Admin-E-Mail registrieren
- **Was tun:** **„Studio registrieren“** – Name `Korrektur-Studio`, Kürzel `korrektur-studio`, E-Mail
  `admin@studio-a.test` (gehört schon dem Admin von Studio A), Vorname `Maria` – **„Registrieren“**.
- **Was du siehst:** Nach wenigen Sekunden steht in der Zeile „Korrektur-Studio“ rot „Fehlgeschlagen“ mit dem Grund
  „Die E-Mail-Adresse admin@studio-a.test gehört bereits zu Studio „Studio A“ (studio-a). …“, darunter
  „Letzter Versuch … – wird automatisch wiederholt.“ und der Link **„Daten korrigieren“**.

### Schritt 3: Direkt aus der Fehlermeldung ins Formular
- **Was tun:** In der Zeile „Korrektur-Studio“ auf **„Daten korrigieren“** klicken.
- **Was du siehst:** Die Seite „Korrektur-Studio bearbeiten“ (URL `/plattform/studios/<id>`). Oben ein roter Kasten
  „Onboarding fehlgeschlagen“ mit dem Grund und dem Hinweis, dass nach dem Speichern sofort erneut versucht wird.
  Das Kürzel `korrektur-studio` ist ausgegraut („Lässt sich nicht ändern“), E-Mail `admin@studio-a.test` und
  Vorname `Maria` sind ausgefüllt.

### Schritt 4: Gleiche Prüfregeln wie beim Registrieren
- **Was tun:** Die E-Mail durch `kein-gueltiger-wert` ersetzen.
- **Was du siehst:** Schon beim Tippen rot unter dem Feld: „Bitte eine gültige E-Mail-Adresse eingeben, z. B.
  name@beispiel.de“.

### Schritt 5: Korrigieren und speichern
- **Was tun:** E-Mail `inhaber@korrektur-studio.test`, Name `Korrektur-Studio Sonnenschein`, dann **„Speichern“**.
- **Was du siehst:** Zurück in der Liste, Meldung „„Korrektur-Studio Sonnenschein“ wurde gespeichert. Das Onboarding
  wird mit den neuen Daten erneut versucht.“ Nach wenigen Sekunden – ohne Neuladen – zeigt die Zeile den neuen Namen
  und „Abgeschlossen“; der Fehler ist verschwunden.

### Schritt 6: Einladung an die neue Adresse
- **Was tun:** Mailpit öffnen: http://localhost:8025
- **Was du siehst:** Eine Einladung „Willkommen bei Photoffice – bitte richten Sie Ihren Zugang ein“ an
  `inhaber@korrektur-studio.test` (an `admin@studio-a.test` ging nichts).

### Schritt 7: Nach dem Onboarding nur noch der Name
- **Was tun:** In der Zeile „Korrektur-Studio Sonnenschein“ auf den Stift klicken.
- **Was du siehst:** Keine roten Hinweise, unter „Erster Studio-Admin“ nur der Text „Das Onboarding ist
  abgeschlossen: … Seine Daten werden hier nicht mehr gespeichert.“ – keine Eingabefelder mehr.
- **Was tun:** Name `Korrektur-Studio Final`, **„Speichern“**.
- **Was du siehst:** Meldung „„Korrektur-Studio Final“ wurde gespeichert.“, die Zeile zeigt den neuen Namen.

### Schritt 8: Name in Keycloak
- **Was tun:** http://localhost:8180/admin öffnen, mit `admin`/`admin` anmelden, Realm **photoffice** wählen, links
  **Organizations** (Organisationen).
- **Was du siehst:** Die Organisation `korrektur-studio` mit der Beschreibung „Korrektur-Studio Final“.

### Schritt 9: Admin-Daten nach dem Onboarding per API (409)
- **Was tun:** Im Terminal (ID aus der Adresszeile von Schritt 7 einsetzen):
  ```bash
  export OP=$(curl -s localhost:8180/realms/photoffice/protocol/openid-connect/token \
    -d grant_type=password -d client_id=photoffice-dev-cli -d username=operator -d password=operator \
    | python3 -c "import sys,json;print(json.load(sys.stdin)['access_token'])")
  curl -s -X PATCH localhost:8080/api/platform/tenants/<id> -H "Authorization: Bearer $OP" \
    -H 'Content-Type: application/json' -d '{"name":"Korrektur-Studio Final","adminEmail":"neu@example.test"}'
  ```
- **Was du siehst:** `"status":409` und `"detail":"Das Onboarding von „Korrektur-Studio Final“ ist bereits
  abgeschlossen – die Daten des ersten Studio-Admins lassen sich nicht mehr ändern."`

## Automatisch abgesichert
- Playwright `frontend/e2e/platform.spec.ts`: „correct the admin of a failed onboarding …“ (Schritte 2–5 und 7) und
  „unknown studio id in the edit url leads back to the list“.
- Backend: `PlatformTenantControllerTests` (Lesen, Ändern, 409 nach Onboarding, Validierung, Rechte, Admin-Daten
  werden nach dem Onboarding gelöscht), `StudioOnboardingIntegrationTests` (Onboarding scheitert an fremder E-Mail →
  E-Mail per `PATCH` korrigiert → abgeschlossen, Einladung an die neue Adresse; Umbenennen ändert die Beschreibung
  der Organisation), `StudioOnboardingAdminDataTests` (aktuelle Daten statt Event, Rückfall auf alte Events),
  `TenantRegisteredCompatibilityTests` (alte Event-Payloads bleiben lesbar, neue ohne personenbezogene Daten).
- Frontend-Unit-Tests: `studio-form.spec.ts` (Bearbeiten, nur Name nach Onboarding, 409, unbekannte ID),
  `studio-list.spec.ts` (Links „Bearbeiten“ und „Daten korrigieren“).
- Übernahme alter Zustellungen: lokal mit dem hängenden „Konflikt-Studio“ aus Demo #35 geprüft (Admin-Daten aus dem
  Event übernommen, nach Korrektur der E-Mail „Abgeschlossen“).

## Noch offen / Einschränkungen
- Hat ein früherer Versuch schon einen Keycloak-Benutzer mit der falschen E-Mail angelegt, bleibt er bestehen
  (Aufräumen in Keycloak von Hand, ADR 0012).
- Studios löschen geht weiterhin nicht; Test-Studios sammeln sich in der lokalen Datenbank.
- Suche und Seiten in der Studio-Liste: #45.

## Aufräumen
Backend und Frontend beenden (Strg+C), `docker compose down`.
