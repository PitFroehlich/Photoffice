--liquibase formatted sql

-- Endkunden eines Studios (Issue #7, Legacy F5).
-- Ein späteres optionales Kundenkonto (F20) verweist auf diese Tabelle (customer_account.customer_id),
-- daher bleibt der Kunde selbst unabhängig von einem Login.

--changeset photoffice:customer-table
CREATE TABLE customer
(
    id          UUID PRIMARY KEY,
    tenant_id   UUID        NOT NULL REFERENCES tenant (id),
    first_name  TEXT        NOT NULL,
    last_name   TEXT        NOT NULL,
    email       TEXT        NOT NULL,
    phone       TEXT,
    street      TEXT,
    postal_code TEXT,
    city        TEXT,
    notes       TEXT,
    created_at  TIMESTAMPTZ NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL
);
-- E-Mail eindeutig pro Studio, ohne Groß-/Kleinschreibung
CREATE UNIQUE INDEX customer_tenant_email_uk ON customer (tenant_id, lower(email));
CREATE INDEX customer_tenant_name_idx ON customer (tenant_id, last_name, first_name);

--changeset photoffice:customer-tenant-isolation
SELECT enable_tenant_isolation('customer');
