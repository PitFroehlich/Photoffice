# Demo #20: Studio-Profil und Rechtstexte

- **Issue / PR:** #20 / #40
- **Datum:** 2026-10-08
- **Agent / Maschine:** Claude Code (zweite Session) / fin-de-nb-0072

## Was wurde umgesetzt
- Neue Seite **Studio-Profil** (`/studio/profil`): Anzeigename, Adresse (Straße mit Hausnummer, PLZ, Ort, Land
  DE/AT/CH), Kontakt (E-Mail, Telefon, Website), Steuern (Steuernummer, USt-IdNr./UID) und Bankverbindung
  (Kontoinhaber, IBAN, BIC).
- **Formatprüfung beim Tippen**, identisch im Backend: PLZ passend zum Land (DE 5, AT/CH 4 Ziffern), IBAN mit
  Prüfsumme, BIC 8/11 Zeichen, USt-IdNr. `DE…`/`ATU…`/`CHE-…`, Website, E-Mail, Telefon. Kontoinhaber und IBAN nur
  gemeinsam. Das Backend speichert einheitlich (IBAN ohne Leerzeichen, USt-IdNr. in Großbuchstaben, `https://`
  vor der Website).
- Neue Seite **Rechtstexte** (`/studio/rechtstexte`) mit Reitern AGB, Widerruf, Impressum, Datenschutz:
  Markdown-Editor mit **Live-Vorschau**, Gliederungsvorschläge (Impressum aus dem Studio-Profil vorbefüllt),
  Hinweis „keine Rechtsberatung“. HTML im Text wird nur als Text angezeigt, nie ausgeführt.
- **Berechtigung:** Alle im Studio sehen Profil und Texte, nur Studio-Administratoren ändern sie.
- **Studios getrennt:** Studio B sieht nichts von Studio A.
- Noch nicht: Anzeige für Kunden (kommt mit #12/#14), Logo (#39).
- Entscheidungen: ADR `docs/decisions/0011-rechtstexte-markdown.md`.

## Vorbereitung
1. `docker compose up -d`
2. `cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`
3. `cd frontend && npm start`
Testbenutzer: siehe AGENTS.md "Dev users" (Passwort = Benutzername).

## Schritt für Schritt

### Schritt 1: Studio-Profil öffnen
- **Was tun:** http://localhost:4200 → "Studio-Login" → `admin-a` / `admin-a`; links in der Navigation
  "Studio-Profil".
- **Was du siehst:** Seite "Studio-Profil" mit den Abschnitten Studio, Adresse, Kontakt, Steuern, Bankverbindung.
  Vorbefüllt: „Studio A Fotografie“, „Lindenstraße 4“, „10969“ „Berlin“, Land „Deutschland“,
  „kontakt@studio-a.example“, „030 1234567“, „https://studio-a.example“, Steuernummer „12/345/67890“,
  USt-IdNr. „DE123456789“, Kontoinhaber „Studio A Fotografie GmbH“, IBAN „DE89370400440532013000“,
  BIC „COBADEFFXXX“. Unten „Zuletzt gespeichert: …“ und der Button "Speichern".

### Schritt 2: Formatprüfung beim Tippen
- **Was tun:** Nacheinander tippen (nicht speichern):
  IBAN letzte Ziffer von `0` auf `1` ändern; PLZ `1060`; danach Land auf „Österreich“ stellen;
  USt-IdNr. `DE12345`; Website `studio`.
- **Was du siehst:** Sofort unter dem Feld: „Bitte eine gültige IBAN eingeben (Prüfsumme), z. B. DE89 3704 0044
  0532 0130 00“; unter PLZ „5 Ziffern für Deutschland“ – nach dem Wechsel auf Österreich verschwindet der Fehler
  (4 Ziffern passen); unter USt-IdNr. „z. B. DE123456789, ATU12345678 oder CHE-123.456.789 MWST“; unter Website
  „Bitte eine Internetadresse eingeben, z. B. www.mein-studio.de“. "Speichern" speichert nichts.
- **Danach:** Seite neu laden (F5) – die gespeicherten Werte sind wieder da.

### Schritt 3: Bankverbindung gehört zusammen
- **Was tun:** IBAN und BIC leeren, Kontoinhaber stehen lassen → "Speichern".
- **Was du siehst:** Unter IBAN „Pflichtfeld“ – ohne IBAN kein Kontoinhaber. Seite neu laden.

### Schritt 4: Speichern – das Backend vereinheitlicht
- **Was tun:** USt-IdNr. `de 987 654 321`, Website `www.studio-a.example`, IBAN `de89 3704 0044 0532 0130 00`
  → "Speichern".
- **Was du siehst:** Meldung „Das Studio-Profil wurde gespeichert.“ Die Felder zeigen jetzt „DE987654321“,
  „https://www.studio-a.example“ und „DE89370400440532013000“; „Zuletzt gespeichert“ zeigt die aktuelle Uhrzeit.
- **Danach:** USt-IdNr. wieder auf `DE123456789` setzen und speichern (Testdaten).

### Schritt 5: Rechtstexte und Vorschau
- **Was tun:** Oben rechts "Rechtstexte" (oder links in der Navigation).
- **Was du siehst:** Hinweis „Ihre Kunden sehen diese Texte später … keine Rechtsberatung …“. Reiter AGB,
  Widerruf, Impressum, Datenschutz. Im Reiter "AGB" links der Markdown-Text, rechts die Vorschau mit Überschrift
  „Allgemeine Geschäftsbedingungen“, kursivem Hinweis, nummerierter Liste und dem Link „Preisliste“.
  "AGB speichern" ist ausgegraut (nichts geändert).

### Schritt 6: HTML wird nicht ausgeführt
- **Was tun:** Ans Ende des AGB-Textes eine Leerzeile und dann `<script>alert('x')</script> [Klick](javascript:alert(1))`
  tippen; in der Vorschau auf „Klick“ klicken.
- **Was du siehst:** Die Vorschau zeigt `<script>alert('x')</script>` als Text, es erscheint kein Popup, auch
  „Klick“ führt nichts aus. Unten „Ungespeicherte Änderungen“, der Reiter heißt „AGB *“.
- **Danach:** Die eingefügte Zeile wieder löschen – „Ungespeicherte Änderungen“ verschwindet.

### Schritt 7: Text aus Gliederung anlegen und wieder entfernen
- **Was tun:** Reiter "Widerruf" → "Gliederung einfügen" → in der ersten Klammer etwas eigenen Text schreiben
  → "Widerruf speichern". Danach den ganzen Text löschen → "Widerruf speichern".
- **Was du siehst:** Nach dem Einfügen Überschriften „Widerrufsbelehrung“, „Widerrufsrecht“, „Folgen des
  Widerrufs“ … mit Platzhaltern in [Klammern], rechts sofort als Vorschau. Meldung „„Widerrufsbelehrung“ wurde
  gespeichert.“, nach dem Leeren „„Widerrufsbelehrung“ wurde entfernt.“

### Schritt 8: Fotograf sieht nur
- **Was tun:** Rechts oben Benutzermenü → "Abmelden"; als `foto-a` anmelden → "Studio-Profil", dann "Rechtstexte",
  Reiter "AGB" und "Widerruf".
- **Was du siehst:** Profil: Hinweis „Nur Studio-Administratoren können das Studio-Profil ändern.“, alle Felder
  ausgegraut, kein "Speichern". Rechtstexte: kein Editor, die AGB fertig formatiert; unter "Widerruf"
  „Widerrufsbelehrung fehlt noch“.

### Schritt 9: Studio B ist getrennt, Impressum aus dem Profil
- **Was tun:** Abmelden, als `admin-b` anmelden → "Studio-Profil"; dann "Rechtstexte" → Reiter "Impressum" →
  "Gliederung einfügen" (nicht speichern).
- **Was du siehst:** Profil von Studio B: „Studio B“, „Mariahilfer Straße 12“, „1060“ „Wien“, Land „Österreich“,
  IBAN „AT611904300234573201“, keine USt-IdNr. – nichts von Studio A. Alle Rechtstexte sind leer (keine AGB von
  Studio A). Die Impressum-Gliederung enthält **Studio B**, Adresse, „Österreich“, Telefon und E-Mail aus dem
  Profil; die Vorschau zeigt die Adresse zeilenweise.

## Automatisch abgesichert
- Backend: `StudioProfileApiTests` (Lesen/Speichern/Normalisieren, Fehlertexte, Rechte Fotograf),
  `StudioProfileIsolationTests` (Studio A/B), `StudioProfileDataTests` + `LegalTextTests` (Formatregeln),
  `StudioProfileDevDataTests` (Testdaten laden und erfüllen die Regeln).
- Frontend: `studio-profile-validators.spec.ts` (gleiche Fälle wie das Backend), `studio-profile-form.spec.ts`,
  `legal-texts-page.spec.ts`, `markdown.spec.ts` (sicheres Rendering).
- E2E: `frontend/e2e/studio-profile.spec.ts` (Schritte 2, 4, 5–9, Studio-Trennung).

## Noch offen / Einschränkungen
- Kunden sehen Profil und Rechtstexte noch nicht – öffentliche Auslieferung kommt mit #12 (Kundenansicht) bzw.
  #14 (Checkout), Baustein `MarkdownView` ist dafür vorbereitet.
- Logo: #39 (braucht die Bildablage aus #9).

## Aufräumen
`docker compose down`, Backend und Frontend beenden.
