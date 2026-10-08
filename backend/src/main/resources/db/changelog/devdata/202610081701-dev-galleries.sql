--liquibase formatted sql

-- Nur Kontext "dev": Beispielgalerien für Studio A (alle Status, eine abgelaufen) und Studio B

--changeset photoffice:dev-galleries context:dev
INSERT INTO gallery (id, tenant_id, name, description, status, expires_on, published_at, created_at, updated_at)
VALUES
  ('f1000000-0000-4000-8000-000000000001', 'a0000000-0000-4000-8000-00000000000a', 'Hochzeit Becker',
   'Eure Hochzeit am Schlossteich – viel Freude beim Aussuchen!', 'ONLINE', current_date + 90, now(), now(), now()),
  ('f1000000-0000-4000-8000-000000000002', 'a0000000-0000-4000-8000-00000000000a', 'Familienshooting Wagner',
   NULL, 'DRAFT', NULL, NULL, now(), now()),
  ('f1000000-0000-4000-8000-000000000003', 'a0000000-0000-4000-8000-00000000000a', 'Bewerbungsfotos Koch',
   NULL, 'OFFLINE', current_date + 30, now() - interval '10 days', now(), now()),
  ('f1000000-0000-4000-8000-000000000004', 'a0000000-0000-4000-8000-00000000000a', 'Babybauch Schröder',
   'Babybauch-Shooting im Studio', 'ONLINE', current_date - 5, now() - interval '60 days', now(), now()),
  ('f1000000-0000-4000-8000-0000000000b1', 'b0000000-0000-4000-8000-00000000000b', 'Porträt Vogel',
   NULL, 'DRAFT', NULL, NULL, now(), now())
ON CONFLICT DO NOTHING;

INSERT INTO gallery_customer (tenant_id, gallery_id, customer_id)
VALUES
  ('a0000000-0000-4000-8000-00000000000a', 'f1000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001'),
  ('a0000000-0000-4000-8000-00000000000a', 'f1000000-0000-4000-8000-000000000002', 'c0000000-0000-4000-8000-000000000003'),
  ('a0000000-0000-4000-8000-00000000000a', 'f1000000-0000-4000-8000-000000000003', 'c0000000-0000-4000-8000-000000000006'),
  ('a0000000-0000-4000-8000-00000000000a', 'f1000000-0000-4000-8000-000000000004', 'c0000000-0000-4000-8000-00000000000a'),
  ('b0000000-0000-4000-8000-00000000000b', 'f1000000-0000-4000-8000-0000000000b1', 'c0000000-0000-4000-8000-0000000000b1')
ON CONFLICT DO NOTHING;
