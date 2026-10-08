# Demo #8: Galerieverwaltung

- **Issue / PR:** #8 / (siehe PR zu #8)
- **Datum:** 2026-10-08
- **Agent / Maschine:** Claude Code / fin-de-nb-0072

## Was wurde umgesetzt
- Studios legen **Galerien** an: Name, Begrüßungstext für Kunden, „Erreichbar bis“-Datum, beliebig viele zugeordnete
  Kunden (Suche mit Chips).
- Status **Entwurf → Online ⇄ Offline**. Veröffentlichen nur, wenn die Galerie nicht abgelaufen ist und die
  Preisliste aktive Einträge hat. Eine Online-Galerie mit überschrittenem Datum wird als **„Abgelaufen“** angezeigt.
- Galerieliste mit Suche, Statusfilter und Filter nach Kunde (Link aus dem Kundenformular); Zustand in der URL.
- Löschen mit Bestätigung; Kunden-Löschung entfernt nur die Zuordnung, die Galerie bleibt.
- Ereignisse für spätere Funktionen: `GalleryPublished` (Link-Mail, #19), `GalleryDeleted` (Bilder löschen, #9).
- Studios strikt getrennt – auch fremde Kunden lassen sich nicht zuordnen.
- Testdaten Studio A: „Hochzeit Becker“ (online), „Familienshooting Wagner“ (Entwurf), „Bewerbungsfotos Koch“
  (offline), „Babybauch Schröder“ (abgelaufen); Studio B: „Porträt Vogel“.

## Vorbereitung
1. `docker compose up -d postgres keycloak`
2. `cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`
3. `cd frontend && npm start`

## Schritt für Schritt

### Schritt 1: Galerieliste
- **Was tun:** http://localhost:4200 → "Studio-Login" → `admin-a` / `admin-a` → links "Galerien".
- **Was du siehst:** "4 Galerien", Tabelle mit Name, Kunden, Status, „Erreichbar bis“. Status als farbige Marke:
  „Online“ (Hochzeit Becker), „Abgelaufen“ (Babybauch Schröder, kupferfarben), „Offline“, „Entwurf“.

### Schritt 2: Filtern
- **Was tun:** Oben „Entwurf“ wählen, danach „Alle“; dann im Suchfeld `hochzeit` eingeben und wieder leeren.
- **Was du siehst:** Bei „Entwurf“ nur „Familienshooting Wagner“, in der Adresszeile `?status=draft`. Die Suche
  findet „Hochzeit Becker“.

### Schritt 3: Neue Galerie mit Prüfungen
- **Was tun:** "Neue Galerie" → Name erst `!!!`, dann `Taufe Neumann`; „Erreichbar bis“ erst `31.02.2030` und
  Feld verlassen, dann `31.12.2030` (oder per Kalender-Symbol wählen); bei „Kunden“ `neum` tippen und
  „Thomas Neumann“ wählen → "Speichern".
- **Was du siehst:** Bei `!!!` sofort „Mindestens ein Buchstabe; …“. Beim ungültigen Datum „Bitte ein Datum im Format
  TT.MM.JJJJ eingeben“; Daten in der Vergangenheit sind im Kalender gesperrt. Thomas Neumann erscheint als Chip.
  Nach dem Speichern „„Taufe Neumann“ wurde angelegt.“ – du bleibst auf der Galerie; rechts die Karte
  „Sichtbarkeit“ mit Status „Entwurf“ und „Erreichbar bis 31.12.2030“.

### Schritt 4: Veröffentlichen und offline nehmen
- **Was tun:** In der Karte „Sichtbarkeit“ auf "Veröffentlichen"; danach "Offline nehmen" und im Dialog bestätigen.
- **Was du siehst:** „„Taufe Neumann“ ist jetzt online.“, Status „Online“. Nach „Offline nehmen“ Status „Offline“
  und wieder der Button „Veröffentlichen“.

### Schritt 5: Ohne Preise keine Veröffentlichung (mit Studio B)
- **Was tun:** Abmelden, als `admin-b` / `admin-b` anmelden → "Preisliste". Die drei Einträge von Studio B
  deaktivieren: Abzug „Glänzend 10 × 15 cm“, Download „Original“ und Paket „Alle Bilder“ (jeweils Name anklicken →
  Schalter „Kunden anbieten (aktiv)“ aus → "Speichern"). Dann "Galerien" → „Porträt Vogel“ → "Veröffentlichen".
  Danach den Abzug wieder aktivieren und erneut "Veröffentlichen".
- **Was du siehst:** Ohne aktive Preise die rote Meldung „Ohne aktive Preise kann die Galerie nicht veröffentlicht
  werden. Legen Sie zuerst in der Preisliste Preise an.“ Mit einem aktiven Eintrag: „„Porträt Vogel“ ist jetzt online.“

### Schritt 6: Abgelaufene Galerie
- **Was tun:** Abmelden, als `admin-a` anmelden → "Galerien" → „Babybauch Schröder“ öffnen.
- **Was du siehst:** Status „Abgelaufen“ und der Hinweis „Das Ablaufdatum ist überschritten – Kunden haben keinen
  Zugriff mehr. Verlängern Sie das Datum …“. (Nicht offline nehmen – eine abgelaufene Galerie kann erst nach
  Verlängern des Datums wieder veröffentlicht werden.)

### Schritt 7: Vom Kunden zu seinen Galerien
- **Was tun:** "Kunden" → „Koch, Jan“ öffnen → oben "Galerien".
- **Was du siehst:** Galerieliste nur mit „Bewerbungsfotos Koch“ und dem Button „Nur Galerien eines Kunden – Filter
  aufheben“.

### Schritt 8: Galerie löschen
- **Was tun:** „Taufe Neumann“ in der Liste → Papierkorb → "Löschen".
- **Was du siehst:** Dialog „Galerie löschen?“ mit Hinweis auf Bilder und Kundenzugriff, danach
  „„Taufe Neumann“ wurde gelöscht.“

### Schritt 9: Studio B ist getrennt
- **Was tun:** Abmelden, als `admin-b` "Galerien"; dann http://localhost:4200/studio/galerien/f1000000-0000-4000-8000-000000000001
  aufrufen (Hochzeit Becker aus Studio A).
- **Was du siehst:** Nur „Porträt Vogel“ (aus Schritt 5 jetzt online). Der direkte Link zeigt „Die Galerie wurde nicht
  gefunden.“

## Automatisch abgesichert
- Backend: 13 Galerie-Tests (u. a. ohne Preise/abgelaufen nicht veröffentlichen, Ereignisse, Filter, Kunden-Löschung)
  und 3 Isolationstests.
- Frontend: Unit-Tests für Liste, Formular (Datum, Namensprüfung, Kundenauswahl, Veröffentlichen) und Datumsadapter;
  Playwright: 5 Galerie-Szenarien.

## Noch offen / Einschränkungen
- Bilder hochladen (#9), Kundenzugang per Link (#11), Mail beim Veröffentlichen (#19).
- „Heute“ wird in deutscher Zeit (Europe/Berlin) bestimmt; Zeitzone pro Studio später.

## Aufräumen
Frontend und Backend beenden (Strg+C), `docker compose down`.
