# Demo #27: UI-Grundlage mit Angular Material und eigenem Design

- **Issue / PR:** #27 / (siehe PR zu #27)
- **Datum:** 2026-10-08
- **Agent / Maschine:** Claude Code / fin-de-nb-0072

## Was wurde umgesetzt
- **Eigenes Photoffice-Design** auf Angular Material: Petrol als Hauptfarbe, Kupfer als Akzent, warme neutrale
  Flächen, Schrift Inter. Schrift und Icons werden selbst ausgeliefert (kein Google-CDN, DSGVO).
- **Öffentlicher Rahmen** (Startseite) und **Studio-Rahmen** mit Kopfzeile (Studio-Name, Benutzermenü mit Abmelden),
  Seitennavigation (am Handy über das Menü-Symbol) und "Zum Inhalt springen" für Tastaturnutzer.
- **Gemeinsame Bausteine** für alle kommenden Seiten: Seitenkopf, Suchfeld, Tabelle mit Seitenwechsel,
  Leerzustand, Ladeanzeige, Formular-Fehlermeldungen auf Deutsch, Bestätigungsdialog, Erfolgs-/Fehlermeldungen.
- **Referenzseite "UI-Bausteine"** (nur in der Entwicklung sichtbar), an der sich alle Agenten orientieren.

## Vorbereitung
1. `docker compose up -d postgres keycloak`
2. `cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`
3. `cd frontend && npm install && npm start`

## Schritt für Schritt

### Schritt 1: Neue Startseite
- **Was tun:** http://localhost:4200 öffnen.
- **Was du siehst:** Helle, warme Fläche; oben links Kamera-Symbol und "Photoffice" in Petrol, rechts der Button
  "Studio-Login". In der Mitte "Ihre Bilder. Ihre Kunden. Ein Ort." mit kurzem Text und dem Button "Als Studio anmelden".

### Schritt 2: Tastaturbedienung
- **Was tun:** Einmal die Tab-Taste drücken.
- **Was du siehst:** Oben links erscheint "Zum Inhalt springen" (nur für Tastaturnutzer). Weiter mit Tab: jeder
  fokussierte Button bekommt einen kupferfarbenen Rahmen.

### Schritt 3: Anmelden und Studio-Rahmen
- **Was tun:** "Als Studio anmelden" → `admin-a`, "Sign In" → Passwort `admin-a`, "Sign In".
- **Was du siehst:** Oben "Photoffice | Studio A" und rechts "Anna Admin". Links die Navigation mit "Übersicht" und
  "UI-Bausteine". Die Übersicht zeigt eine Karte "Willkommen, Anna Admin" mit der Rolle "Studio-Administrator".
  Der Browser-Tab heißt "Übersicht – Photoffice".

### Schritt 4: Benutzermenü
- **Was tun:** Oben rechts auf "Anna Admin" klicken (Menü noch nicht abmelden, mit Esc schließen).
- **Was du siehst:** Menü mit Name, "Studio-Administrator · Studio A" und "Abmelden".

### Schritt 5: Liste mit Suche und Seitenwechsel
- **Was tun:** Links "UI-Bausteine" öffnen.
- **Was du siehst:** Seitenkopf "UI-Bausteine", darunter eine Tabelle mit 5 Beispielkunden und unten
  "Einträge pro Seite 5" und "1 – 5 von 23". Mit dem Pfeil nach rechts blätterst du auf "6 – 10 von 23".

### Schritt 6: Suchen, Laden, Leerzustand
- **Was tun:** In "Name oder Ort suchen" `Leipzig` eingeben; danach den Begriff durch `xyz` ersetzen.
- **Was du siehst:** Kurz ein Ladebalken, dann 3 Treffer ("1 – 3 von 3"). Bei `xyz`: Symbol mit "Keine Treffer" und
  "Passen Sie den Suchbegriff an." Mit dem X im Suchfeld wird die Suche geleert.

### Schritt 7: Löschen mit Bestätigung
- **Was tun:** In der Zeile "Anna Bauer" auf den Papierkorb klicken, zuerst "Abbrechen", dann nochmal und "Löschen".
- **Was du siehst:** Dialog "Eintrag löschen?" mit rotem Button "Löschen". Nach "Abbrechen" bleibt alles, nach
  "Löschen" verschwindet die Zeile, unten rechts erscheint "„Anna Bauer“ wurde gelöscht." und die Liste zeigt "von 22".

### Schritt 8: Formular mit Prüfung
- **Was tun:** Im Bereich "Formular mit Validierung" sofort "Speichern" klicken. Dann Name `Zoe Test`, E-Mail
  `kein-email` eingeben, "Speichern". Dann E-Mail `zoe@example.test`, "Speichern".
- **Was du siehst:** Zuerst bei beiden Feldern "Pflichtfeld" in Rot. Dann bei E-Mail "Bitte eine gültige E-Mail-Adresse
  eingeben". Zuletzt "„Zoe Test“ wurde gespeichert." – und Zoe steht oben in der Liste.

### Schritt 9: Fehlermeldung aus dem Backend
- **Was tun:** Oben rechts "Fehlermeldung zeigen" klicken.
- **Was du siehst:** Rote Meldung "Beispiel: Diese E-Mail-Adresse ist bereits vergeben." – so erscheinen künftig
  Fehler, die das Backend meldet (Problem Details).

### Schritt 10: Handy-Ansicht
- **Was tun:** Browserfenster schmal ziehen (oder Entwicklertools → Gerätemodus, z. B. iPhone).
- **Was du siehst:** Die Navigation verschwindet, oben links erscheint ein Menü-Symbol, das sie als Overlay öffnet.
  Studio- und Benutzername werden ausgeblendet. Die Tabelle lässt sich seitlich wischen, die Seite selbst nicht.

### Schritt 11: Abmelden
- **Was tun:** Benutzermenü → "Abmelden".
- **Was du siehst:** Zurück auf der Startseite mit "Studio-Login".

## Automatisch abgesichert
- Playwright (`frontend/e2e/`): Login/Logout je Rolle über das Benutzermenü, Liste/Suche/Leerzustand, Löschen mit
  Bestätigung, Formularprüfung, Fehlermeldung (9 Tests).
- Unit-Tests (26): Rahmen, Benutzermenü, Sitzung, Bausteine (Suchfeld, Fehlermeldungen, Bestätigungsdialog, API-Fehler).

## Noch offen / Einschränkungen
- Dunkelmodus ist über die Theme-Tokens vorbereitet, aber noch nicht umschaltbar.
- Die Kunden-Galerie (#12) bekommt später einen eigenen, bildbetonten Rahmen auf Basis derselben Tokens.

## Aufräumen
Frontend und Backend beenden (Strg+C), `docker compose down`.
