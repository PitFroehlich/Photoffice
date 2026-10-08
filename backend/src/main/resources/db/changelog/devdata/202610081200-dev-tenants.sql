--liquibase formatted sql

-- Nur Kontext "dev" (Profil dev): Studios passend zu den Keycloak-Organisationen im Dev-Realm (Alias = slug)

--changeset photoffice:dev-tenants context:dev
INSERT INTO tenant (id, slug, name, status, created_at)
VALUES ('a0000000-0000-4000-8000-00000000000a', 'studio-a', 'Studio A', 'ACTIVE', now()),
       ('b0000000-0000-4000-8000-00000000000b', 'studio-b', 'Studio B', 'ACTIVE', now())
ON CONFLICT (slug) DO NOTHING;
