--liquibase formatted sql

-- Galerien eines Studios (Issue #8, Legacy F1). Bilder folgen mit #9, der Zugang per Link mit #11.

--changeset photoffice:customer-tenant-id-unique
-- Ziel für mandantensichere Fremdschlüssel auf Kunden (z. B. aus gallery_customer)
ALTER TABLE customer ADD CONSTRAINT customer_tenant_id_uk UNIQUE (tenant_id, id);

--changeset photoffice:gallery-table
CREATE TABLE gallery
(
    id           UUID PRIMARY KEY,
    tenant_id    UUID        NOT NULL REFERENCES tenant (id),
    name         TEXT        NOT NULL,
    description  TEXT,
    status       TEXT        NOT NULL,
    expires_on   DATE,
    published_at TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL,
    updated_at   TIMESTAMPTZ NOT NULL,
    CONSTRAINT gallery_status_values CHECK (status IN ('DRAFT', 'ONLINE', 'OFFLINE')),
    CONSTRAINT gallery_tenant_id_uk UNIQUE (tenant_id, id)
);
CREATE INDEX gallery_tenant_name_idx ON gallery (tenant_id, lower(name));

--changeset photoffice:gallery-tenant-isolation
SELECT enable_tenant_isolation('gallery');

--changeset photoffice:gallery-customer-table
-- Zuordnung Galerie ↔ Kunde (n:m). Wird ein Kunde oder eine Galerie gelöscht, verschwindet die Zuordnung.
CREATE TABLE gallery_customer
(
    tenant_id   UUID NOT NULL,
    gallery_id  UUID NOT NULL,
    customer_id UUID NOT NULL,
    PRIMARY KEY (gallery_id, customer_id),
    CONSTRAINT gallery_customer_gallery_fk FOREIGN KEY (tenant_id, gallery_id)
        REFERENCES gallery (tenant_id, id) ON DELETE CASCADE,
    CONSTRAINT gallery_customer_customer_fk FOREIGN KEY (tenant_id, customer_id)
        REFERENCES customer (tenant_id, id) ON DELETE CASCADE
);
CREATE INDEX gallery_customer_customer_idx ON gallery_customer (tenant_id, customer_id);

--changeset photoffice:gallery-customer-tenant-isolation
SELECT enable_tenant_isolation('gallery_customer');
