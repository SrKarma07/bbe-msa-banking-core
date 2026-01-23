CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

DROP TABLE IF EXISTS movement CASCADE;
DROP TABLE IF EXISTS account CASCADE;
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

CREATE TABLE IF NOT EXISTS customer (
    id        UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    person_id BIGINT NOT NULL REFERENCES person(person_id),
    password  VARCHAR(255) NOT NULL,
    "state"   BOOLEAN      NOT NULL
);

CREATE TABLE IF NOT EXISTS account (
    "number"      VARCHAR(64) PRIMARY KEY,
    "type"        VARCHAR(20)  NOT NULL,
    balance       NUMERIC(19,2) NOT NULL DEFAULT 0,
    "state"       BOOLEAN       NOT NULL,
    customer_id   UUID          NOT NULL REFERENCES customer(id),
    CONSTRAINT chk_account_balance_non_negative CHECK (balance >= 0)
);

CREATE TABLE IF NOT EXISTS movement (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    account_number  VARCHAR(64) NOT NULL REFERENCES account(number),
    "date"          DATE        NOT NULL,
    "type"          VARCHAR(20) NOT NULL,
    "value"         NUMERIC(19,2) NOT NULL,
    balance         NUMERIC(19,2) NOT NULL,
    detail          VARCHAR(255),
    idempotency_key VARCHAR(128),
    CONSTRAINT chk_movement_value_positive CHECK ("value" > 0),
    CONSTRAINT chk_movement_type_valid CHECK ("type" IN ('Deposito', 'Retiro'))
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_movement_idempotency_key
    ON movement (idempotency_key)
    WHERE idempotency_key IS NOT NULL;

-- Seed mínimo
INSERT INTO person ("name", gender, age, identification, address, phone_number)
VALUES ('Ana Maria Torres','F',29,'EC-1102457890','Quito','+593-98-111-2233');

INSERT INTO customer (id, person_id, password, "state")
VALUES ('ce069cbb-fd30-4f39-8ca6-903d0217d951', 1, 'seed-hash', true);

INSERT INTO account ("number","type",balance,"state",customer_id)
VALUES ('ACC-100001','Ahorro',1500.00,true,'ce069cbb-fd30-4f39-8ca6-903d0217d951');
