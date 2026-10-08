# Demo #7: Kundenverwaltung

- **Issue / PR:** #7 / (siehe PR zu #7)
- **Datum:** 2026-10-08
- **Agent / Maschine:** Claude Code / fin-de-nb-0072

## Was wurde umgesetzt
- Studios verwalten ihre Endkunden: Liste mit Suche (Name, E-Mail, Ort) und Seitenwechsel, Kunden anlegen,
  bearbeiten (Kontakt, Adresse, interne Notizen) und löschen (mit Bestätigung).
- E-Mail-Adressen sind pro Studio eindeutig (Groß-/Kleinschreibung egal) – derselbe Kunde darf bei einem anderen
  Studio existieren.
- **Strikte Trennung der Studios:** Studio B sieht und ändert keine Kunden von Studio A – weder in der Liste noch
  über eine direkt eingegebene Adresse (Datenbank-Absicherung aus #5).
- Testdaten: 12 Kunden in Studio A, 2 in Studio B.

## Vorbereitung
1. `docker compose up -d postgres keycloak`
2. `cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`
3. `cd frontend && npm start`

## Schritt für Schritt

### Schritt 1: Kundenliste
- **Was tun:** http://localhost:4200 → "Studio-Login" → `admin-a` / `admin-a`; links "Kunden".
- **Was du siehst:** Seite "Kunden" mit "12 Kunden", Button "Neuer Kunde", Suchfeld und Tabelle (Name, E-Mail,
  Telefon, Ort), alphabetisch nach Nachname: "Becker, Julia", "Braun, Ben", "Hoffmann, Marie" …

### Schritt 2: Suchen
- **Was tun:** Im Suchfeld `berlin` eingeben.
- **Was du siehst:** "4 Treffer für „berlin“" – Becker, Klein, Schmidt, Zimmermann. Die Adresszeile enthält
  `?suche=berlin` (Suche bleibt beim Zurückkommen erhalten). Suche danach mit dem X leeren.

### Schritt 3: Kunden anlegen
- **Was tun:** "Neuer Kunde" → Vorname `Erika`, Nachname `Muster`, E-Mail `erika@example.test`, Ort `Görlitz` → "Speichern".
- **Was du siehst:** Meldung "„Erika Muster“ wurde angelegt.", zurück in der Liste mit "13 Kunden" und "Muster, Erika".

### Schritt 4: Pflichtfelder und doppelte E-Mail
- **Was tun:** "Neuer Kunde" → sofort "Speichern". Dann Vorname `Doppelt`, Nachname `Becker`,
  E-Mail `JULIA.BECKER@example.test` → "Speichern". Danach "Abbrechen".
- **Was du siehst:** Zuerst "Pflichtfeld" unter Vorname, Nachname und E-Mail. Danach unter E-Mail:
  "Ein Kunde mit der E-Mail-Adresse julia.becker@example.test existiert bereits." – Groß-/Kleinschreibung zählt nicht.

### Schritt 5: Kunden bearbeiten
- **Was tun:** In der Liste auf "Becker, Julia" klicken; Telefon ändern auf `0171 7654321` → "Speichern".
- **Was du siehst:** Formular mit Überschrift "Julia Becker" und allen Daten (Lindenstraße 4, 10969 Berlin, Notiz
  "Hochzeit Juni"). Nach dem Speichern: "„Julia Becker“ wurde gespeichert." und die neue Nummer in der Liste.

### Schritt 6: Kunden löschen
- **Was tun:** Bei "Muster, Erika" den Papierkorb → im Dialog "Löschen".
- **Was du siehst:** Dialog "Kunden löschen?", danach "„Erika Muster“ wurde gelöscht." und wieder "12 Kunden".

### Schritt 7: Anderes Studio sieht diese Kunden nicht
- **Was tun:** Benutzermenü → "Abmelden"; als `admin-b` / `admin-b` anmelden → "Kunden"; dann nach `Becker` suchen.
- **Was du siehst:** Nur "2 Kunden" (Fuchs, Vogel). Die Suche nach Becker ergibt "Keine Treffer".

### Schritt 8: Auch der direkte Link ist gesperrt
- **Was tun:** Als `admin-b` in der Adresszeile aufrufen:
  http://localhost:4200/studio/kunden/c0000000-0000-4000-8000-000000000001 (Julia Becker aus Studio A).
- **Was du siehst:** Meldung "Der Kunde wurde nicht gefunden." und Rückkehr zur eigenen Kundenliste – die Daten von
  Studio A sind für Studio B nicht vorhanden.

## Automatisch abgesichert
- Backend (49 Tests): u. a. Anlegen/Lesen/Ändern/Löschen, Suche inkl. Sonderzeichen, Seitenwechsel, doppelte E-Mail,
  Validierung, und 3 Tests zur Trennung der Studios.
- Frontend: 9 Unit-Tests für Liste/Formular; Playwright (4 Kunden-Szenarien inkl. Studio-Trennung).

## Noch offen / Einschränkungen
- Zuordnung von Kunden zu Galerien folgt mit #8 (das Löschen eines Kunden meldet bereits ein Ereignis dafür).
- Optionales Kundenkonto (F20) später – das Datenmodell ist darauf vorbereitet.
- Keine Warnung bei ungespeicherten Änderungen beim Verlassen des Formulars.

## Aufräumen
Frontend und Backend beenden (Strg+C), `docker compose down`.
