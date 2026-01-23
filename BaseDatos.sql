CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ======================
-- PERSON
-- ======================
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

-- ======================
-- CUSTOMER
-- ======================
DROP TABLE IF EXISTS customer CASCADE;

CREATE TABLE IF NOT EXISTS customer (
    id        UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    person_id BIGINT NOT NULL REFERENCES person(person_id),
    password  VARCHAR(255) NOT NULL,
    "state"   BOOLEAN      NOT NULL
);

CREATE INDEX IF NOT EXISTS ix_customer_person_id ON customer(person_id);

-- ======================
-- ACCOUNT
-- ======================
DROP TABLE IF EXISTS account CASCADE;

CREATE TABLE IF NOT EXISTS account (
    "number"      VARCHAR(64) PRIMARY KEY,
    "type"        VARCHAR(20)  NOT NULL,
    balance       NUMERIC(19,2) NOT NULL DEFAULT 0,
    "state"       BOOLEAN       NOT NULL,
    customer_id   UUID          NOT NULL REFERENCES customer(id),
    CONSTRAINT chk_account_balance_non_negative CHECK (balance >= 0)
);

CREATE INDEX IF NOT EXISTS ix_account_customer_id ON account(customer_id);

-- ======================
-- MOVEMENT
-- ======================
DROP TABLE IF EXISTS movement CASCADE;

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

CREATE INDEX IF NOT EXISTS ix_movement_account_number ON movement(account_number);
CREATE INDEX IF NOT EXISTS ix_movement_account_date ON movement(account_number, "date");

CREATE UNIQUE INDEX IF NOT EXISTS ux_movement_idempotency_key
    ON movement (idempotency_key)
    WHERE idempotency_key IS NOT NULL;

-- ======================
-- SEED DATA (production-like)
-- ======================

-- PERSON
INSERT INTO person ("name", gender, age, identification, address, phone_number) VALUES
('Ana Maria Torres',      'F', 29, 'EC-1102457890', 'Quito, Pichincha - La Carolina',     '+593-98-111-2233'),
('Carlos Andres Molina',  'M', 34, 'EC-1712456789', 'Guayaquil, Guayas - Urdesa',         '+593-99-222-3344'),
('Diana Paola Rivera',    'F', 41, 'EC-0912345678', 'Cuenca, Azuay - El Vergel',          '+593-98-333-4455'),
('Jorge Luis Herrera',    'M', 27, 'EC-1809876543', 'Ambato, Tungurahua - Ficoa',         '+593-99-444-5566'),
('Maria Fernanda Rojas',  'F', 38, 'EC-0102030405', 'Manta, Manabi - Barbasquillo',       '+593-98-555-6677'),
('Pedro Jose Salazar',    'M', 46, 'EC-0607080910', 'Loja, Loja - Centro',                '+593-99-666-7788'),
('Sofia Alejandra Vega',  'F', 25, 'EC-1112131415', 'Ibarra, Imbabura - El Retorno',      '+593-98-777-8899'),
('Ricardo Ivan Paredes',  'M', 31, 'EC-1617181920', 'Santo Domingo - Centro',             '+593-99-888-9900');

-- CUSTOMER
-- Using deterministic UUIDs for predictable testing
INSERT INTO customer (id, person_id, password, "state") VALUES
('ce069cbb-fd30-4f39-8ca6-903d0217d951', 1, '$2a$10$seed.hash.ana',     true),
('7f0c2d65-7a0a-4e78-a0fa-2bb4a6b8d219', 2, '$2a$10$seed.hash.carlos',  true),
('f3d9f1c6-3122-4b6d-9d55-93b2f3c1ab10', 3, '$2a$10$seed.hash.diana',   true),
('2b6f7a58-9f78-4b33-a3d2-8b6c0b4d8f11', 4, '$2a$10$seed.hash.jorge',   true),
('a7d88a62-8e2b-4d72-8d41-9b7b0a2ce9a5', 5, '$2a$10$seed.hash.maria',   true),
('d6d1b2a0-6c4d-4c74-9ed0-0b5f1c2d3e44', 6, '$2a$10$seed.hash.pedro',   true),
('0d1d2e3f-4a5b-4c6d-8e9f-001122334455', 7, '$2a$10$seed.hash.sofia',   true),
('11223344-5566-7788-99aa-bbccddeeff00', 8, '$2a$10$seed.hash.ricardo', true);

-- ACCOUNT
-- Types follow your DB check constraints (not enforced here), but we keep consistent with your domain: 'Ahorro'/'Corriente'
INSERT INTO account ("number", "type", balance, "state", customer_id) VALUES
('ACC-100001', 'Ahorro',     1500.00, true, 'ce069cbb-fd30-4f39-8ca6-903d0217d951'),
('ACC-100002', 'Corriente',   250.00, true, 'ce069cbb-fd30-4f39-8ca6-903d0217d951'),

('ACC-200001', 'Ahorro',     3200.00, true, '7f0c2d65-7a0a-4e78-a0fa-2bb4a6b8d219'),
('ACC-200002', 'Corriente',   890.00, true, '7f0c2d65-7a0a-4e78-a0fa-2bb4a6b8d219'),

('ACC-300001', 'Ahorro',      980.00, true, 'f3d9f1c6-3122-4b6d-9d55-93b2f3c1ab10'),

('ACC-400001', 'Corriente',  1200.00, true, '2b6f7a58-9f78-4b33-a3d2-8b6c0b4d8f11'),

('ACC-500001', 'Ahorro',     5100.00, true, 'a7d88a62-8e2b-4d72-8d41-9b7b0a2ce9a5'),
('ACC-500002', 'Corriente',   600.00, false,'a7d88a62-8e2b-4d72-8d41-9b7b0a2ce9a5'),

('ACC-600001', 'Ahorro',      300.00, true, 'd6d1b2a0-6c4d-4c74-9ed0-0b5f1c2d3e44'),

('ACC-700001', 'Corriente',  1750.00, true, '0d1d2e3f-4a5b-4c6d-8e9f-001122334455'),

('ACC-800001', 'Ahorro',      50.00,  true, '11223344-5566-7788-99aa-bbccddeeff00'),
('ACC-800002', 'Corriente',  400.00,  true, '11223344-5566-7788-99aa-bbccddeeff00');

-- MOVEMENTS
-- IMPORTANT: movement.value is positive; balance is the account balance after applying the movement.
-- We seed multiple accounts with realistic transaction histories.
-- Dates span multiple months for report testing.

-- ACC-100001 (Ana) starting around 2025-11, ending around 2026-01
INSERT INTO movement (account_number, "date", "type", "value", balance, detail, idempotency_key) VALUES
('ACC-100001','2025-11-05','Deposito',  500.00,  500.00,'Initial funding',               'idem-ana-100001-001'),
('ACC-100001','2025-11-20','Deposito', 1200.00, 1700.00,'Salary deposit',                NULL),
('ACC-100001','2025-12-02','Retiro',     200.00, 1500.00,'ATM withdrawal',                NULL),
('ACC-100001','2025-12-18','Deposito',   300.00, 1800.00,'Transfer in',                   NULL),
('ACC-100001','2026-01-07','Retiro',     300.00, 1500.00,'Bills payment',                 NULL);

-- ACC-100002 (Ana)
INSERT INTO movement (account_number, "date", "type", "value", balance, detail, idempotency_key) VALUES
('ACC-100002','2025-12-01','Deposito',   250.00,  250.00,'Initial funding',               'idem-ana-100002-001'),
('ACC-100002','2025-12-15','Retiro',      50.00,  200.00,'Debit card purchase',           NULL),
('ACC-100002','2026-01-10','Deposito',    50.00,  250.00,'Refund',                        NULL);

-- ACC-200001 (Carlos)
INSERT INTO movement (account_number, "date", "type", "value", balance, detail, idempotency_key) VALUES
('ACC-200001','2025-10-10','Deposito',  3000.00, 3000.00,'Initial funding',               'idem-carlos-200001-001'),
('ACC-200001','2025-11-01','Deposito',   500.00, 3500.00,'Business income',               NULL),
('ACC-200001','2025-11-25','Retiro',     200.00, 3300.00,'Cash withdrawal',               NULL),
('ACC-200001','2025-12-10','Retiro',     100.00, 3200.00,'Online purchase',               NULL);

-- ACC-200002 (Carlos)
INSERT INTO movement (account_number, "date", "type", "value", balance, detail, idempotency_key) VALUES
('ACC-200002','2025-11-12','Deposito',   890.00,  890.00,'Initial funding',               'idem-carlos-200002-001'),
('ACC-200002','2025-12-05','Retiro',     120.00,  770.00,'Service payment',               NULL),
('ACC-200002','2026-01-03','Deposito',   120.00,  890.00,'Reimbursement',                 NULL);

-- ACC-300001 (Diana)
INSERT INTO movement (account_number, "date", "type", "value", balance, detail, idempotency_key) VALUES
('ACC-300001','2025-09-20','Deposito',  1000.00, 1000.00,'Initial funding',               'idem-diana-300001-001'),
('ACC-300001','2025-10-08','Retiro',      20.00,  980.00,'Fee adjustment',                NULL);

-- ACC-400001 (Jorge)
INSERT INTO movement (account_number, "date", "type", "value", balance, detail, idempotency_key) VALUES
('ACC-400001','2025-12-03','Deposito',  1200.00, 1200.00,'Initial funding',               'idem-jorge-400001-001'),
('ACC-400001','2026-01-05','Retiro',     250.00,  950.00,'Groceries',                     NULL),
('ACC-400001','2026-01-18','Deposito',   250.00, 1200.00,'Refund',                        NULL);

-- ACC-500001 (Maria)
INSERT INTO movement (account_number, "date", "type", "value", balance, detail, idempotency_key) VALUES
('ACC-500001','2025-08-01','Deposito',  5000.00, 5000.00,'Initial funding',               'idem-maria-500001-001'),
('ACC-500001','2025-12-22','Deposito',   500.00, 5500.00,'Year-end bonus',                NULL),
('ACC-500001','2026-01-20','Retiro',     400.00, 5100.00,'Insurance payment',             NULL);

-- ACC-500002 (Maria) CLOSED account (state=false). Movements exist historically.
INSERT INTO movement (account_number, "date", "type", "value", balance, detail, idempotency_key) VALUES
('ACC-500002','2025-07-10','Deposito',   600.00,  600.00,'Initial funding',               'idem-maria-500002-001');

-- ACC-600001 (Pedro)
INSERT INTO movement (account_number, "date", "type", "value", balance, detail, idempotency_key) VALUES
('ACC-600001','2025-12-28','Deposito',   300.00,  300.00,'Initial funding',               'idem-pedro-600001-001');

-- ACC-700001 (Sofia)
INSERT INTO movement (account_number, "date", "type", "value", balance, detail, idempotency_key) VALUES
('ACC-700001','2025-11-30','Deposito',  1750.00, 1750.00,'Initial funding',               'idem-sofia-700001-001'),
('ACC-700001','2025-12-20','Retiro',     150.00, 1600.00,'Travel expenses',               NULL),
('ACC-700001','2026-01-08','Deposito',   150.00, 1750.00,'Adjustment',                    NULL);

-- ACC-800001 (Ricardo) low balance + multiple small transactions
INSERT INTO movement (account_number, "date", "type", "value", balance, detail, idempotency_key) VALUES
('ACC-800001','2026-01-02','Deposito',    50.00,   50.00,'Initial funding',               'idem-ricardo-800001-001');

-- ACC-800002 (Ricardo)
INSERT INTO movement (account_number, "date", "type", "value", balance, detail, idempotency_key) VALUES
('ACC-800002','2025-12-11','Deposito',   400.00,  400.00,'Initial funding',               'idem-ricardo-800002-001');

-- Ensure account balances match seeded movement latest balances
UPDATE account SET balance = 1500.00 WHERE "number" = 'ACC-100001';
UPDATE account SET balance =  250.00 WHERE "number" = 'ACC-100002';
UPDATE account SET balance = 3200.00 WHERE "number" = 'ACC-200001';
UPDATE account SET balance =  890.00 WHERE "number" = 'ACC-200002';
UPDATE account SET balance =  980.00 WHERE "number" = 'ACC-300001';
UPDATE account SET balance = 1200.00 WHERE "number" = 'ACC-400001';
UPDATE account SET balance = 5100.00 WHERE "number" = 'ACC-500001';
UPDATE account SET balance =  600.00 WHERE "number" = 'ACC-500002';
UPDATE account SET balance =  300.00 WHERE "number" = 'ACC-600001';
UPDATE account SET balance = 1750.00 WHERE "number" = 'ACC-700001';
UPDATE account SET balance =   50.00 WHERE "number" = 'ACC-800001';
UPDATE account SET balance =  400.00 WHERE "number" = 'ACC-800002';
