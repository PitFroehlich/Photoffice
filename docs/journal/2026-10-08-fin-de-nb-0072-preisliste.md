# Preisliste und Produkte

- **Date:** 2026-10-08
- **Machine / Agent:** fin-de-nb-0072 / Claude Code (zweite Session, parallel zur Hauptsession)
- **Issue / PR:** #13 / #34
- **Branch:** feature/13-preisliste (basiert auf `feature/7-kundenverwaltung`, da PR #32 noch nicht gemergt war)

## Goal
Studio pflegt seine Preisliste (Legacy F7 + Downloads F17): Abzüge, Downloads, Download-Pakete, Versandarten,
Währung/Steuersatz; nur gültige Kombinationen für Kunden.

## Done
- ADR 0007 Preismodell (Produkttypen PRINT/DOWNLOAD mit CHECK-Constraints, Pakete, Versandarten, Bruttopreise in Cent,
  EUR + Steuersatz pro Studio, aktiv/inaktiv, Rechte).
- Liquibase `202610081600-price-list.sql`: `price_list_settings`, `product`, `download_package`, `shipping_method`
  (alle mit RLS); Dev-Daten für Studio A (19 %) und B (0 %).
- API-Block `# --- price list (#13) ---` unter `/studio/price-list` (Gesamtliste ohne Paging, CRUD je Eintragsart,
  `PUT /settings`).
- Backend-Modul `pricing` (`PriceListManagement` inkl. `offer()` nur mit aktiven Einträgen für #14).
- `SecurityConfiguration`: `GET /api/studio/price-list/**` für alle Studio-Mitglieder, Änderungen nur `STUDIO_ADMIN`.
- `application.yml`: `spring.jackson.deserialization.accept-float-as-int: false` (sonst wurde `4.9` still zu 4 Cent).
- Frontend `src/app/price-list/`: Übersicht mit fünf Abschnitten, Formulare für Produkt, Paket, Versandart, Steuersatz;
  `money.ts` (Cent-Umrechnung per String), `canEditPriceList()`; Navigationseintrag "Preisliste";
  `validationMessage` kennt den Fehler `price`; Test-Helfer `studioSessionAs(role)`.
- Bundle: `studio-session.ts` und `start-page.ts` importieren API-Funktionen direkt statt über den Barrel
  `api/functions` (Regel in AGENTS.md) – Initial-Bundle von 501 kB (Warnung) auf 491 kB.
- Tests: Backend 65 (16 neu), Frontend 60 Unit (25 neu), E2E-Spec `e2e/price-list.spec.ts` (8 Tests, **nicht lokal
  ausgeführt** – Ports gehören der Hauptsession).

## Open / Next steps
- E2E-Spec gegen den laufenden Dev-Stack ausführen und die geführte Demo `docs/demos/0013-preisliste.md` machen.
- Nach dem Merge von #32: Branch auf `main` rebasen (Konflikte möglich in `api/openapi.yaml`, `app.routes.ts`,
  `studio-navigation.ts`, `AGENTS.md`).
- #14 Bestellung: Preis/Steuersatz/Bezeichnung als Kopie in Bestellpositionen; danach Löschen referenzierter Einträge
  neu bewerten. Kunden-Ansicht der Preisliste kommt mit Galerie-Zugang/Shop.
- Preise pro Galerie überschreiben (ADR 0007), Zahlungsarten (mit dem Bezahldienst).

## Pitfalls
- Jackson akzeptiert standardmäßig Dezimalzahlen für Integer-Felder und schneidet ab – bei Geldbeträgen gefährlich.
- Generierte ng-openapi-gen-Funktionen haben Seiteneffekte (`fn.PATH = ...`) und werden nicht tree-geshakt; ein
  Barrel-Import im Initial-Bundle zieht alle Endpunkte in `main`.
- Template-Typprüfung: `let row` in `mat-table` ist `any` – `Record<Enum, string>[row.x]` schlägt fehl; Methode nutzen.
- Worktree-Agent: Heredocs/Compound-Kommandos werden blockiert, Dateien mit dem Write-Tool anlegen.
