--liquibase formatted sql

-- Plattform-Oberfläche (Issue #35): letzter Fehler beim Studio-Onboarding, damit der Betreiber ihn sieht.
-- NULL = kein Fehler (noch kein Versuch, Versuch läuft oder Onboarding abgeschlossen).

--changeset photoffice:tenant-onboarding-error
ALTER TABLE tenant ADD COLUMN onboarding_error TEXT;
ALTER TABLE tenant ADD COLUMN onboarding_failed_at TIMESTAMPTZ;
