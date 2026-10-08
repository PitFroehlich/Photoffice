--liquibase formatted sql

-- Studio bearbeiten (Issue #42, ADR 0012): Daten des ersten Studio-Admins am Studio, solange das Onboarding nicht
-- abgeschlossen ist (danach NULL, Datensparsamkeit). Bisher standen sie nur im Event TenantRegistered.
-- version: optimistische Sperre, damit sich Bearbeiten und Onboarding nicht gegenseitig überschreiben.

--changeset photoffice:tenant-initial-admin
ALTER TABLE tenant ADD COLUMN admin_email TEXT;
ALTER TABLE tenant ADD COLUMN admin_first_name TEXT;
ALTER TABLE tenant ADD COLUMN admin_last_name TEXT;
ALTER TABLE tenant ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

--changeset photoffice:tenant-initial-admin-backfill splitStatements:false
-- Studios, deren Onboarding vor #42 begann und noch nicht abgeschlossen ist: Admin-Daten aus der (letzten)
-- Zustellung von TenantRegistered übernehmen, damit man sie bearbeiten kann. Nicht lesbare Einträge werden
-- übersprungen – das Onboarding nutzt dann weiter die Daten aus dem Event.
DO $$
DECLARE
    publication RECORD;
    event JSONB;
BEGIN
    FOR publication IN
        SELECT serialized_event FROM event_publication
        WHERE event_type = 'de.photoffice.tenant.TenantRegistered'
        ORDER BY publication_date
    LOOP
        BEGIN
            event := publication.serialized_event::jsonb;
            UPDATE tenant
            SET admin_email      = event #>> '{admin,email}',
                admin_first_name = event #>> '{admin,firstName}',
                admin_last_name  = event #>> '{admin,lastName}'
            WHERE id = (event #>> '{tenantId,value}')::uuid
              AND onboarded_at IS NULL
              AND event #>> '{admin,email}' IS NOT NULL;
        EXCEPTION WHEN others THEN
            RAISE NOTICE 'TenantRegistered publication skipped: %', SQLERRM;
        END;
    END LOOP;
END
$$;

--changeset photoffice:tenant-registered-completed-without-admin splitStatements:false
-- Datensparsamkeit: abgeschlossene Zustellungen brauchen die Admin-Daten nicht mehr (neue Events enthalten sie
-- gar nicht). Offene Zustellungen bleiben unverändert, Spring Modulith liest sie bei der Wiederholung.
DO $$
DECLARE
    publication RECORD;
BEGIN
    FOR publication IN
        SELECT id, serialized_event FROM event_publication
        WHERE event_type = 'de.photoffice.tenant.TenantRegistered' AND completion_date IS NOT NULL
    LOOP
        BEGIN
            UPDATE event_publication
            SET serialized_event = (publication.serialized_event::jsonb - 'admin')::text
            WHERE id = publication.id;
        EXCEPTION WHEN others THEN
            RAISE NOTICE 'TenantRegistered publication % skipped: %', publication.id, SQLERRM;
        END;
    END LOOP;
END
$$;
