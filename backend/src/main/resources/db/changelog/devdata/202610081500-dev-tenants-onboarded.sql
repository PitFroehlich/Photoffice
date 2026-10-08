--liquibase formatted sql

-- Nur Kontext "dev": Organisationen und Benutzer der Dev-Studios kommen aus dem Dev-Realm, nicht aus dem Onboarding

--changeset photoffice:dev-tenants-onboarded context:dev
UPDATE tenant SET onboarded_at = created_at WHERE slug IN ('studio-a', 'studio-b') AND onboarded_at IS NULL;
