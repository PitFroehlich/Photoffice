# Studio-Profil und Rechtstexte

- **Date:** 2026-10-08
- **Machine / Agent:** fin-de-nb-0072 / Claude Code (zweite Session, eigener Worktree)
- **Issue / PR:** #20 / #40
- **Branch:** feature/20-studio-profil

## Goal
Issue #20 (Legacy F11): Studio pflegt Stammdaten und Rechtstexte (AGB, Widerruf, Impressum, Datenschutz) als
sicher gerenderten Text. Logo und Kundenanzeige sind ausgeklammert.

## Done
- API-Block `# --- studio profile (#20) ---` in `api/openapi.yaml`: `GET/PUT /studio/profile`,
  `GET /studio/profile/legal-texts`, `PUT /studio/profile/legal-texts/{kind}` (Tag `studio-profile`).
- Liquibase `202610081700-studio-profile.sql` (`studio_profile` eine Zeile pro Studio, `studio_legal_text` eine
  Zeile pro Studio und Art, beide RLS); Dev-Daten `202610081701-dev-studio-profile.sql` (A: Berlin, vollständig, AGB
  + Impressum; B: Wien, Kleinunternehmer, keine Texte).
- Modul `de.photoffice.studioprofile`: `StudioProfileData` (Formatregeln + Normalisierung, IBAN-Prüfsumme),
  `Country` (PLZ-Regel je Land), `LegalText` (Markdown-Regeln), `StudioProfileManagement` (öffentliches
  Lesemodell), Controller. `SecurityConfiguration`: GET alle Studio-Benutzer, Änderungen nur `STUDIO_ADMIN`.
- Frontend: `/studio/profil` (Formular, schreibgeschützt für Fotografen), `/studio/rechtstexte` (Reiter, Editor
  mit Live-Vorschau, Gliederungsvorschläge, Impressum aus dem Profil), Navigation. Neuer Baustein
  `MarkdownView` + `renderMarkdown` in `shared/ui` (`marked`, rohes HTML als Text, Bilder als Alt-Text, danach
  Angular-Sanitizer).
- ADR 0011 (ursprünglich 0009, umnummeriert wegen Kollision mit dem Keycloak-Theme), AGENTS.md (Abschnitt Studio-Profil, `MarkdownView`, Hinweis zu `DatePipe`), Demo
  `docs/demos/0020-studio-profil.md`, E2E `frontend/e2e/studio-profile.spec.ts` (nicht ausgeführt – Ports gehören
  der Hauptsession).
- Folge-Issue #39 „Studio-Logo hochladen“ (blockiert durch #9 und #20), Roadmap #21 ergänzt.

## Open / Next steps
- E2E-Spec gegen den laufenden Stack ausführen und die Demo mit dem Nutzer durchgehen.
- Öffentliche Auslieferung an Kunden (z. B. `GET /api/public/studios/{slug}/legal-texts` bzw. im Galerie-Kontext)
  mit #12/#14; dort nur `MarkdownView` verwenden.

## Pitfalls
- Angulars `DatePipe` in zwei Lazy-Seiten hat das Initial-Bundle von 491 kB auf 502 kB (Budget-Warnung) gehoben:
  esbuild legt den Datumscode in den gemeinsamen Initial-Chunk von `@angular/common`. Lösung:
  `Intl.DateTimeFormat` (`studio-profile/format-date-time.ts`), jetzt 493 kB.
- Im Worktree werden zusammengesetzte Shell-Befehle (cd + env + Umleitung, Heredocs, Variablen in `gh`-Aufrufen)
  abgelehnt – Maven mit absolutem Pfad aufrufen (`JAVA_HOME=… backend/mvnw -f backend/pom.xml …`), Dateien mit dem
  Write-Tool anlegen.
- Kein Test lief bisher mit Liquibase-Kontext `dev`; `StudioProfileDevDataTests` (`@ActiveProfiles("dev")`) prüft
  jetzt, dass die Dev-Daten laden und die Formatregeln erfüllen.
