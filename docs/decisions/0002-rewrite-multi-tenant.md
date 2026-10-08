# 0002: Neuentwicklung als mandantenfähiges Produkt

- **Status:** Accepted
- **Date:** 2026-10-08

## Context
Die Legacy-App (PHP 5.6, PEAR, Flash-Upload) ist in wesentlichen Teilen nicht lauffähig (Kundenbereich, Bestellprozess),
hat gravierende Sicherheitsmängel (SQL-Injection, fehlende Autorisierung, MD5-Passwörter) und ist auf ein Studio pro Installation ausgelegt.
Ziel ist ein Produkt für mehrere Fotostudios, das später erweitert werden kann (z. B. um einen Bezahldienst).
Details: [docs/analysis/legacy-features.md](../analysis/legacy-features.md).

## Decision
- Die Anwendung wird **neu geschrieben**. Die Legacy-App dient nur als fachliche Vorlage, sie wird nicht repariert oder migriert.
- Das neue Produkt ist **mandantenfähig** (mehrere Studios).
- Die bestehenden Features werden **schrittweise** in der Reihenfolge der Priorisierung aus der Feature-Analyse umgesetzt.
- Die Architektur muss Erweiterungen ohne Umbau des Kerns ermöglichen (z. B. Bezahldienst, Benachrichtigungen, Bildverarbeitung, Speicher).
- Der Tech-Stack wird in einem eigenen, späteren ADR festgelegt.

## Consequences
- Der Legacy-Code bleibt im Repository als Referenz, bis der Neubau ihn ersetzt.
- Mandantentrennung, Authentifizierung und Rollen sind Voraussetzung für alle weiteren Features (Priorität P0).
- Bis zur Stack-Entscheidung werden keine Implementierungs-Issues angelegt, die von der Technologie abhängen.
