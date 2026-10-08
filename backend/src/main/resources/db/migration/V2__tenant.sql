-- Mandantenfähigkeit (ADR 0002/0003, Issue #5)
--
-- Die Anwendung arbeitet zur Laufzeit als Rolle photoffice_app (SET ROLE bei jeder Verbindung, siehe
-- TenantAwareDataSource). Diese Rolle ist weder Superuser noch Tabellenbesitzer, daher greift Row-Level Security.
-- Flyway läuft weiterhin als Besitzer und legt das Schema an.

DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'photoffice_app') THEN
        CREATE ROLE photoffice_app NOLOGIN;
    END IF;
END
$$;

-- Der Login-Benutzer muss die Rolle annehmen dürfen
GRANT photoffice_app TO CURRENT_USER;

GRANT USAGE ON SCHEMA public TO photoffice_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO photoffice_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO photoffice_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO photoffice_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT USAGE, SELECT ON SEQUENCES TO photoffice_app;

-- Aktueller Mandant der Verbindung; NULL, wenn keiner gesetzt ist (dann sind keine Mandantendaten sichtbar)
CREATE FUNCTION current_tenant_id() RETURNS uuid
    LANGUAGE sql STABLE
AS $$
    SELECT NULLIF(current_setting('app.tenant_id', true), '')::uuid
$$;

-- Aktiviert die Mandantentrennung für eine Tabelle mit Spalte tenant_id.
-- Jede fachliche Tabelle muss in ihrer Migration so abgesichert werden:
--   SELECT enable_tenant_isolation('customer');
-- TenantIsolationCoverageTests schlägt fehl, wenn eine Tabelle mit tenant_id nicht abgesichert ist.
CREATE FUNCTION enable_tenant_isolation(target regclass) RETURNS void
    LANGUAGE plpgsql
AS $$
BEGIN
    EXECUTE format('ALTER TABLE %s ENABLE ROW LEVEL SECURITY', target);
    EXECUTE format('ALTER TABLE %s FORCE ROW LEVEL SECURITY', target);
    EXECUTE format(
        'CREATE POLICY tenant_isolation ON %s USING (tenant_id = current_tenant_id()) WITH CHECK (tenant_id = current_tenant_id())',
        target);
END
$$;

-- Studios (Mandanten). Bewusst ohne Row-Level Security: die Tabelle ist das Mandantenverzeichnis selbst;
-- der Zugriff wird in der Anwendung auf den Plattform-Betreiber beschränkt.
CREATE TABLE tenant
(
    id         UUID PRIMARY KEY,
    slug       TEXT        NOT NULL UNIQUE,
    name       TEXT        NOT NULL,
    status     TEXT        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT tenant_slug_format CHECK (slug ~ '^[a-z0-9]([a-z0-9-]{1,61}[a-z0-9])$'),
    CONSTRAINT tenant_status_values CHECK (status IN ('ACTIVE', 'SUSPENDED'))
);
