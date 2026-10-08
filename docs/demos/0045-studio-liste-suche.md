# Demo #45: Studio-Liste der Plattform-Verwaltung – Suche, Filter und Seiten

- **Issue / PR:** #45 / (PR folgt)
- **Datum:** 2026-10-09
- **Agent / Maschine:** Claude Code / fin-de-nb-0061

## Was wurde umgesetzt
- **Suche:** Über der Studio-Liste sucht ein Feld in **Name, Kürzel und E-Mail des ersten Admins** (Groß-/
  Kleinschreibung egal, serverseitig). Die Admin-E-Mail ist nur bis zum Abschluss des Onboardings gespeichert und
  damit nur so lange auffindbar (Datensparsamkeit, #42).
- **Filter:** „Status“ (Aktiv/Gesperrt) und „Onboarding“ (Nicht abgeschlossen/Fehlgeschlagen/Abgeschlossen). Gibt es
  fehlgeschlagene Onboardings, führt **„Nur fehlgeschlagene anzeigen“** direkt dorthin.
- **Seiten:** 25 Studios pro Seite (10/25/50/100 wählbar), sortiert nach Name.
- **Zustand in der Adresszeile** (wie bei den Kunden): Suche, Filter, Seite und Seitengröße bleiben beim Neuladen und
  beim Zurückkommen aus dem Bearbeiten-Formular erhalten. Nach dem Registrieren zeigt die Liste das neue Studio
  (Suche nach seinem Kürzel).
- Die Liste aktualisiert sich weiter von selbst, solange auf der Seite ein Onboarding läuft.
- **API:** `GET /api/platform/tenants?search=&status=&onboarding=&page=&size=` liefert
  `{items, page, size, totalElements, failedOnboardings}` statt einer Liste aller Studios.

## Vorbereitung
1. `docker compose up -d`
2. `cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`
3. `cd frontend && npm install && npm start`

Testbenutzer: siehe AGENTS.md "Dev users" (Passwort = Benutzername). Für die Demo brauchst du `operator`.
Die Schritte legen drei Studios mit dem Zusatz `demo45` an; gibt es sie schon aus einem früheren Durchlauf, nimm
einen anderen Zusatz (z. B. `demo46`) und ersetze ihn in allen Schritten.

## Schritt für Schritt

### Schritt 1: Testdaten per API anlegen
- **Was tun:** Im Terminal:
  ```bash
  export OP=$(curl -s localhost:8180/realms/photoffice/protocol/openid-connect/token \
    -d grant_type=password -d client_id=photoffice-dev-cli -d username=operator -d password=operator \
    | python3 -c "import sys,json;print(json.load(sys.stdin)['access_token'])")
  for s in "Atelier Licht demo45:demo45-atelier:inhaber@demo45-atelier.test" \
           "Bildwerk demo45:demo45-bildwerk:inhaber@demo45-bildwerk.test" \
           "Conflict demo45:demo45-konflikt:admin@studio-a.test"; do
    IFS=: read name slug mail <<< "$s"
    curl -s -o /dev/null -w "%{http_code}\n" localhost:8080/api/platform/tenants -H "Authorization: Bearer $OP" \
      -H 'Content-Type: application/json' -d "{\"name\":\"$name\",\"slug\":\"$slug\",\"adminEmail\":\"$mail\"}"
  done
  ```
- **Was du siehst:** Dreimal `201`.

### Schritt 2: Liste mit Suchfeld und Filtern
- **Was tun:** http://localhost:4200 öffnen, **„Plattform-Login“**, `operator`/`operator`.
- **Was du siehst:** Die Studio-Liste: unter der Überschrift „Studios“ die Anzahl („N Studios, davon M mit
  fehlgeschlagenem Onboarding“), darunter das Suchfeld „Name, Kürzel oder Admin-E-Mail suchen“, die Auswahlfelder
  „Status“ und „Onboarding“ und rot **„Nur fehlgeschlagene anzeigen“**. Unter der Tabelle die Seitenleiste
  „Einträge pro Seite 25 … 1 – … von N“.

### Schritt 3: Suchen
- **Was tun:** Ins Suchfeld `DEMO45` tippen.
- **Was du siehst:** Nach kurzer Pause nur noch drei Zeilen – „Atelier Licht demo45“, „Bildwerk demo45“,
  „Conflict demo45“ (nach Name sortiert). Darüber „3 Treffer für „DEMO45““ (plus „insgesamt … mit fehlgeschlagenem
  Onboarding“). Die Adresszeile endet auf `?suche=DEMO45`. „Conflict demo45“ zeigt „Fehlgeschlagen“.

### Schritt 4: Nach Onboarding filtern
- **Was tun:** Auswahl **„Onboarding“** öffnen und **„Fehlgeschlagen“** wählen.
- **Was du siehst:** Nur noch „Conflict demo45“; die Adresse enthält `onboarding=fehlgeschlagen`. Danach wieder
  **„Alle“** wählen – drei Zeilen.

### Schritt 5: Seiten
- **Was tun:** Unten bei „Einträge pro Seite“ **10** wählen (die Adresse enthält jetzt `anzahl=10`). Dann in der Adresszeile
  `http://localhost:4200/plattform/studios?suche=demo45&anzahl=2` aufrufen und auf den Pfeil **„Nächste Seite“**
  klicken.
- **Was du siehst:** Mit `anzahl=2` zwei Zeilen und „1 – 2 von 3“; nach „Nächste Seite“ nur „Conflict demo45“,
  „3 – 3 von 3“, die Adresse enthält `seite=1`.

### Schritt 6: Neu laden und aus dem Formular zurück
- **Was tun:** Seite neu laden (F5). Dann in der Zeile „Conflict demo45“ auf den Stift klicken und im Formular
  **„Abbrechen“**.
- **Was du siehst:** Nach dem Neuladen dieselbe Ansicht (Suche `demo45`, Seite 2, „3 – 3 von 3“). Nach „Abbrechen“
  wieder genau diese Ansicht (`?suche=demo45&seite=1&anzahl=2`).

### Schritt 7: Suche nach Kürzel und Admin-E-Mail
- **Was tun:** Suchfeld leeren (x) und `demo45-bildwerk` eingeben. Danach `admin@studio-a` eingeben.
- **Was du siehst:** Erst nur „Bildwerk demo45“ (über das Kürzel). Dann die Studios, deren Onboarding an
  `admin@studio-a.test` gescheitert ist – u. a. „Conflict demo45“ (die Admin-E-Mail ist gespeichert, solange das
  Onboarding nicht abgeschlossen ist). „Studio A“ selbst erscheint nicht: Nach dem Onboarding ist keine Admin-E-Mail
  mehr am Studio gespeichert.

### Schritt 8: Keine Treffer
- **Was tun:** `gibt-es-nicht-demo45` eingeben.
- **Was du siehst:** „Keine Treffer – Passen Sie Suchbegriff oder Filter an.“ statt der Tabelle.

### Schritt 9: „Nur fehlgeschlagene anzeigen“
- **Was tun:** Suchfeld leeren, dann **„Nur fehlgeschlagene anzeigen“** klicken.
- **Was du siehst:** Nur Studios mit „Fehlgeschlagen“, darüber „N Studios gefunden“; die Auswahl „Onboarding“ steht
  auf „Fehlgeschlagen“, der Knopf ist verschwunden.

### Schritt 10: Registrieren zeigt das neue Studio
- **Was tun:** **„Studio registrieren“** – Name `Demo 45 Neu`, Kürzel `demo45-neu`, E-Mail
  `inhaber@demo45-neu.test` – **„Registrieren“**.
- **Was du siehst:** Die Liste mit `?suche=demo45-neu`: genau die neue Zeile, nach wenigen Sekunden ohne Neuladen
  „Abgeschlossen“.

### Schritt 11: API
- **Was tun:** `curl -s "localhost:8080/api/platform/tenants?search=demo45&size=2&page=1" -H "Authorization: Bearer $OP" | python3 -m json.tool`
  und `curl -s "localhost:8080/api/platform/tenants?status=GELOESCHT" -H "Authorization: Bearer $OP"`
- **Was du siehst:** Erst `"page": 1`, `"size": 2`, `"totalElements": 4` („Demo 45 Neu“ ist dazugekommen),
  `"failedOnboardings": …` und in `items` die Studios 3 und 4: „Conflict demo45“ und „Demo 45 Neu“. Dann `"status":400` mit
  `"detail":"Ungültiger Wert „GELOESCHT“ für den Parameter „status“ – erlaubt: [ACTIVE, SUSPENDED]."`

## Automatisch abgesichert
- Playwright `frontend/e2e/platform-studio-list.spec.ts` (Schritte 3–6, 8, 9) und `platform.spec.ts`
  (Registrieren zeigt das neue Studio, Zurück aus dem Formular in dieselbe Ansicht).
- Backend `PlatformTenantListTests`: Suche in Name/Kürzel/Admin-E-Mail ohne Groß-/Kleinschreibung, `%`/`_` wörtlich,
  Seiten, Filter Status/Onboarding, Admin-E-Mail nach dem Onboarding nicht mehr auffindbar, ungültige Parameter.
- Frontend-Unit-Tests `studio-list.spec.ts` (Suche, Seite, URL, „Nur fehlgeschlagene“, Wiederherstellen aus der URL,
  leere Seite) und `studio-form.spec.ts` (Rückkehr in dieselbe Listenansicht).

## Noch offen / Einschränkungen
- Sortierung nur nach Name (keine sortierbaren Spalten).
- Studios löschen geht weiterhin nicht; Test-Studios sammeln sich in der lokalen Datenbank.

## Aufräumen
Backend und Frontend beenden (Strg+C), `docker compose down`.
