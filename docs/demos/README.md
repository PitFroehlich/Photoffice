# Demos

Nach jedem abgeschlossenen Issue legt der bearbeitende Agent hier eine Demo an (Pflicht, siehe AGENTS.md
"Demo after every finished issue"). Sie zeigt, wie die Funktion aussieht und was umgesetzt wurde.

- Datei: `<issue-nr vierstellig>-<thema>.md`, z. B. `0007-kunden.md`
- Bilder: Ordner mit gleichem Namen, z. B. `0007-kunden/01-liste.png`
- Screenshots: `cd frontend && node scripts/screenshot.mjs <pfad> <ausgabe.png> [--login <benutzer>]`
  (laufende App vorausgesetzt: `docker compose up -d`, Backend mit Profil `dev`, `npm start`)

## Vorlage

```markdown
# Demo #<nr>: <Titel>

- **Issue / PR:** #<nr> / #<pr>
- **Datum:** YYYY-MM-DD
- **Agent / Maschine:** <agent> / <hostname>

## Was wurde umgesetzt
Kurz und fachlich: was kann man jetzt tun, was hat sich geändert.

## So probierst du es aus
Benutzer, URLs, Schritte (lokale Umgebung, siehe AGENTS.md).

## Screenshots
![Beschreibung](<ordner>/01-....png)

## API-Beispiele
Aufruf und echte Ausgabe (bei Backend-Funktionen).

## Tests
Was automatisiert abgesichert ist.

## Noch offen / Einschränkungen
```
