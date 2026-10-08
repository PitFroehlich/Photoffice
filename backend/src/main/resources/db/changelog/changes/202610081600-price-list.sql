--liquibase formatted sql

-- Preisliste eines Studios (Issue #13, Legacy F7 + neues Feature F17), Modell siehe ADR 0007.
-- Alle Beträge sind Bruttopreise in Cent (INTEGER, nie Gleitkomma) in der Währung aus price_list_settings.

--changeset photoffice:price-list-settings-table
-- Höchstens eine Zeile pro Studio; fehlt sie, gelten die Standardwerte (EUR, 19 %).
CREATE TABLE price_list_settings
(
    tenant_id        UUID PRIMARY KEY REFERENCES tenant (id),
    currency         TEXT          NOT NULL,
    vat_rate_percent NUMERIC(4, 2) NOT NULL,
    updated_at       TIMESTAMPTZ   NOT NULL,
    CONSTRAINT price_list_settings_currency_values CHECK (currency IN ('EUR')),
    CONSTRAINT price_list_settings_vat_rate_range CHECK (vat_rate_percent >= 0 AND vat_rate_percent < 100)
);

--changeset photoffice:price-list-settings-tenant-isolation
SELECT enable_tenant_isolation('price_list_settings');

--changeset photoffice:product-table
-- Einzeln verkaufte Produkte. Typabhängige Spalten sind per CHECK abgesichert; ein neuer Produkttyp
-- (z. B. Fotobuch) ergänzt eigene Spalten und einen weiteren Zweig in product_type_attributes.
CREATE TABLE product
(
    id           UUID PRIMARY KEY,
    tenant_id    UUID        NOT NULL REFERENCES tenant (id),
    type         TEXT        NOT NULL,
    paper_type   TEXT,
    print_format TEXT,
    resolution   TEXT,
    price_cents  INTEGER     NOT NULL,
    active       BOOLEAN     NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL,
    updated_at   TIMESTAMPTZ NOT NULL,
    CONSTRAINT product_price_range CHECK (price_cents BETWEEN 0 AND 10000000),
    CONSTRAINT product_resolution_values CHECK (resolution IN ('WEB', 'FULL')),
    CONSTRAINT product_type_attributes CHECK (
        (type = 'PRINT' AND paper_type IS NOT NULL AND print_format IS NOT NULL AND resolution IS NULL)
            OR (type = 'DOWNLOAD' AND resolution IS NOT NULL AND paper_type IS NULL AND print_format IS NULL))
);
-- Jede Kombination Papier × Format bzw. jede Download-Auflösung gibt es pro Studio nur einmal
CREATE UNIQUE INDEX product_print_uk ON product (tenant_id, lower(paper_type), lower(print_format)) WHERE type = 'PRINT';
CREATE UNIQUE INDEX product_download_uk ON product (tenant_id, resolution) WHERE type = 'DOWNLOAD';

--changeset photoffice:product-tenant-isolation
SELECT enable_tenant_isolation('product');

--changeset photoffice:download-package-table
-- Download-Pakete: n Bilder oder die ganze Galerie zu einem Paketpreis
CREATE TABLE download_package
(
    id          UUID PRIMARY KEY,
    tenant_id   UUID        NOT NULL REFERENCES tenant (id),
    name        TEXT        NOT NULL,
    kind        TEXT        NOT NULL,
    image_count INTEGER,
    resolution  TEXT        NOT NULL,
    price_cents INTEGER     NOT NULL,
    active      BOOLEAN     NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL,
    CONSTRAINT download_package_price_range CHECK (price_cents BETWEEN 0 AND 10000000),
    CONSTRAINT download_package_resolution_values CHECK (resolution IN ('WEB', 'FULL')),
    CONSTRAINT download_package_kind_attributes CHECK (
        (kind = 'IMAGE_COUNT' AND image_count BETWEEN 2 AND 10000)
            OR (kind = 'WHOLE_GALLERY' AND image_count IS NULL))
);
CREATE UNIQUE INDEX download_package_tenant_name_uk ON download_package (tenant_id, lower(name));

--changeset photoffice:download-package-tenant-isolation
SELECT enable_tenant_isolation('download_package');

--changeset photoffice:shipping-method-table
CREATE TABLE shipping_method
(
    id          UUID PRIMARY KEY,
    tenant_id   UUID        NOT NULL REFERENCES tenant (id),
    name        TEXT        NOT NULL,
    price_cents INTEGER     NOT NULL,
    active      BOOLEAN     NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL,
    CONSTRAINT shipping_method_price_range CHECK (price_cents BETWEEN 0 AND 10000000)
);
CREATE UNIQUE INDEX shipping_method_tenant_name_uk ON shipping_method (tenant_id, lower(name));

--changeset photoffice:shipping-method-tenant-isolation
SELECT enable_tenant_isolation('shipping_method');
