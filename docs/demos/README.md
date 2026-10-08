# Demos

Nach jedem abgeschlossenen Issue führt der bearbeitende Agent den Nutzer **Schritt für Schritt durch die laufende
Anwendung** (Pflicht, siehe AGENTS.md "Demo after every finished issue"). Keine Screenshots – der Nutzer probiert
selbst aus. Die Anleitung dafür liegt hier.

- Datei: `<issue-nr vierstellig>-<thema>.md`, z. B. `0007-kunden.md`
- Jeder Schritt: **Was tun** (wohin klicken, was eingeben) und **Was du siehst** (erwartetes Ergebnis).
- Die Schritte vorher selbst prüfen, am besten als Playwright-Test in `frontend/e2e/`.

## Vorlage

```markdown
# Demo #<nr>: <Titel>

- **Issue / PR:** #<nr> / #<pr>
- **Datum:** YYYY-MM-DD
- **Agent / Maschine:** <agent> / <hostname>

## Was wurde umgesetzt
Kurz und fachlich: was kann man jetzt tun, was hat sich geändert.

## Vorbereitung
1. `docker compose up -d`
2. `cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`
3. `cd frontend && npm start`
Testbenutzer: siehe AGENTS.md "Dev users" (Passwort = Benutzername).

## Schritt für Schritt
### Schritt 1: <kurzer Titel>
- **Was tun:** …
- **Was du siehst:** …

### Schritt 2: …

## Automatisch abgesichert
Welche Tests die Schritte abdecken.

## Noch offen / Einschränkungen

## Aufräumen
`docker compose down`, Backend und Frontend beenden.
```
