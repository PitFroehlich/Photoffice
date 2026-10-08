# Feature-Analyse der Legacy-Anwendung

- **Stand:** 2026-10-08 (Commit `5591028` auf `main`)
- **Methode:** Statische Code-Analyse (Controller, Show-/Action-Behaviors, Models, `photoffice.sql`). Die App wurde dabei **nicht** gestartet; Aussagen zum Zustand ("funktioniert", "kaputt") beruhen auf dem Code.
- **Zweck:** Fachliche Vorlage für die Neuentwicklung (siehe [ADR 0002](../decisions/0002-rewrite-multi-tenant.md)).
  Die Legacy-App wird nicht repariert. Relevant ist, *was* sie fachlich leistet bzw. leisten sollte – und welche Fehler wir nicht wiederholen.

---

## 1. Überblick

Photoffice ist ein Galerie- und Bestellsystem für Fotostudios:
Der **Fotograf** lädt Bilder in Galerien, ordnet sie Kunden zu und pflegt eine Preisliste für Abzüge.
Der **Kunde** loggt sich ein, sieht seine Galerien, wählt Bilder mit Papiertyp, Format und Anzahl aus und schickt eine Bestellung ab.
Der Fotograf bearbeitet die Bestellung (Übersicht, PDF, Abschluss-Mail).

Die Legacy-App ist **ein Studio pro Installation** (Tabelle `firma` mit genau einer Zeile).
Das neue Produkt soll **mehrere Studios** bedienen.

### Rollen

| Rolle | Legacy | Neubau (Anforderung) |
|---|---|---|
| Studio-Mitarbeiter / Fotograf | Rolle `photographer`, alle Fotografen gleichberechtigt | Rollen pro Studio (z. B. Inhaber/Admin, Fotograf) |
| Kunde | Rolle `customer`, Login nur per Passwort | Login pro Studio, eindeutige Identifikation |
| Plattform-Betreiber | – | Neu: verwaltet Studios (Mandanten) |
| Öffentlicher Besucher | – (öffentliche Galerien nur angedeutet) | Optional, siehe F13 |

---

## 2. Feature-Katalog

Legende Zustand: ✅ funktioniert · ⚠️ funktioniert mit Mängeln · ❌ kaputt · ⬜ nur angelegt / Fragment

### F1 – Galerieverwaltung
- **Fachlich:** Fotograf legt Galerien an (Kunden-Galerie oder "öffentliche" Galerie), benennt sie um, schaltet sie online/offline, setzt ein Verfallsdatum. Eine Galerie kann nur online gehen, wenn sie nicht abgelaufen ist und Preise existieren. Beim Online-Schalten kann optional eine Mail an den Kunden gehen (F8).
- **Daten:** `gallerien` (`online`, `verfallsdatum`, `bildanzahl`, `nurpreise`), `kunden_has_gallerien` (n:m Kunde–Galerie).
- **Legacy:** ✅ – Jahresauswahl fest auf 2010–2015 (`view/showbehaviors/galerieaendern_show_behavior.php:73`). Feld `nurpreise` ohne erkennbare Verwendung.
- **Code:** `controller/allegalerien.php`, `neuegalerie.php`, `galerieaendern.php`, `einzelgalerie.php`, `model/classes/insertgalerie.php`.

### F2 – Bild-Upload und Bildverarbeitung
- **Fachlich:** Mehrfach-Upload in eine Galerie. Pro Bild: EXIF auslesen, verkleinerte Ansicht (600 px) und Thumbnail (120 px) erzeugen, optional Wasserzeichen einbrennen. Bei Namenskollision Suffix anhängen.
- **Daten:** `bild` (Datei-/Icon-Name, `position`, `online`, EXIF-Felder: Blende, Belichtung, Brennweite, ISO, Blitz, Kamera, Datum). Dateien unter `view/images/galeriebilder/<galerieId>/`.
- **Legacy:** ⚠️ Upload per **Flash (Uploadify)** – in heutigen Browsern nicht mehr nutzbar. Nur JPEG. **Original wird verworfen** (nur 600-px-Version gespeichert). Kein Login-Check im Upload-Endpunkt. Fotograf wird immer mit ID 1 gespeichert.
- **Code:** `controller/actionbehaviors/dateiupload_action_behavior.php`, `model/classes/images.php`, `view/templates/bilduploadbox.tpl`.

### F3 – Kunden-Login und Galerieansicht (Kundensicht)
- **Fachlich:** Kunde meldet sich an, sieht Liste seiner Online-Galerien, öffnet eine Galerie, sieht Bilder (Thumbnails, Lightbox), markiert Bilder für die Bestellung. Zeigt pro Bild die bereits bestellte Anzahl.
- **Legacy:** ❌
  - Die Controller `kundenindex`, `kundenlogout`, `allekundengalerien`, `kundeeinzelgalerie`, `warenkorb`, `preisliste` und `onlineshop` referenzieren Klassen (`kundestandard_action_behavior`, `onlineshopstandard_action_behavior`), die in Commit b3060e9 bzw. 68849f8 gelöscht wurden → Fatal Error.
  - Login nur per **Passwort ohne Benutzername**; der erste Kunde mit passendem MD5-Hash wird eingeloggt (`model/classes/kundenlogincheck.php`).
  - Keine Prüfung, ob die angefragte Galerie dem Kunden gehört.
- **Code:** `controller/kundenlogin.php`, `view/customer/*`, `view/js/kundensicht.js`.

### F4 – Bestellprozess (Warenkorb, Checkout)
- **Fachlich:** Kunde wählt Bild(er) + Papiertyp → verfügbare Formate (nur solche mit Preis) → Anzahl → in den Warenkorb. Warenkorb zeigt Positionen und Summe, Anmerkungsfeld, AGB-Bestätigung, Absenden oder Leeren. Nach Absenden Mail an das Studio.
- **Daten:** `bestellung` (Kunde, Zahlungsart, Versandkosten, `bestellwert`, Status-Flags `kundeabgeschlossen`/`fotografabgeschlossen`, Anmerkung), `bestellung_has_bild` (Bild, Papier, Format, Anzahl).
- **Legacy:** ❌ Halbfertig.
  - Positionen landen nur in der Session (`$_SESSION['aktuelleBestellung']`), werden nie in die DB geschrieben.
  - Der Warenkorb liest dagegen aus der DB → zeigt immer 0,00.
  - Absenden scheitert mit SQL-Fehler (leere ID) und meldet "Warenkorb ist leer".
  - Zahlungsart und Versandkosten werden im Checkout **nicht** abgefragt, obwohl die Daten existieren.
  - AGB-Bestätigung nur clientseitig.
  - `bestellwert` ist `decimal(10,0)` → Cent-Beträge gehen verloren.
- **Code:** `controller/actionbehaviors/bestellungeintragen_action_behavior.php`, `warenkorbeintragen_action_behavior.php`, `model/classes/insertbestellung.php` (ungenutzt), `updatebestellung.php`.

### F5 – Kundenverwaltung
- **Fachlich:** Liste mit Suche und Paging, Kunde anlegen/bearbeiten/löschen, Detailansicht (Adresse, E-Mail, Login).
- **Daten:** `kunden`.
- **Legacy:** ✅ (Löschen ohne Authentifizierung, siehe Abschnitt 5).
- **Code:** `controller/kunden.php`, `kundenliste.php`, `neukunde.php`, `kundeaendern.php`, `kundendaten.php`.

### F6 – Bestellverwaltung für das Studio
- **Fachlich:** Liste aller Bestellungen (Kunde, Summe, Status), Detailansicht der Positionen, Bestellung löschen, Bestellung abschließen mit frei formulierter Mail an den Kunden (setzt `fotografabgeschlossen`).
- **Legacy:** ✅ – erhält aber wegen F4 keine echten Daten. TODO im Code: "nur abgeschlossene Bestellungen anzeigen".
- **Code:** `controller/bestellungsliste.php`, `bestellungsdaten.php`, `bestellungabschliessen.php`, `view/js/bestellung.js`.

### F7 – Preisliste und Shop-Stammdaten
- **Fachlich:** Studio pflegt Papiertypen, Bildformate, Preis je Kombination (Papier × Format), Versandarten mit Kosten, Zahlungsarten (aktiv/inaktiv). Kunde kann die Preisliste einsehen.
- **Daten:** `papier`, `bildformate`, `preis` (PK Papier+Format), `versandkosten`, `zahlungsart`.
- **Legacy:** ✅ Pflege durch das Studio (`controller/fotodaten.php`). ❌ Kunden-Preisliste (fehlende Klasse, s. F3).

### F8 – E-Mail-Benachrichtigungen
- **Fachlich:**
  1. Galerie online → Zugangsdaten an den Kunden (`galerieaendern_show_behavior.php`).
  2. Bestellung abgesendet → Mail an das Studio (`warenkorbeintragen_action_behavior.php`).
  3. Bestellung abgeschlossen → Mail an den Kunden (`bestellungabschliessen`).
- **Legacy:** ⚠️ Klartext-Passwort per Mail; Passwort wird beim Online-Schalten neu generiert und überschreibt das alte. Absender der Studio-Mail ist die Kundenadresse (Header-Injection möglich). Link-Erzeugung über `HTTP_HOST`. Versand über PHP `mail()`.

### F9 – Bestell-PDF
- **Fachlich:** PDF einer Bestellung mit Logo, Titel, Tabelle (Format, Preis, Anzahl, Bild), Seitenzahlen – Arbeitsliste für das Studio.
- **Legacy:** ⚠️ funktioniert; ohne Summe, Versand, Zahlungsart, Adressen. Kein Rechnungsdokument.
- **Code:** `model/classes/pdf_bestellung.php` (FPDF), `view/showbehaviors/generierebestellungspdf_show_behavior.php`.

### F10 – Benutzerverwaltung des Studios (Fotografen)
- **Fachlich:** Fotografen-Logins anlegen, bearbeiten, löschen (letzter Login kann nicht gelöscht werden).
- **Daten:** `fotograf` (Bezug zu `firma`).
- **Legacy:** ✅ – nur eine Rolle, keine Rechteabstufung.

### F11 – Firmenstammdaten und AGB
- **Fachlich:** Studio pflegt Adresse, Kontakt, Bankverbindung, Steuernummer, Logo; AGB als Rich-Text (TinyMCE). Kunde sieht die AGB.
- **Daten:** `firma` (eine Zeile, inkl. `agb`).
- **Legacy:** ✅

### F12 – EXIF-Anzeige und Wasserzeichen
- **Fachlich:** Detailpanel mit Aufnahmedaten pro Bild; Wasserzeichen (PNG, mittig) als Schutz der Vorschaubilder.
- **Legacy:** ✅ / ⚠️ – Transparenz-Einstellung wird ignoriert (fest 20).

### F13 – Öffentliche Galerien
- **Fachlich (gedacht):** Galerien ohne Kundenzuordnung, für alle sichtbar (Portfolio/Marketing).
- **Legacy:** ⬜ Anlegen möglich, aber es gibt keine öffentliche Ansicht.

### F14 – Onlineshop
- **Legacy:** ⬜ Leerer Template-Rahmen ohne Funktion. Fachliche Absicht unklar.

### F15 – Rechnungen
- **Legacy:** ⬜ Nur Tabelle `rechnung` (Rechnungsnummer je Kunde), kein Code.

### F16 – Lizenz-/Update-Prüfung
- **Legacy:** ⬜ XML-RPC-Aufruf an photoffice.de, komplett auskommentiert (`model/classes/version.php`). Für den Neubau irrelevant (Lizenzierung wäre im SaaS-Modell Teil der Mandantenverwaltung).

### Hilfsfunktionen (keine eigenständigen Features)
Breadcrumb-Navigation (Tabelle `navigation`), Dashboard/Startseite des Studios, AJAX-Endpunkte für Session-State, Logout.

---

## 3. Priorisierung für den Neubau

Kriterium: Wertschöpfungskette eines Studios – **Bilder ausliefern → Abzüge verkaufen → Bestellungen abwickeln** – plus die Voraussetzungen, die ein Produkt für mehrere Studios braucht.

| Prio | Feature | Begründung |
|---|---|---|
| **P0** | **Mandanten, Authentifizierung, Rollen** (neu; ersetzt Logins aus F3/F10) | Grundlage für alles andere. Mandantenfähigkeit nachträglich einzubauen ist teuer; die Legacy-App zeigt, wie fehlende Autorisierung jede Funktion angreifbar macht. |
| **P1** | F1 Galerieverwaltung | Ohne Galerien kein Produkt. |
| **P1** | F2 Bild-Upload und Bildverarbeitung | Bilder sind der Inhalt; Upload ist der Einstieg in jeden Arbeitsablauf. Originale müssen erhalten bleiben (spätere Downloads, Abzüge in hoher Auflösung). |
| **P1** | F5 Kundenverwaltung | Galerien und Bestellungen hängen an Kunden. |
| **P1** | F3 Kundensicht auf Galerien | Die zentrale Leistung für den Endkunden. |
| **P1** | F7 Preisliste | Voraussetzung für Bestellungen (und in der Legacy-Logik für das Online-Schalten). |
| **P1** | F4 Bestellprozess | Umsatzquelle des Studios; muss von Anfang an so gebaut sein, dass ein Bezahldienst später andocken kann. |
| **P1** | F6 Bestellverwaltung | Ohne sie sind eingegangene Bestellungen für das Studio unsichtbar. |
| **P2** | F8 E-Mail-Benachrichtigungen | Wichtig für den Ablauf, aber anfangs manuell überbrückbar. Sinnvoll als ereignisbasierter Mechanismus (Erweiterbarkeit). |
| **P2** | F11 Studio-Stammdaten und AGB | Rechtlich nötig für den Verkauf, fachlich einfach. Wird im Mandantenmodell zum "Studio-Profil". |
| **P2** | F9 Bestell-PDF | Arbeitserleichterung; kann im ersten Schritt durch die Detailansicht ersetzt werden. |
| **P3** | F12 Wasserzeichen | Schutz vor Bilderklau – für manche Studios wichtig, aber kein Blocker. |
| **P3** | F12 EXIF-Anzeige | Nettes Extra; EXIF sollte aber schon beim Upload (F2) gespeichert werden. |
| **P4** | F13 Öffentliche Galerien | Marketing-Funktion, in der Legacy-App nie fertig. |
| **P4** | F15 Rechnungen | Nie umgesetzt; wird mit dem Bezahldienst relevant. |
| – | F14 Onlineshop, F16 Lizenzprüfung | Nicht übernehmen (kein fachlicher Inhalt bzw. obsolet). |

**Vorschlag für die Reihenfolge der Umsetzung (vertikale Schnitte):**
1. P0 Mandant + Studio-Login
2. F5 Kunden → F1 Galerien → F2 Upload (Studio kann Bilder bereitstellen)
3. F3 Kundensicht (Kunde kann Bilder sehen) → erster nutzbarer Stand
4. F7 Preisliste → F4 Bestellung → F6 Bestellverwaltung (Studio kann verkaufen)
5. P2 und folgende

---

## 4. Anforderungen aus "mehrere Studios" und "erweiterbar"

Diese Punkte ergeben sich nicht aus dem Legacy-Code, sondern aus dem Ziel des Neubaus. Sie sollten bei der Technologie-Wahl berücksichtigt werden.

- **Mandantentrennung:** Jede fachliche Entität (Kunde, Galerie, Bild, Preis, Bestellung, …) gehört genau einem Studio. Zugriffe müssen immer auf den Mandanten eingeschränkt sein – Legacy hat für `firma` nur eine Zeile und keine Trennung.
- **Kunden-Identität:** Ein Endkunde gehört zu einem Studio (Legacy) – oder kann ein Kunde bei mehreren Studios sein? → offene Frage.
- **Studio-spezifische Konfiguration:** Preislisten, Zahlungs-/Versandarten, AGB, Logo, Wasserzeichen, Mail-Absender, ggf. eigene Domain/Subdomain.
- **Erweiterungspunkte:**
  - *Bezahldienst:* Bestellung als Zustandsmaschine (z. B. Warenkorb → bestellt → bezahlt → in Produktion → versendet/abgeschlossen) statt zweier Boolean-Flags; Zahlungsart als austauschbare Schnittstelle.
  - *Benachrichtigungen:* Domain-Events (z. B. `GalerieVeröffentlicht`, `BestellungEingegangen`) statt Mailversand direkt in der View-Logik.
  - *Bildverarbeitung:* austauschbare Pipeline (Formate, Größen, Wasserzeichen) und austauschbarer Speicher (lokal / Object Storage).
- **Geldbeträge:** Als Ganzzahl in Cent oder als exakter Dezimaltyp mit 2 Nachkommastellen, Währung und Steuersatz mitdenken.
- **Bild-Speicher:** Originale behalten; Ableitungen (Vorschau, Thumbnail, Wasserzeichen-Version) erzeugen und cachen; Pfade pro Mandant trennen; Zugriff auf Bilder nur für Berechtigte (Legacy: Dateien liegen frei im Webroot).

---

## 5. Was wir aus der Legacy-App *nicht* übernehmen

| Problem in Legacy | Beispiel | Konsequenz für den Neubau |
|---|---|---|
| SQL per String-Verkettung | `model/classes/deletekunde.php:17`, `database.php:93` | Nur parametrisierte Queries / ORM. |
| Aktionen ohne Autorisierung | `controller/actionbehaviors/ajaxdelete_action_behavior.php` löscht vor dem Login-Check; Upload ohne Check | Autorisierung zentral, standardmäßig verweigern. |
| Unsichere Passwörter | ungesalzenes MD5, Seed-Admin `admin`/`password`, Klartext-Passwort per Mail | Moderner Passwort-Hash; Einladungs-/Reset-Links statt Passwörtern per Mail. |
| Kunden-Login ohne Benutzername | `model/classes/kundenlogincheck.php` | Eindeutige Identifikation (E-Mail) pro Studio. |
| Kein Output-Escaping, kein CSRF-Schutz | durchgängig | Framework mit automatischem Escaping und CSRF-Schutz. |
| Fachlogik in View-Klassen | DB-Writes in `view/showbehaviors/*` | Klare Schichten: Domain / Anwendung / UI. |
| Warenkorb nur in der Session | `bestellungeintragen_action_behavior.php` | Warenkorb persistent (DB). |
| Keine Foreign Keys, `decimal(10,0)` für Beträge | `photoffice.sql` | Referentielle Integrität, korrekte Geldtypen. |
| Ganze Tabellen in PHP laden und filtern | Logins, Listen (`_ausgeben()`) | Filtern/Paginieren in der Datenbank. |
| Flash-Upload, veraltete Bibliotheken | Uploadify, PEAR DB/QuickForm/ITX, FPDF | Aktueller, gepflegter Stack. |
| Kaum Tests | nur `test/test_breadcrumb.php` | Tests ab dem ersten Feature, CI. |

---

## 6. Offene fachliche Fragen

1. Kann ein Endkunde bei mehreren Studios Kunde sein (ein Login) – oder ist er pro Studio getrennt?
2. Wie sollen Kunden Zugang zu Galerien bekommen: eigener Account, Galerie-Link mit Code, beides?
3. Was soll ein Kunde bestellen können: nur Abzüge (Legacy), auch digitale Downloads?
4. Wer produziert die Abzüge – das Studio selbst oder ein Labor (spätere Labor-Anbindung)?
5. Welche Bedeutung hat das Galerie-Feld `nurpreise`? (Im Legacy-Code nicht ausgewertet.)
6. Wie wird das Produkt abgerechnet (Abo pro Studio, Speicherplatz, Provision)? Beeinflusst Mandanten- und Bezahlmodell.
7. Sind öffentliche Galerien (F13) und Rechnungen (F15) für die erste Version gewünscht?
