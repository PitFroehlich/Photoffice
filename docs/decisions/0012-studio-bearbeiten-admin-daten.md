# 0012: Studio bearbeiten – Admin-Daten am Studio statt im Event

- **Status:** Accepted
- **Date:** 2026-10-09
- **Ändert:** [0007](0007-studio-onboarding-keycloak.md) (Event mit Admin-Daten), [0010](0010-plattform-bereich-onboarding-fehler.md)
  („Erneut versuchen“ über die gespeicherte Zustellung)

## Context
Bisher standen E-Mail und Name des ersten Studio-Admins nur im Event `TenantRegistered` (ADR 0007). Scheiterte das
Onboarding an diesen Daten – z. B. weil die E-Mail schon einem Benutzer eines anderen Studios gehört –, wiederholten
„Erneut versuchen“ und `EventResubmission` das unveränderte Event und liefen immer wieder in denselben Fehler
(ADR 0010, Demo #35 Schritt 10). Korrigieren ließ sich weder das noch der Studioname (Issue #42).

## Decision
- **Admin-Daten am Studio:** Spalten `tenant.admin_email`, `admin_first_name`, `admin_last_name`. Sie werden beim
  Registrieren gesetzt, bis zum Abschluss des Onboardings gehalten und von `markOnboarded` gelöscht
  (Datensparsamkeit: danach verwaltet der Admin sein Konto in Keycloak). Die API zeigt sie in `TenantResponse`
  (`adminEmail`, `adminFirstName`, `adminLastName`), solange das Onboarding nicht abgeschlossen ist.
- **Event ohne personenbezogene Daten:** Neue `TenantRegistered`-Events tragen kein `admin` mehr (`null`).
  `StudioOnboarding` liest die **aktuellen** Daten über `Tenant.initialAdmin()`. Damit verwenden „Erneut versuchen“
  und die automatische Wiederholung korrigierte Daten, ohne das gespeicherte Event zu ändern.
- **Alte Zustellungen (vor #42):** Das Feld `admin` bleibt im Record, damit offene Zustellungen lesbar bleiben
  (`TenantRegisteredCompatibilityTests`). Der Changeset `202610091042-tenant-initial-admin.sql` übernimmt die
  Admin-Daten offener Studios aus `event_publication` in die neuen Spalten. Fehlen sie am Studio trotzdem, nimmt
  `StudioOnboarding` die Kopie aus dem Event; fehlen sie in beiden, schlägt das Onboarding mit einem deutschen Grund
  fehl. Bei abgeschlossenen Zustellungen entfernt der Changeset `admin` aus dem gespeicherten Event; offene
  Zustellungen bleiben unverändert, weil Spring Modulith sie bei der Wiederholung liest.
- **Ändern:** `PATCH /api/platform/tenants/{id}` (`UpdateTenantRequest`): Name immer; Admin-Daten nur, solange das
  Onboarding nicht abgeschlossen ist (sonst 409 mit deutschem Text). `adminEmail` fehlt = Admin unverändert; sonst
  werden E-Mail, Vor- und Nachname zusammen ersetzt. Das Kürzel (= Alias und Name der Keycloak-Organisation) ist
  unveränderlich. Nach geänderten Admin-Daten stößt der Controller sofort `retryOnboarding` an (wie „Erneut
  versuchen“; ohne fehlgeschlagene Zustellung passiert nichts). `GET /api/platform/tenants/{id}` liefert ein Studio
  für das Formular.
- **Name in Keycloak:** Eine Namensänderung veröffentlicht `TenantRenamed`; `StudioOnboarding` setzt die
  Beschreibung der Organisation (idempotent, mit dem **aktuellen** Namen aus der Datenbank, damit verspätete
  Zustellungen nichts zurückdrehen). Gibt es noch keine Organisation, passiert nichts; das Onboarding gleicht die
  Beschreibung beim Anlegen bzw. bei jeder Wiederholung an.
- **Optimistische Sperre:** `tenant.version` (`@Version`). Bearbeiten und Onboarding (z. B. `markOnboarded`,
  `recordOnboardingFailure`) dürfen sich nicht gegenseitig überschreiben. Trifft das Bearbeiten auf eine gleichzeitige
  Änderung, antwortet die API mit 409 („bitte neu laden“); trifft es den Listener, wird die Zustellung wie jeder
  Fehler wiederholt.
- **Oberfläche:** „Bearbeiten“ in jeder Zeile der Studio-Liste, „Daten korrigieren“ direkt unter einer
  Fehlermeldung. Formular `/plattform/studios/:id` = Registrierungsformular (gleiche Prüfregeln), Kürzel gesperrt,
  Admin-Felder nur bis zum Abschluss des Onboardings, Fehlergrund oben im Formular.

## Consequences
- Studios mit falscher Admin-E-Mail lassen sich ohne Eingriff in Keycloak oder Datenbank reparieren.
- Hat ein früherer Versuch schon einen Keycloak-Benutzer mit der alten E-Mail angelegt (z. B. Fehler erst beim
  Mailversand), bleibt dieser bestehen; ggf. ist er sogar Mitglied der Organisation. Aufräumen ist Handarbeit in
  Keycloak.
- Kleines Zeitfenster: Ändert der Betreiber die Admin-Daten, während ein Versuch mit den alten Daten gerade
  erfolgreich endet, gilt das Onboarding als abgeschlossen und die Änderung ist verworfen (Daten gelöscht).
- Abgeschlossene Zustellungen von **neuen** Events enthalten keine personenbezogenen Daten; offene alte Zustellungen
  behalten die Admin-Daten bis zu ihrem Abschluss (danach bleiben sie im Event – Aufräumen des
  `event_publication`-Archivs ist ein eigenes Thema).
- Weitere Listener auf `TenantRegistered` dürfen sich nicht auf `admin()` verlassen.
