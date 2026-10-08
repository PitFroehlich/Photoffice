# 0003: Tech-Stack für den Neubau

- **Status:** Accepted
- **Date:** 2026-10-08

## Context
Photoffice wird als mandantenfähiges SaaS-Produkt für viele Fotostudios neu entwickelt ([ADR 0002](0002-rewrite-multi-tenant.md)).
Anforderungen aus der [Feature-Analyse](../analysis/legacy-features.md):
- Viele Studios von Anfang an; strikte Mandantentrennung.
- Galerien mit großen Bilddateien; Originale bleiben erhalten (Downloads, externer Druck-Service); Speicherverbrauch pro Studio messbar (Abo-Tarife).
- Studio-eigene Wasserzeichen auf Vorschaubildern.
- Erweiterbar um Bezahldienst, Druck-Service-Anbindung und Benachrichtigungen, ohne den Kern umzubauen.
- Hosting durch uns in einer EU-Cloud.
- Team-Know-how: Java, TypeScript. Gewünscht: SPA + API.

## Decision

| Bereich | Entscheidung |
|---|---|
| Backend | **Java (aktuelles LTS, mind. 22 wegen FFM für vips-ffm) + Spring Boot**, als modularer Monolith mit **Spring Modulith**. Module kommunizieren über Domain-Events (persistiertes Event-Publication-Registry). |
| API | REST, beschrieben per **OpenAPI**; Frontend-Client wird daraus generiert. |
| Frontend | **Angular + TypeScript** als SPA. Zwei Oberflächen: Studio-Backoffice und Kunden-Galerie. |
| Datenbank | **PostgreSQL**; Mandant per `tenant_id`-Spalte, zusätzlich abgesichert durch **Row-Level Security**. Schema-Migrationen mit Flyway. |
| Bildspeicher | **S3-kompatibler Object Storage** (z. B. Hetzner Object Storage; lokal SeaweedFS, siehe ADR 0004). Präfix pro Mandant für Verbrauchsmessung. Zugriff nur über signierte, zeitlich begrenzte URLs. |
| Bildverarbeitung | **imgproxy (Open-Source-Version, MIT)** für alle Ableitungen (Größen, Thumbnails, Formate), on-the-fly mit Cache. Das **studiospezifische Wasserzeichen** wird beim Upload einmalig vom Backend mit **libvips (vips-ffm)** in eine Vorschau-Datei gerendert; imgproxy skaliert diese weiter. EXIF-Auslesen ebenfalls im Backend beim Upload. |
| Authentifizierung | **Keycloak** (selbst gehostet), Feature *Organizations* für Studios, Rollen pro Studio. Backend als OAuth2 Resource Server. Der **Galerie-Link mit Code** für Endkunden ist eine eigene Implementierung im Backend (kein Keycloak-Login). |
| Bezahlung | **Bewusst offen.** Zahlungen (Studio-Abos und Endkunden-Bestellungen) laufen ausschließlich über eine eigene Schnittstelle (Port); der Anbieter wird in einem späteren ADR gewählt. |
| Hintergrundverarbeitung | Asynchrone Event-Listener von Spring Modulith (Upload-Nachbearbeitung, Mails, Druckaufträge); dedizierte Queue erst bei Bedarf. |
| Hosting/Betrieb | **EU-Cloud** (Hetzner oder IONOS), alle Komponenten als Container. Orchestrierung (k3s vs. managed Kubernetes) in einem eigenen ADR. |
| Tests/CI | JUnit + **Testcontainers** (PostgreSQL, S3, Keycloak), Spring-Modulith-Modultests, **Playwright** für E2E, GitHub Actions. |

### Verworfene Alternativen
- **Kotlin** statt Java – Team bevorzugt Java.
- **TypeScript durchgängig (NestJS)** – möglich, aber mehr Eigenbau für Modulgrenzen und zuverlässige Event-Verarbeitung.
- **React** statt Angular – Team bevorzugt Angular.
- **imgproxy Pro** – nötig nur für Wasserzeichen pro Anfrage (`watermark_url`); kostenpflichtig. Bei Bedarf später ohne Umbau einsetzbar.
- **Thumbor** (langsamer, mehr Ressourcen), **imaginary** (kaum noch gepflegt), **nur Backend-Rendering** (jede neue Bildgröße erfordert Neuberechnung aller Bilder).
- **Eigene Authentifizierung** – Keycloak liefert Login, Rollen, Passwort-Reset, 2FA.

## Consequences
- Mehrere Laufzeitkomponenten (Backend, Frontend, PostgreSQL, Keycloak, imgproxy, Object Storage) → lokale Entwicklung braucht Docker Compose; Betrieb braucht Container-Orchestrierung.
- Row-Level Security erfordert, dass jede DB-Verbindung den aktuellen Mandanten setzt (z. B. per Session-Variable) – muss früh zentral gelöst werden (P0).
- Die Vorschau mit Wasserzeichen wird zusätzlich zum Original gespeichert; ändert ein Studio sein Wasserzeichen, müssen die Vorschauen neu gerendert werden (Hintergrundjob).
- Bezahlung, Druck-Service und Orchestrierung brauchen eigene ADRs, bevor sie umgesetzt werden.
- `AGENTS.md` beschreibt noch den Legacy-Stack; Setup-, Test- und Konventionsabschnitte für den Neubau werden mit dem ersten Code ergänzt.
