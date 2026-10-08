# 0007: Preismodell – Produkte, Download-Pakete, Versandarten

- **Status:** Accepted
- **Date:** 2026-10-08

## Context
Issue #13 (Legacy F7, neues Feature F17): Das Studio pflegt, was Kunden kaufen können. Legacy kannte nur
"Papier × Format" mit `decimal(10,0)`-Preisen (ohne Cent!). Neu sind Downloads (pro Bild und als Pakete,
Entscheidungen 3 und 11 in `docs/analysis/legacy-features.md`). Bezahldienst und Druck-Service kommen später
(ADR 0002/0003) und dürfen nicht verbaut werden. Bestellungen (#14) bauen auf dem Preismodell auf.

## Decision
- **Eine Preisliste pro Studio** (Mandant), Modul `de.photoffice.pricing`, Tabellen mit `tenant_id` + RLS.
- **Produkte** (`product`) mit Einzelpreis und **Produkttyp**:
  - `PRINT` (Abzug): Papiertyp × Format als Freitext; jede Kombination gibt es pro Studio einmal
    (Groß-/Kleinschreibung egal). Es gibt keine eigenen Stammdaten-Tabellen für Papier und Format: eine
    Kombination existiert genau dann, wenn sie einen Preis hat – Kunden sehen damit nur gültige Kombinationen.
  - `DOWNLOAD`: eine Auflösungsvariante (`WEB`, `FULL`), je Auflösung ein Preis.
  - Typabhängige Spalten sind nullable und per CHECK-Constraint an den Typ gebunden. Ein neuer Typ
    (z. B. Fotobuch) ergänzt Spalten, einen Enum-Wert und einen CHECK-Zweig. Der Typ ist nach dem Anlegen fest.
- **Download-Pakete** (`download_package`): Name, Art (`IMAGE_COUNT` mit Bildanzahl ≥ 2 oder `WHOLE_GALLERY`),
  Auflösung, Paketpreis. Ein Paket bezieht sich immer auf die Bilder *einer* Galerie.
- **Versandarten** (`shipping_method`): Name und Kosten; nur für Bestellungen mit Abzügen relevant.
- **Aktiv/inaktiv** für alle Einträge: inaktive Einträge bleiben gepflegt, werden Kunden aber nicht angeboten.
  `PriceListManagement.offer()` liefert nur aktive Einträge (Grundlage für Shop/Bestellung #14).
- **Geld:** Bruttopreise (Endkunden, inkl. USt.) als **ganze Cent** (`INTEGER`, 0 … 10.000.000), in API und Java
  als Integer. Kein Gleitkomma; Jackson lehnt Dezimalzahlen für Integer-Felder ab
  (`spring.jackson.deserialization.accept-float-as-int: false`), damit `4.9` nicht still zu `4` Cent wird.
  Das Frontend rechnet Eingaben wie „12,90" per Zeichenkette in Cent um.
- **Währung und Steuersatz** pro Studio (`price_list_settings`, höchstens eine Zeile; ohne Zeile gelten EUR und
  19 %). Währung vorerst nur `EUR` (CHECK-Constraint), Steuersatz `NUMERIC(4,2)` (z. B. 0 für Kleinunternehmer).
- **Berechtigung:** Alle Studio-Benutzer dürfen die Preisliste lesen, nur `STUDIO_ADMIN` darf sie ändern
  (URL-Regel in `SecurityConfiguration`: `GET /api/studio/price-list/**` für Studio-Mitglieder, alle anderen
  Methoden zusätzlich `ROLE_STUDIO_ADMIN`).
- **API:** `GET /api/studio/price-list` liefert die ganze Liste ohne Paging (wenige Dutzend Einträge);
  CRUD je Eintragsart unter `/products`, `/download-packages`, `/shipping-methods`, `PUT /settings`.

## Consequences
- Einfache Pflege in einer Maske; Erweiterung um Produkttypen ohne neue Tabellen-Hierarchie.
- Ein Steuersatz pro Studio reicht für Abzüge und Downloads in DE (beide 19 %). Brauchen Produkte später
  unterschiedliche Sätze, bekommt `product` eine eigene Spalte (Default aus den Einstellungen).
- Bestellungen (#14) müssen Preis, Steuersatz und Produktbezeichnung **als Kopie** in der Bestellposition speichern,
  damit spätere Preisänderungen oder gelöschte Produkte alte Bestellungen nicht verändern. Ist das umgesetzt,
  muss das Löschen referenzierter Einträge neu bewertet werden (Deaktivieren statt Löschen).
- Papier/Format als Freitext erlaubt Tippvarianten („13x18" vs. „13 × 18 cm"); falls nötig, später als
  Auswahllisten nachrüsten.
- Galerie-spezifische Preise (Preisliste pro Galerie überschreiben) sind bewusst nicht umgesetzt – Galerien gibt
  es noch nicht (#8). Möglicher Weg: optionale Tabelle `gallery_price_override` mit Verweis auf Produkt/Paket.
