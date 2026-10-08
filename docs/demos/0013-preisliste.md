# Demo #13: Preisliste und Produkte

- **Issue / PR:** #13 / #34
- **Datum:** 2026-10-08
- **Agent / Maschine:** Claude Code (zweite Session) / fin-de-nb-0072

## Was wurde umgesetzt
- Neue Seite **Preisliste** im Studio-Bereich mit fünf Abschnitten: Allgemein (Währung, Umsatzsteuer), Abzüge
  (Papier × Format), Downloads (Einzelpreis je Auflösung), Download-Pakete („10 Downloads“, „ganze Galerie“) und
  Versandarten.
- Alle Preise sind Endpreise inkl. USt., exakt in Cent gespeichert. Eingaben wie `2,9` oder `12.90` werden
  korrekt übernommen, `4,999` wird abgelehnt.
- Einträge lassen sich **deaktivieren** (bleiben gepflegt, werden Kunden nicht angeboten) oder löschen.
- Jede Kombination Papier × Format, jede Download-Auflösung, jeder Paket- und Versandart-Name gibt es pro Studio
  nur einmal.
- **Berechtigung:** Alle im Studio sehen die Preisliste, nur Studio-Administratoren können sie ändern.
- **Studios getrennt:** Studio B sieht und ändert nichts von Studio A.
- Testdaten: Studio A mit vollständiger Preisliste (19 % USt., je ein inaktiver Abzug/Paket/Versand), Studio B
  klein und als Kleinunternehmer (0 % USt.).
- Modell und Gründe: ADR `docs/decisions/0007-preismodell.md`.

## Vorbereitung
1. `docker compose up -d postgres keycloak`
2. `cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`
3. `cd frontend && npm start`
Testbenutzer: siehe AGENTS.md "Dev users" (Passwort = Benutzername).

## Schritt für Schritt

### Schritt 1: Preisliste öffnen
- **Was tun:** http://localhost:4200 → "Studio-Login" → `admin-a` / `admin-a`; links "Preisliste".
- **Was du siehst:** Seite "Preisliste" mit Untertitel „… alle Preise sind Endpreise inkl. Umsatzsteuer“.
  Abschnitt "Allgemein": Währung „Euro (EUR)“, Umsatzsteuer „19 %“. Abschnitt "Abzüge" mit sechs Zeilen, z. B.
  „Glänzend | 10 × 15 cm | 1,90 € | Aktiv“ und „Fine Art | 30 × 45 cm | 24,90 € | Inaktiv“ (grau/kursiv).
  "Downloads": „Web-Auflösung 4,90 €“, „Volle Auflösung 9,90 €“ – ein Button „Download hinzufügen“ fehlt, weil
  beide Auflösungen schon einen Preis haben. "Download-Pakete": „5 Downloads Web“ (inaktiv), „10 Downloads“
  (10 Bilder, 69,00 €), „Ganze Galerie“ (149,00 €). "Versandarten": „Abholung im Studio 0,00 €“,
  „Standardversand 4,90 €“, „Expressversand 12,90 €“ (inaktiv).

### Schritt 2: Abzug anlegen (Kurzschreibweise und Betrag mit Komma)
- **Was tun:** "Abzug hinzufügen" → Papier `Matt`, Format `9x13`, Preis `1,5` → "Speichern".
- **Was du siehst:** Meldung „„Abzug Matt, 9 × 13 cm“ wurde angelegt.“, zurück in der Preisliste. Das Format wurde
  einheitlich als „9 × 13 cm“ gespeichert. Die neue Zeile steht nach Preis sortiert ganz oben:
  „Matt | 9 × 13 cm | 1,50 € | Aktiv“.

### Schritt 3: Ungültige Eingaben und doppelte Kombination
- **Was tun:** "Abzug hinzufügen" → sofort "Speichern". Dann tippen: Papier `308`, Format `13`, Preis `2,999`.
  Danach korrigieren: Papier `matt`, Format `13x18`, Preis `3` → "Speichern". Danach "Abbrechen".
- **Was du siehst:** Zuerst „Pflichtfeld“ unter allen drei Feldern. Beim Tippen sofort: unter Papier „Beginnt mit
  einem Buchstaben; …“, unter Format „Breite x Höhe in cm (z. B. 13 x 18 oder 10,5 x 15) oder DIN A0 bis A6“, unter
  Preis „Bitte einen Betrag zwischen 0,00 und 100.000,00 eingeben, z. B. 12,90“. Nach der Korrektur unter Format:
  „Den Abzug „matt, 13 × 18 cm“ gibt es bereits.“ – „13x18“ und „13 × 18 cm“ sind dasselbe Format,
  Groß-/Kleinschreibung zählt nicht.

### Schritt 4: Abzug ändern und deaktivieren
- **Was tun:** In "Abzüge" auf „Matt“ in der Zeile „9 × 13 cm“ klicken → Preis `1,75`, Schalter „Kunden anbieten
  (aktiv)“ ausschalten → "Speichern".
- **Was du siehst:** „„Abzug Matt, 9 × 13 cm“ wurde gespeichert.“; die Zeile zeigt „1,75 €“ und „Inaktiv“.

### Schritt 5: Download-Paket „ganze Galerie“
- **Was tun:** "Paket hinzufügen" → Name `Alle Bilder Web`, Inhalt „Ganze Galerie“ wählen, Auflösung
  „Web-Auflösung“, Paketpreis `79` → "Speichern".
- **Was du siehst:** Nach der Auswahl „Ganze Galerie“ verschwindet das Feld „Anzahl Bilder“. Nach dem Speichern
  steht „Alle Bilder Web | Ganze Galerie | Web-Auflösung | 79,00 €“ in "Download-Pakete".
  Optional: noch einmal mit Name `alle bilder web` → Fehler am Namen „Ein Paket mit dem Namen … gibt es bereits.“

### Schritt 6: Versandart und Löschen
- **Was tun:** "Versandart hinzufügen" → Name `Kurier`, Kosten `9,90` → "Speichern". Dann in der Zeile „Kurier“ das
  Papierkorb-Symbol → im Dialog "Löschen".
- **Was du siehst:** „„Kurier“ wurde angelegt.“, dann ein Bestätigungsdialog mit dem Hinweis, dass man Einträge
  auch nur deaktivieren kann; nach "Löschen" „„Kurier“ wurde gelöscht.“ und die Zeile ist weg.

### Schritt 7: Steuersatz ändern
- **Was tun:** "Steuersatz ändern" → `7,5` → "Speichern". Danach wieder auf `19` zurückstellen.
- **Was du siehst:** In "Allgemein" steht „7,5 %“, danach wieder „19 %“. Eingaben wie `19,123` werden mit
  „Ungültiges Format“ abgelehnt.

### Schritt 8: Fotograf sieht die Preisliste nur
- **Was tun:** Rechts oben Benutzermenü → "Abmelden"; als `foto-a` / `foto-a` anmelden → "Preisliste".
- **Was du siehst:** Dieselbe Preisliste (inkl. deiner Änderungen aus Schritt 2–5), darüber „Nur
  Studio-Administratoren können die Preisliste ändern.“ Keine Buttons „… hinzufügen“, keine Bearbeiten-/Löschen-
  Symbole, Namen sind keine Links. (Die API lehnt Änderungen durch Fotografen mit 403 ab.)

### Schritt 9: Studio B ist getrennt
- **Was tun:** Abmelden; als `admin-b` / `admin-b` anmelden → "Preisliste". Danach in die Adresszeile
  `http://localhost:4200/studio/preisliste/produkte/e1000000-0000-4000-8000-000000000003` eingeben (ein Abzug von
  Studio A).
- **Was du siehst:** Umsatzsteuer „0 %“, nur ein Abzug („Glänzend 10 × 15 cm 2,50 €“), ein Download, das Paket
  „Alle Bilder“ und die Versandart „Briefversand“ – nichts von Studio A. Die direkte Adresse zeigt die Meldung „Das
  Produkt wurde nicht gefunden.“ und führt zurück zur Preisliste.

### Schritt 10 (optional): API mit curl
- **Was tun:**
  ```bash
  TOKEN=$(curl -s -d grant_type=password -d client_id=photoffice-dev-cli -d username=foto-a -d password=foto-a \
    http://localhost:8180/realms/photoffice/protocol/openid-connect/token | jq -r .access_token)
  curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/studio/price-list | jq '.settings, .products[0]'
  curl -s -o /dev/null -w '%{http_code}\n' -X POST -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
    -d '{"name":"Express","priceCents":990}' http://localhost:8080/api/studio/price-list/shipping-methods
  ```
- **Was du siehst:** Einstellungen mit `"currency": "EUR"` und `"vatRatePercent"` 19, das günstigste Produkt mit
  `"priceCents": 150` (aus Schritt 2) bzw. `190` ohne Schritt 2;
  der POST als Fotograf liefert `403`.

## Automatisch abgesichert
- Backend: `PriceListApiTests` (CRUD aller Eintragsarten, Validierung je Produkttyp, Cent-Grenzen inkl. Ablehnung von
  `4.9`, Konflikte, Sortierung, Steuersatz, nur aktive Einträge in `offer()`, Fotograf liest/403 beim Ändern) und
  `PriceListIsolationTests` (Studio B sieht/ändert nichts von A, gleiche Einträge in beiden Studios erlaubt);
  `TenantIsolationCoverageTests` prüft RLS der vier neuen Tabellen.
- Frontend-Unit-Tests: `money.spec.ts`, `price-list-page.spec.ts`, `product-form.spec.ts`,
  `download-package-form.spec.ts`, `shipping-method-form.spec.ts`, `price-list-settings-form.spec.ts`.
- E2E: `frontend/e2e/price-list.spec.ts` (Schritte 1–9).

## Noch offen / Einschränkungen
- Kunden sehen die Preisliste noch nicht – dafür fehlen Galerie-Zugang und Shop (#8, #14). Die Grundlage
  (`PriceListManagement.offer()`, nur aktive Einträge) ist vorhanden.
- Preise pro Galerie überschreiben: nicht umgesetzt (Galerien gibt es noch nicht), siehe ADR 0007.
- Zahlungsarten (Legacy F7) gehören zum Bezahldienst und kommen später.
- Papier und Format sind Freitext; Tippvarianten („13x18“) sind möglich.

## Aufräumen
`docker compose down`, Backend und Frontend beenden.
