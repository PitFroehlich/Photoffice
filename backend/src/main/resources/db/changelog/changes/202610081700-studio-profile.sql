--liquibase formatted sql

-- Studio-Profil und Rechtstexte eines Studios (Issue #20, Legacy F11 "firma").
-- Formatregeln (PLZ je Land, IBAN-Prüfsumme, USt-IdNr. …) prüft das Backend (StudioProfileData), die Datenbank
-- sichert nur die Grundstruktur ab.

--changeset photoffice:studio-profile-table
-- Höchstens eine Zeile pro Studio; fehlt sie, liefert die API ein leeres Profil mit dem Studionamen.
CREATE TABLE studio_profile
(
    tenant_id      UUID PRIMARY KEY REFERENCES tenant (id),
    display_name   TEXT        NOT NULL,
    street         TEXT,
    postal_code    TEXT,
    city           TEXT,
    country        TEXT        NOT NULL,
    email          TEXT,
    phone          TEXT,
    website        TEXT,
    tax_number     TEXT,
    vat_id         TEXT,
    account_holder TEXT,
    iban           TEXT,
    bic            TEXT,
    updated_at     TIMESTAMPTZ NOT NULL,
    CONSTRAINT studio_profile_country_values CHECK (country IN ('DE', 'AT', 'CH')),
    CONSTRAINT studio_profile_bank_account_complete CHECK ((account_holder IS NULL) = (iban IS NULL))
);

--changeset photoffice:studio-profile-tenant-isolation
SELECT enable_tenant_isolation('studio_profile');

--changeset photoffice:studio-legal-text-table
-- Ein Markdown-Text pro Studio und Art (AGB, Widerrufsbelehrung, Impressum, Datenschutzerklärung).
-- Ein leerer Text wird nicht gespeichert (Zeile gelöscht).
CREATE TABLE studio_legal_text
(
    id         UUID PRIMARY KEY,
    tenant_id  UUID        NOT NULL REFERENCES tenant (id),
    kind       TEXT        NOT NULL,
    markdown   TEXT        NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT studio_legal_text_kind_values
        CHECK (kind IN ('TERMS_AND_CONDITIONS', 'CANCELLATION_POLICY', 'IMPRINT', 'PRIVACY_POLICY')),
    CONSTRAINT studio_legal_text_length CHECK (char_length(markdown) BETWEEN 1 AND 50000)
);
CREATE UNIQUE INDEX studio_legal_text_tenant_kind_uk ON studio_legal_text (tenant_id, kind);

--changeset photoffice:studio-legal-text-tenant-isolation
SELECT enable_tenant_isolation('studio_legal_text');
