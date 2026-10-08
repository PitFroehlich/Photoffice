--liquibase formatted sql

-- Studio-Onboarding (Issue #24): Zeitpunkt, zu dem Keycloak-Organisation und erster Studio-Admin angelegt waren.
-- NULL = Onboarding läuft noch bzw. wird nach einem Fehler wiederholt.

--changeset photoffice:tenant-onboarded-at
ALTER TABLE tenant ADD COLUMN onboarded_at TIMESTAMPTZ;
