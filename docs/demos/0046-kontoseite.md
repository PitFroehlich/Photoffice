# Demo #46: Eigenes Konto – Profil und Passwort über das Benutzermenü, Keycloak-Kontoverwaltung abgeschaltet

- **Issue / PR:** #46 / (PR folgt)
- **Datum:** 2026-10-09
- **Agent / Maschine:** Claude Code / fin-de-nb-0061

## Was wurde umgesetzt
- Im Benutzermenü (oben rechts) gibt es im Studio- und im Plattform-Bereich zwei neue Einträge:
  **„Profil bearbeiten“** und **„Passwort ändern“**. Sie öffnen die passende Keycloak-Seite im Photoffice-Design und
  führen danach – auch nach „Abbrechen“ – auf die Seite zurück, von der man kam. Der geänderte Name steht sofort oben
  rechts; nach dem Speichern erscheint eine kurze Bestätigung.
- Benutzer dürfen nur **Vor- und Nachnamen** ändern. Die **E-Mail-Adresse** (= Anmeldung, mit dem Studio verbunden)
  wird schreibgeschützt mit Hinweis angezeigt; ändern kann sie nur ein Admin.
- Die **Keycloak-Kontoverwaltung** (`/realms/photoffice/account`) ist abgeschaltet – Oberfläche und REST-API.
- Entscheidung: ADR 0013.

## Vorbereitung
1. `docker compose up -d --force-recreate keycloak` (lädt den geänderten Realm), danach `docker compose up -d`
2. `cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`
3. `cd frontend && npm install && npm start`

Testbenutzer: siehe AGENTS.md "Dev users" (Passwort = Benutzername). **Wichtig:** In Schritt 6 änderst du das
Passwort von `foto-a` – in Schritt 8 wird es wieder auf `foto-a` zurückgesetzt, sonst passen die Dev-Zugangsdaten
nicht mehr. Mailpit wird nicht gebraucht.

## Schritt für Schritt

### Schritt 1: Kontoverwaltung von Keycloak ist weg
- **Was tun:** http://localhost:8180/realms/photoffice/account öffnen.
- **Was du siehst:** Keycloak-Fehlerseite im Photoffice-Design: „Es ist ein Fehler aufgetreten.“ / „Seite nicht
  gefunden“. Keine Anmeldung, keine Kontoverwaltung.

### Schritt 2: Als Fotograf anmelden und das Menü öffnen
- **Was tun:** http://localhost:4200 → „Studio-Login“ → `foto-a` / `foto-a` (zwei Seiten). Links auf „Kunden“
  klicken, dann oben rechts auf „Felix Foto“.
- **Was du siehst:** Das Benutzermenü mit Name, „Fotograf · Studio A“, darunter **„Profil bearbeiten“**,
  **„Passwort ändern“**, Trennlinie, „Abmelden“.

### Schritt 3: Profil bearbeiten
- **Was tun:** „Profil bearbeiten“ klicken.
- **Was du siehst:** Keycloak-Seite im Photoffice-Design (Kamera-Logo, Karte mit Kupfer-Streifen), Überschrift
  „Profil bearbeiten“, „* Pflichtfelder“. Das Feld „E-Mail-Adresse“ mit `foto@studio-a.test` ist grau hinterlegt und
  nicht bearbeitbar, darunter: „Ihre E-Mail-Adresse ist Ihre Anmeldung und kann hier nicht geändert werden. Für eine
  Änderung wenden Sie sich bitte an Photoffice.“ Darunter „Vorname“ und „Nachname“ (bearbeitbar), Buttons
  „Absenden“ und „Abbrechen“.

### Schritt 4: Namen ändern
- **Was tun:** Nachname auf `Fotograf` ändern, „Absenden“.
- **Was du siehst:** Zurück in Photoffice auf der Kundenliste (`/studio/kunden`), oben rechts „Felix Fotograf“,
  unten rechts kurz „Ihr Profil wurde gespeichert.“

### Schritt 5: Passwort ändern – abbrechen
- **Was tun:** Benutzermenü → „Passwort ändern“, auf der Keycloak-Seite „Abbrechen“.
- **Was du siehst:** Vorher: Überschrift **„Passwort ändern“**, Felder „Neues Passwort“ und „Passwort wiederholen“,
  Kästchen „Von anderen Geräten abmelden“, Buttons „Absenden“ und „Abbrechen“. Nachher: wieder die Kundenliste, keine
  Meldung, nichts geändert.

### Schritt 6: Passwort ändern
- **Was tun:** Benutzermenü → „Passwort ändern“, zweimal `Neu-Geheim-456` eingeben, „Absenden“.
- **Was du siehst:** Zurück auf der Kundenliste mit der Meldung „Ihr Passwort wurde geändert.“ (Liegt die Anmeldung
  länger als 5 Minuten zurück, fragt Keycloak vorher noch einmal nach dem aktuellen Passwort.)

### Schritt 7: Mit dem neuen Passwort anmelden
- **Was tun:** Benutzermenü → „Abmelden“, dann „Studio-Login“ mit `foto-a` und zuerst dem alten Passwort `foto-a`,
  danach mit `Neu-Geheim-456`.
- **Was du siehst:** Altes Passwort: „E-Mail-Adresse/Benutzername oder Passwort ist falsch.“ Neues Passwort: Studio-
  Bereich, oben rechts „Felix Fotograf“.

### Schritt 8: Zurücksetzen (Dev-Daten wiederherstellen)
- **Was tun:** Benutzermenü → „Passwort ändern“ → zweimal `foto-a` → „Absenden“. Dann „Profil bearbeiten“ →
  Nachname `Foto` → „Absenden“.
- **Was du siehst:** „Ihr Passwort wurde geändert.“ bzw. „Ihr Profil wurde gespeichert.“, oben rechts wieder
  „Felix Foto“.

### Schritt 9: Plattform-Bereich
- **Was tun:** Abmelden, als `operator` / `operator` anmelden, links „Studios“, dann Benutzermenü
  („Paula Plattform“).
- **Was du siehst:** Dieselben Einträge „Profil bearbeiten“ und „Passwort ändern“ wie im Studio. Optional: Vorname
  ändern → zurück auf `/plattform/studios`, neuer Name oben rechts (danach wieder auf „Paula“ setzen).

## Automatisch abgesichert
- `KeycloakIntegrationTests`: `accountConsoleIsSwitchedOff` (Konsole und Account-REST-API liefern 404, auch mit
  einem Benutzer-Token über `admin-cli`), `defaultRolesGrantNoAccountManagement`,
  `usersMayChangeTheirNameButNotTheirEmailAddress` (User Profile: `email`/`username` nur Admin). Das Onboarding
  (`StudioOnboardingIntegrationTests`) legt Benutzer mit E-Mail und Namen weiter an.
- Vitest: `auth.service.spec.ts` (Start der Aktion mit `kc_action`, Rückkehr auf die Ausgangsseite, Ergebnis aus
  `kc_action_status`, nur app-interne Rücksprungziele), `studio-shell.spec.ts` / `platform-shell.spec.ts`
  (Menüeinträge, Bestätigung).
- Playwright `e2e/account.spec.ts`: Schritte 1–7 und 9 mit frisch angelegten Benutzern (die Dev-Benutzer bleiben
  unverändert; die Testbenutzer werden am Ende gelöscht).

## Noch offen / Einschränkungen
- E-Mail-Adresse ändern geht nur über einen Admin (Keycloak-Admin-Konsole); eine Funktion dafür in der App wäre ein
  eigenes Issue.
- Weitere Selbstbedienung (2FA, Sitzungen) gibt es bewusst nicht mehr über Keycloak (ADR 0013).

## Aufräumen
`docker compose down`, Backend und Frontend beenden.
