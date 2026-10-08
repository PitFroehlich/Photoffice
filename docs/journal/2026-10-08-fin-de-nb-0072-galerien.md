# Galerieverwaltung

- **Date:** 2026-10-08
- **Machine / Agent:** fin-de-nb-0072 / Claude Code
- **Issue / PR:** #8 / (siehe PR zu #8)
- **Branch:** feature/8-galerien

## Goal
Galerien mit Kunden-Zuordnung, Status und Ablaufdatum (Legacy F1).

## Done
- Liquibase: `gallery`, `gallery_customer` (RLS, mandantensichere Fremdschlüssel; `customer` bekommt `UNIQUE (tenant_id, id)`), Dev-Galerien in allen Status.
- API `/api/studio/galleries` (Liste mit Suche/Status/Kunde, CRUD, `publish`, `unpublish`).
- Modul `gallery`: Veröffentlichen nur nicht abgelaufen und mit aktiven Preisen (`PriceListManagement.hasActiveOffer()`), Events `GalleryPublished`/`GalleryDeleted`, `CustomerManagement.findAllById` für Zuordnung und Anzeige.
- Frontend: Galerieliste (Statusfilter, Kundenfilter `?kunde=`), Formular mit Kunden-Chips/Autocomplete, Datepicker, Sichtbarkeits-Karte; Link „Galerien“ im Kundenformular.
- Gemeinsam: `GermanDateAdapter` (TT.MM.JJJJ, Studio-Defaults), `toApiDate`/`fromApiDate`/`formatApiDate`, Datepicker-Fehlermeldungen; Listen navigieren nur noch bei geänderter URL.
- Tests: Backend 185, Frontend 81 Unit, E2E 29.

## Open / Next steps
- #9 Upload, #11 Galerie-Link sind frei.

## Pitfalls
- `MatChipsModule` bringt einen eigenen `ErrorStateMatcher` mit → Fehler erschienen erst nach Verlassen des Feldes. Komponente setzt `ShowOnDirtyErrorStateMatcher` selbst (AGENTS.md).
- Datepicker: ungültige Eingabe in leerem Feld ändert den Wert nicht → Feld beim Verlassen als dirty markieren.
- `getByLabel('Name der Galerie')` traf auch „Name der Galerie suchen“ – `exact: true` verwenden.
- E2E-Tests dürfen Seed-Daten nicht irreversibel ändern (abgelaufene Galerie offline genommen → nicht wieder online zu bekommen); solche Regeln im Backend-Test prüfen.
- Changeset-Zeitstempel bestimmen die Reihenfolge: Galerie (FK auf `customer`) muss nach `202610081500-customer.sql` sortieren.
