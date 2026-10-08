--liquibase formatted sql

-- Event-Publication-Registry von Spring Modulith (JDBC, Schema v2 für PostgreSQL).
-- Quelle: spring-modulith-events-jdbc, org/springframework/modulith/events/jdbc/schemas/v2/schema-postgresql.sql

--changeset photoffice:event-publication
CREATE TABLE event_publication
(
  id                     UUID NOT NULL,
  listener_id            TEXT NOT NULL,
  event_type             TEXT NOT NULL,
  serialized_event       TEXT NOT NULL,
  publication_date       TIMESTAMP WITH TIME ZONE NOT NULL,
  completion_date        TIMESTAMP WITH TIME ZONE,
  status                 TEXT,
  completion_attempts    INT,
  last_resubmission_date TIMESTAMP WITH TIME ZONE,
  PRIMARY KEY (id)
);
CREATE INDEX event_publication_serialized_event_hash_idx ON event_publication USING hash(serialized_event);
CREATE INDEX event_publication_by_completion_date_idx ON event_publication (completion_date);
