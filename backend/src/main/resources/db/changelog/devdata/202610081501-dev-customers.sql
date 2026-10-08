--liquibase formatted sql

-- Nur Kontext "dev": Beispielkunden für Studio A (12) und Studio B (2)

--changeset photoffice:dev-customers context:dev
INSERT INTO customer (id, tenant_id, first_name, last_name, email, phone, street, postal_code, city, notes, created_at, updated_at)
VALUES
  ('c0000000-0000-4000-8000-000000000001', 'a0000000-0000-4000-8000-00000000000a', 'Julia', 'Becker', 'julia.becker@example.test', '0171 1234567', 'Lindenstraße 4', '10969', 'Berlin', 'Hochzeit Juni', now(), now()),
  ('c0000000-0000-4000-8000-000000000002', 'a0000000-0000-4000-8000-00000000000a', 'Thomas', 'Neumann', 'thomas.neumann@example.test', NULL, 'Hafenweg 12', '20457', 'Hamburg', NULL, now(), now()),
  ('c0000000-0000-4000-8000-000000000003', 'a0000000-0000-4000-8000-00000000000a', 'Sophie', 'Wagner', 'sophie.wagner@example.test', '0160 9876543', NULL, NULL, 'München', 'Familienshooting', now(), now()),
  ('c0000000-0000-4000-8000-000000000004', 'a0000000-0000-4000-8000-00000000000a', 'Lukas', 'Schmidt', 'lukas.schmidt@example.test', NULL, NULL, NULL, 'Berlin', NULL, now(), now()),
  ('c0000000-0000-4000-8000-000000000005', 'a0000000-0000-4000-8000-00000000000a', 'Marie', 'Hoffmann', 'marie.hoffmann@example.test', '030 555123', 'Schillerstraße 7', '04109', 'Leipzig', NULL, now(), now()),
  ('c0000000-0000-4000-8000-000000000006', 'a0000000-0000-4000-8000-00000000000a', 'Jan', 'Koch', 'jan.koch@example.test', NULL, NULL, NULL, 'Köln', 'Bewerbungsfotos', now(), now()),
  ('c0000000-0000-4000-8000-000000000007', 'a0000000-0000-4000-8000-00000000000a', 'Lea', 'Richter', 'lea.richter@example.test', NULL, NULL, NULL, 'Dresden', NULL, now(), now()),
  ('c0000000-0000-4000-8000-000000000008', 'a0000000-0000-4000-8000-00000000000a', 'Paul', 'Klein', 'paul.klein@example.test', '0151 2223334', NULL, NULL, 'Berlin', NULL, now(), now()),
  ('c0000000-0000-4000-8000-000000000009', 'a0000000-0000-4000-8000-00000000000a', 'Emma', 'Wolf', 'emma.wolf@example.test', NULL, NULL, NULL, 'Bremen', NULL, now(), now()),
  ('c0000000-0000-4000-8000-00000000000a', 'a0000000-0000-4000-8000-00000000000a', 'Felix', 'Schröder', 'felix.schroeder@example.test', NULL, NULL, NULL, 'Hannover', 'Babybauch', now(), now()),
  ('c0000000-0000-4000-8000-00000000000b', 'a0000000-0000-4000-8000-00000000000a', 'Hanna', 'Zimmermann', 'hanna.zimmermann@example.test', NULL, NULL, NULL, 'Berlin', NULL, now(), now()),
  ('c0000000-0000-4000-8000-00000000000c', 'a0000000-0000-4000-8000-00000000000a', 'Ben', 'Braun', 'ben.braun@example.test', NULL, NULL, NULL, 'Potsdam', NULL, now(), now()),
  ('c0000000-0000-4000-8000-0000000000b1', 'b0000000-0000-4000-8000-00000000000b', 'Karin', 'Vogel', 'karin.vogel@example.test', NULL, NULL, NULL, 'Stuttgart', NULL, now(), now()),
  ('c0000000-0000-4000-8000-0000000000b2', 'b0000000-0000-4000-8000-00000000000b', 'Oliver', 'Fuchs', 'oliver.fuchs@example.test', NULL, NULL, NULL, 'Freiburg', NULL, now(), now())
ON CONFLICT DO NOTHING;
