# 0009: Studio-Profil und Rechtstexte – Markdown, sicher gerendert

- **Status:** Accepted
- **Date:** 2026-10-08

## Context
Issue #20 (Legacy F11): Jedes Studio pflegt Stammdaten (Name, Adresse, Kontakt, Steuer- und Bankdaten) und
Rechtstexte (AGB, Widerrufsbelehrung, Impressum, Datenschutzerklärung), die Kunden später in der Galerie (#12) und
im Checkout (#14) sehen. Legacy speicherte die AGB als HTML aus TinyMCE in einer einzigen `firma`-Zeile ohne
Mandantentrennung und gab sie ungefiltert aus.

Rechtstexte schreibt das Studio, lesen tun sie Endkunden auf öffentlichen Seiten. Ein Rich-Text-Editor mit
gespeichertem HTML ist eine XSS-Quelle und ein schwerer Bundle-Brocken; die Texte brauchen aber nur Überschriften,
Absätze, Listen, Hervorhebungen, Links und ggf. Tabellen.

## Decision
- **Modul `de.photoffice.studioprofile`**, Tabellen `studio_profile` (eine Zeile pro Studio, Schlüssel `tenant_id`)
  und `studio_legal_text` (eine Zeile pro Studio und Art), beide mit RLS. Fehlt das Profil, liefert die API ein
  leeres Profil mit dem Studionamen; fehlt ein Text, ist er leer. Ein leer gespeicherter Text wird gelöscht.
- **Formatregeln wie bei Kunden** (identisch in `StudioProfileData.java` und `studio-profile-validators.ts`):
  PLZ abhängig vom Land (DE 5, AT/CH 4 Ziffern), IBAN mit ISO-7064-Prüfsumme (und fester Länge für DE/AT/CH/LI),
  BIC 8/11 Zeichen, USt-IdNr. DE/ATU/CHE, Steuernummer 8–13 Ziffern, Website mit optionalem `http(s)://`.
  Kontoinhaber und IBAN nur gemeinsam, BIC nur mit IBAN. Das Backend normalisiert (IBAN ohne Leerzeichen,
  USt-IdNr. kanonisch, `https://` ergänzt, E-Mail klein).
- **Rechtstexte sind Markdown** (GFM, einzelne Zeilenumbrüche bleiben – Adressen im Impressum), höchstens
  50.000 Zeichen, keine Steuerzeichen. Das Backend speichert den Markdown-Quelltext, nicht HTML.
- **Rendering nur im Client** mit `marked` und zwei Sicherungen: (1) rohes HTML im Markdown wird als Text
  ausgegeben (eigener `html`-Renderer), Bilder nur als Alt-Text (keine externen Requests von Kundenseiten);
  (2) das Ergebnis läuft durch Angulars `DomSanitizer` (`SecurityContext.HTML`), der z. B. `javascript:`-Links
  neutralisiert. Baustein: `MarkdownView` in `shared/ui` – auch für die Kundenansicht (#12/#14) zu verwenden.
- Editor: Textarea mit Live-Vorschau, Gliederungsvorschläge mit Platzhaltern (keine Rechtsberatung, die Seite
  weist darauf hin). Das Impressum kann aus dem Studio-Profil vorbefüllt werden.
- **Berechtigung:** Lesen alle Studio-Benutzer, Ändern nur `STUDIO_ADMIN` (wie Preisliste, `SecurityConfiguration`).
- **Lesemodell für andere Module:** `StudioProfileManagement.profile()` / `legalTexts()` / `legalText(kind)`
  (öffentliche API des Moduls).

## Consequences
- Kein HTML in der Datenbank, keine serverseitige HTML-Bereinigung nötig; ein anderer Client (z. B. E-Mail #19,
  Bestell-PDF) rendert den Markdown selbst und muss dieselben Regeln einhalten (kein rohes HTML).
- Studios ohne Markdown-Kenntnisse bekommen eine Kurzhilfe unter dem Editor und die Vorschau; ein WYSIWYG-Editor
  kann später ergänzt werden, solange er Markdown erzeugt.
- `marked` (~40 kB) liegt nur im Lazy-Chunk der Rechtstexte-Seite.
- **Offen:** öffentliche Auslieferung an Kunden (z. B. `GET /api/public/studios/{slug}/legal-texts`) kommt mit der
  Kundenansicht (#12) bzw. dem Checkout (#14); das Logo mit der Bildablage (#9, Folge-Issue).
