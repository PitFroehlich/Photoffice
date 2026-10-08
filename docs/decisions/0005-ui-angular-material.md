# 0005: UI mit Angular Material und eigenem Photoffice-Design

- **Status:** Accepted
- **Date:** 2026-10-08

## Context
Ab #7 entstehen viele Masken (Listen, Formulare, Dialoge, Upload) – bearbeitet von mehreren Agenten parallel.
Ohne gemeinsame UI-Bibliothek und gemeinsame Bausteine würde jede Maske anders aussehen und sich anders bedienen.
Optionen: Angular Material, PrimeNG, eigenes CSS ohne Bibliothek.

## Decision
- **Angular Material** (inkl. CDK) ist die UI-Bibliothek des Frontends (Entscheidung des Nutzers).
- Darüber liegt ein **eigenes Photoffice-Design** (Farben, Typografie, Dichte als Design-Tokens/Theme), festgelegt in #27.
- Wiederkehrende Muster (Seitenkopf, Listen mit Suche/Paging, Formulare, Bestätigungsdialog, Meldungen, Lade-/Leerzustände) werden in #27 einmal gebaut und von allen Oberflächen-Issues verwendet.
- Keine zweite Komponentenbibliothek daneben.

## Consequences
- Konsistente, barrierearme Bedienung; Updates im Gleichschritt mit Angular.
- Oberflächen-Issues (#7, #13, #20, …) hängen von #27 ab.
- Das Kundenerlebnis der Galerie (#12) kann eigenständiger gestaltet werden, nutzt aber dieselben Tokens.
