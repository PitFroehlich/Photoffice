# 0004: SeaweedFS statt MinIO als lokaler S3-Speicher

- **Status:** Accepted
- **Date:** 2026-10-08
- **Ändert:** ADR 0003 (nur den Punkt "lokal MinIO")

## Context
ADR 0003 sah MinIO als lokalen Ersatz für den S3-kompatiblen Object Storage vor.
MinIO stellt für die Community-Edition keine fertigen Docker-Images mehr bereit; `minio/minio` und `quay.io/minio/minio` ließen sich beim Aufsetzen des Grundgerüsts (#4) nicht mehr laden.

## Decision
Lokal (Docker Compose) und in Tests wird **SeaweedFS** (Apache-2.0, aktiv gepflegt, S3-API) verwendet.
Produktiv bleibt es beim S3-kompatiblen Object Storage des Hosters (z. B. Hetzner Object Storage).
Der Code spricht ausschließlich die S3-API an und darf keine SeaweedFS-spezifischen Funktionen nutzen.

## Consequences
- Ein Bucket wird lokal per S3-API angelegt (`seaweedfs-init` in `docker-compose.yml`).
- Für Testcontainers gibt es kein fertiges SeaweedFS-Modul; S3-Integrationstests nutzen einen `GenericContainer`.
- Wechsel des lokalen S3-Ersatzes bleibt einfach, solange nur die S3-API verwendet wird.
