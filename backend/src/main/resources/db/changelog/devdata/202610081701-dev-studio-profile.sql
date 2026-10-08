--liquibase formatted sql

-- Nur Kontext "dev": Studio-Profile für Studio A (Berlin, vollständig, mit AGB und Impressum) und Studio B
-- (Wien, Kleinunternehmer ohne UID, noch ohne Rechtstexte). Alle Daten sind erfunden; die IBANs sind die
-- offiziellen Beispiel-IBANs mit gültiger Prüfsumme.

--changeset photoffice:dev-studio-profile context:dev
INSERT INTO studio_profile (tenant_id, display_name, street, postal_code, city, country, email, phone, website,
                            tax_number, vat_id, account_holder, iban, bic, updated_at)
VALUES ('a0000000-0000-4000-8000-00000000000a', 'Studio A Fotografie', 'Lindenstraße 4', '10969', 'Berlin', 'DE',
        'kontakt@studio-a.example', '030 1234567', 'https://studio-a.example', '12/345/67890', 'DE123456789',
        'Studio A Fotografie GmbH', 'DE89370400440532013000', 'COBADEFFXXX', now()),
       ('b0000000-0000-4000-8000-00000000000b', 'Studio B', 'Mariahilfer Straße 12', '1060', 'Wien', 'AT',
        'hallo@studio-b.example', '+43 1 2345678', NULL, '12-345/6789', NULL,
        'Studio B e.U.', 'AT611904300234573201', NULL, now())
ON CONFLICT DO NOTHING;

--changeset photoffice:dev-studio-legal-texts context:dev
INSERT INTO studio_legal_text (id, tenant_id, kind, markdown, updated_at)
VALUES ('f2000000-0000-4000-8000-000000000001', 'a0000000-0000-4000-8000-00000000000a', 'TERMS_AND_CONDITIONS',
        '# Allgemeine Geschäftsbedingungen

*Beispieltext für die Entwicklung – keine rechtlich geprüften AGB.*

## 1. Geltungsbereich

Diese AGB gelten für alle Bestellungen von Abzügen und Downloads über die Online-Galerien von **Studio A Fotografie**.

## 2. Vertragsschluss

1. Die Darstellung der Bilder in der Galerie ist kein bindendes Angebot.
2. Mit dem Klick auf **Zahlungspflichtig bestellen** geben Sie eine verbindliche Bestellung ab.

## 3. Preise

Alle Preise sind Endpreise inkl. Umsatzsteuer, zuzüglich Versandkosten (siehe [Preisliste](https://studio-a.example/preise)).', now()),
       ('f2000000-0000-4000-8000-000000000002', 'a0000000-0000-4000-8000-00000000000a', 'IMPRINT',
        '# Impressum

**Studio A Fotografie GmbH**
Lindenstraße 4
10969 Berlin

Telefon: 030 1234567
E-Mail: kontakt@studio-a.example

Umsatzsteuer-ID: DE123456789

*Beispieltext für die Entwicklung.*', now())
ON CONFLICT DO NOTHING;
