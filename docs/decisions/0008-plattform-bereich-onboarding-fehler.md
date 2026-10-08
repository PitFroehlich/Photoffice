# 0008: Plattform-Bereich im Frontend und sichtbare Onboarding-Fehler

- **Status:** Accepted
- **Date:** 2026-10-08

## Context
Der Plattform-Betreiber (Realm-Rolle `platform-admin`) hatte keine Oberfläche: Studios registrieren und sperren ging
nur per `curl`, und nach dem Login landete er im Studio-Bereich mit „Kein Zugriff“ (Issue #35). Scheitert das
Studio-Onboarding (ADR 0007), stand der Grund nur im Log; das Studio blieb ohne Erklärung auf `PENDING`.
`StudioOnboarding` läuft als `@ApplicationModuleListener` in einer eigenen Transaktion, die bei einem Fehler
zurückgerollt wird. Die Daten des ersten Admins (E-Mail, Name) stehen nur im Event `TenantRegistered`, nicht in der
Datenbank.

## Decision
- **Eigener Bereich `/plattform`** mit eigener Shell (`PlatformShell`, gleiches Layout wie der Studio-Bereich) und
  eigenem Guard (`platformGuard`). Welcher Bereich passt, entscheidet das Frontend anhand der Realm-Rollen im
  Access-Token (`AuthService.roles()`); das ist nur Navigation, die Rechte prüft weiterhin das Backend.
  Nach dem Login landen alle auf `/studio`; `studioGuard` leitet den Betreiber (ohne Studio-Rolle) nach
  `/plattform` weiter. Studio-Benutzer, die `/plattform` aufrufen, landen in `/studio`. Ein eigener Keycloak-Client
  für den Betreiber ist nicht nötig.
- **Letzter Onboarding-Fehler am Studio:** Spalten `tenant.onboarding_error` und `onboarding_failed_at`.
  `StudioOnboarding` fängt Fehler, speichert einen deutschen Grund über
  `TenantManagement.recordOnboardingFailure` (**`REQUIRES_NEW`**, damit er den Rollback der Listener-Transaktion
  übersteht) und wirft die Exception weiter – nur so markiert Spring Modulith die Zustellung als `FAILED` und
  `EventResubmission` wiederholt sie. Erfolg (`markOnboarded`) löscht den Fehler. Die API liefert ihn als
  `TenantResponse.onboardingError`/`onboardingFailedAt`; `onboardingStatus` bleibt `PENDING`/`COMPLETED`
  (kein neuer Status, weil das Onboarding weiter automatisch wiederholt wird).
- **„Erneut versuchen“** (`POST /api/platform/tenants/{id}/onboarding/retry`): stellt über
  `FailedEventPublications.resubmit` mit Filter (`TenantRegistered` dieses Studios, `minAge` 0) sofort erneut zu,
  statt auf den 5-Minuten-Takt zu warten. Ohne transaktionalen Kontext, damit der asynchrone Listener die
  Zustellung selbst abschließen kann. Gibt es keine fehlgeschlagene Zustellung, passiert nichts (202). Eine
  Wiederholung über die gespeicherte Zustellung ist nötig, weil nur das Event die Admin-Daten enthält.
- **Leere/ungültige Studio-ID:** Die Ursache des leeren 401 war Spring Securitys `StrictHttpFirewall`: Sie lehnt
  `//` ab, der Standard-Handler setzt nur den Status 400 per `sendError`, und die anschließende Fehlerseite
  (`/error`, Dispatcher-Typ `ERROR`) lief ohne Authentifizierung in `denyAll` → 401 ohne Body. Jetzt antwortet ein
  `RequestRejectedHandler` direkt mit einem Problem Detail (400), und `ERROR`-Dispatches sind erlaubt (sie zeigen
  nur den Fehler einer bereits geprüften Anfrage). Eine Studio-ID, die keine UUID ist, liefert 400 mit deutschem
  Text.

## Consequences
- Weitere Plattform-Seiten: Kind-Route unter `plattform` in `app.routes.ts` + Eintrag in
  `layout/platform-navigation.ts`.
- Fehlertexte für den Betreiber sind deutsch und werden im Backend gebildet (`StudioOnboarding.failureReason`).
- Andere Fehler, die über die Fehlerseite laufen (z. B. 500 außerhalb von MVC), liefern jetzt Spring Boots
  Fehler-JSON statt eines leeren 401.
- Die Studio-Liste lädt sich nach Aktionen etwa zwei Minuten lang alle 3 Sekunden neu, solange ein Onboarding läuft.
  Bei sehr vielen Studios braucht die Liste später Suche/Seiten (API liefert derzeit alle).
