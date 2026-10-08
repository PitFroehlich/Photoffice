# Feature-Analyse der Legacy-Anwendung

- **Date:** 2026-10-08
- **Machine / Agent:** fin-de-nb-0072 / Claude Code
- **Issue / PR:** – (Issues werden nach der Stack-Wahl gemeinsam angelegt)
- **Branch:** docs/legacy-feature-analysis

## Goal
Bestehende Features der Legacy-App erfassen, für eine Neuentwicklung priorisieren und begründen.

## Done
- `docs/analysis/legacy-features.md`: Feature-Katalog (F1–F16) mit Legacy-Zustand, Priorisierung für den Neubau, Anforderungen aus Mandantenfähigkeit/Erweiterbarkeit, Anti-Patterns, offene fachliche Fragen.
- `docs/decisions/0002-rewrite-multi-tenant.md`: Entscheidung des Nutzers – Neubau, Produkt für mehrere Studios, schrittweise Umsetzung, erweiterbar (z. B. Bezahldienst). Tech-Stack folgt in eigenem ADR.
- Offene fachliche Fragen mit dem Nutzer geklärt und in Abschnitt 6 der Analyse dokumentiert: Studios unabhängig, Zugang per Galerie-Link mit Code, Abzüge + Downloads, externer Druck-Service, SaaS mit Abo nach Speicherplatz, `nurpreise` entfällt, keine öffentlichen Galerien/Rechnungen in V1. Neue Features F17–F19, Priorisierung angepasst.
- Folgefragen geklärt (Fragen 8–12): optionales Kundenkonto (F20), Zahlungsempfänger und Druck-Anbieter noch offen → austauschbare Schnittstellen, Downloads pro Bild und als Paket, Studio-Abos automatisch über Zahlungsanbieter.

## Open / Next steps
- Noch offen: Wer kassiert (Studio/Plattform/Druck-Service)? Welcher Druck-Service-Anbieter?
- Tech-Stack wählen (eigenes ADR).
- Danach gemeinsam Issues für P0/P1 anlegen.

## Pitfalls
- Kundenbereich der Legacy-App ist nicht lauffähig: 7 Controller referenzieren gelöschte Behavior-Klassen (Commits b3060e9, 68849f8).
- Die Analyse ist statisch; die App wurde nicht im Docker-Container gestartet.
