# Photoffice

Galerie- und Bestellplattform für Fotostudios (SaaS, mandantenfähig) – Neuentwicklung.

- **Stand und Fahrplan:** angepinntes Roadmap-Issue (#21) und `docs/analysis/legacy-features.md`
- **Entscheidungen:** `docs/decisions/`
- **Entwickeln, Testen, Konventionen:** `AGENTS.md`
- **Alte PHP-Anwendung (Referenz):** `legacy/`

## Schnellstart

```bash
docker compose up -d
cd backend && ./mvnw spring-boot:run      # JDK 25
cd frontend && npm install && npm start   # http://localhost:4200
```

## Lizenz

Lizenz des Neubaus: noch nicht festgelegt. Legacy-Anwendung: siehe `legacy/README.md`.
