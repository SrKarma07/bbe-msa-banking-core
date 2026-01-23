CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

DROP TABLE IF EXISTS customer CASCADE;
DROP TABLE IF EXISTS person CASCADE;

CREATE TABLE IF NOT EXISTS person (
    person_id      BIGSERIAL PRIMARY KEY,
    "name"         VARCHAR(100) NOT NULL,
    gender         VARCHAR(1)   NOT NULL,
    age            INT          NOT NULL,
    identification VARCHAR(50)  NOT NULL UNIQUE,
    address        VARCHAR(200) NOT NULL,
    phone_number   VARCHAR(50)  NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_person_identification ON person(identification);

CREATE TABLE IF NOT EXISTS customer (
    id        UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    person_id BIGINT NOT NULL REFERENCES person(person_id),
    password  VARCHAR(255) NOT NULL,
    "state"   BOOLEAN      NOT NULL
);

CREATE INDEX IF NOT EXISTS ix_customer_person_id ON customer(person_id);

-- Seed mínimo (para probar conflicto por identificación)
INSERT INTO person ("name", gender, age, identification, address, phone_number)
VALUES ('Ana Maria Torres','F',29,'EC-1102457890','Quito','+593-98-111-2233');

INSERT INTO customer (id, person_id, password, "state")
VALUES ('ce069cbb-fd30-4f39-8ca6-903d0217d951', 1, 'seed-hash', true);
