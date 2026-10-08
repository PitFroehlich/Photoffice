--liquibase formatted sql

-- Nur Kontext "dev": Beispiel-Preislisten für Studio A (vollständig, mit inaktiven Einträgen) und Studio B (klein,
-- Kleinunternehmer ohne Umsatzsteuer). Beträge in Cent.

--changeset photoffice:dev-price-list context:dev
INSERT INTO price_list_settings (tenant_id, currency, vat_rate_percent, updated_at)
VALUES ('a0000000-0000-4000-8000-00000000000a', 'EUR', 19.00, now()),
       ('b0000000-0000-4000-8000-00000000000b', 'EUR', 0.00, now())
ON CONFLICT DO NOTHING;

INSERT INTO product (id, tenant_id, type, paper_type, print_format, resolution, price_cents, active, created_at, updated_at)
VALUES
  ('e1000000-0000-4000-8000-000000000001', 'a0000000-0000-4000-8000-00000000000a', 'PRINT', 'Glänzend', '10 × 15 cm', NULL, 190, TRUE, now(), now()),
  ('e1000000-0000-4000-8000-000000000002', 'a0000000-0000-4000-8000-00000000000a', 'PRINT', 'Matt', '10 × 15 cm', NULL, 190, TRUE, now(), now()),
  ('e1000000-0000-4000-8000-000000000003', 'a0000000-0000-4000-8000-00000000000a', 'PRINT', 'Glänzend', '13 × 18 cm', NULL, 290, TRUE, now(), now()),
  ('e1000000-0000-4000-8000-000000000004', 'a0000000-0000-4000-8000-00000000000a', 'PRINT', 'Matt', '13 × 18 cm', NULL, 290, TRUE, now(), now()),
  ('e1000000-0000-4000-8000-000000000005', 'a0000000-0000-4000-8000-00000000000a', 'PRINT', 'Matt', '20 × 30 cm', NULL, 790, TRUE, now(), now()),
  ('e1000000-0000-4000-8000-000000000006', 'a0000000-0000-4000-8000-00000000000a', 'PRINT', 'Fine Art', '30 × 45 cm', NULL, 2490, FALSE, now(), now()),
  ('e1000000-0000-4000-8000-000000000007', 'a0000000-0000-4000-8000-00000000000a', 'DOWNLOAD', NULL, NULL, 'WEB', 490, TRUE, now(), now()),
  ('e1000000-0000-4000-8000-000000000008', 'a0000000-0000-4000-8000-00000000000a', 'DOWNLOAD', NULL, NULL, 'FULL', 990, TRUE, now(), now()),
  ('e1000000-0000-4000-8000-0000000000b1', 'b0000000-0000-4000-8000-00000000000b', 'PRINT', 'Glänzend', '10 × 15 cm', NULL, 250, TRUE, now(), now()),
  ('e1000000-0000-4000-8000-0000000000b2', 'b0000000-0000-4000-8000-00000000000b', 'DOWNLOAD', NULL, NULL, 'FULL', 1500, TRUE, now(), now())
ON CONFLICT DO NOTHING;

INSERT INTO download_package (id, tenant_id, name, kind, image_count, resolution, price_cents, active, created_at, updated_at)
VALUES
  ('e2000000-0000-4000-8000-000000000001', 'a0000000-0000-4000-8000-00000000000a', '10 Downloads', 'IMAGE_COUNT', 10, 'FULL', 6900, TRUE, now(), now()),
  ('e2000000-0000-4000-8000-000000000002', 'a0000000-0000-4000-8000-00000000000a', 'Ganze Galerie', 'WHOLE_GALLERY', NULL, 'FULL', 14900, TRUE, now(), now()),
  ('e2000000-0000-4000-8000-000000000003', 'a0000000-0000-4000-8000-00000000000a', '5 Downloads Web', 'IMAGE_COUNT', 5, 'WEB', 1990, FALSE, now(), now()),
  ('e2000000-0000-4000-8000-0000000000b1', 'b0000000-0000-4000-8000-00000000000b', 'Alle Bilder', 'WHOLE_GALLERY', NULL, 'FULL', 9900, TRUE, now(), now())
ON CONFLICT DO NOTHING;

INSERT INTO shipping_method (id, tenant_id, name, price_cents, active, created_at, updated_at)
VALUES
  ('e3000000-0000-4000-8000-000000000001', 'a0000000-0000-4000-8000-00000000000a', 'Abholung im Studio', 0, TRUE, now(), now()),
  ('e3000000-0000-4000-8000-000000000002', 'a0000000-0000-4000-8000-00000000000a', 'Standardversand', 490, TRUE, now(), now()),
  ('e3000000-0000-4000-8000-000000000003', 'a0000000-0000-4000-8000-00000000000a', 'Expressversand', 1290, FALSE, now(), now()),
  ('e3000000-0000-4000-8000-0000000000b1', 'b0000000-0000-4000-8000-00000000000b', 'Briefversand', 200, TRUE, now(), now())
ON CONFLICT DO NOTHING;
