-- Esquema inicial: equipos, empleados, skills y tickets.
-- Clientes, servicios y reglas de ruteo llegan en la fase 4, con sus propias migraciones.

CREATE TABLE teams (
    id   UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE employees (
    id               UUID PRIMARY KEY,
    name             VARCHAR(150) NOT NULL,
    team_id          UUID         NOT NULL REFERENCES teams (id),
    availability     VARCHAR(20)  NOT NULL,
    max_open_tickets INTEGER      NOT NULL CHECK (max_open_tickets > 0),
    open_tickets     INTEGER      NOT NULL DEFAULT 0 CHECK (open_tickets >= 0),
    version          BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX idx_employees_team ON employees (team_id);

CREATE TABLE employee_skills (
    employee_id UUID        NOT NULL REFERENCES employees (id) ON DELETE CASCADE,
    skill_code  VARCHAR(50) NOT NULL,
    PRIMARY KEY (employee_id, skill_code)
);

CREATE INDEX idx_employee_skills_code ON employee_skills (skill_code);

CREATE TABLE tickets (
    id              UUID PRIMARY KEY,
    title           VARCHAR(200) NOT NULL,
    description     TEXT         NOT NULL,
    impact          VARCHAR(20)  NOT NULL,
    required_skill  VARCHAR(50)  NOT NULL,
    status          VARCHAR(30)  NOT NULL,
    assignee_id     UUID         REFERENCES employees (id),
    review_reason   TEXT,
    created_at      TIMESTAMPTZ  NOT NULL,
    sla_deadline    TIMESTAMPTZ  NOT NULL,
    idempotency_key VARCHAR(100),
    version         BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT chk_tickets_sla_after_creation CHECK (sla_deadline > created_at)
);

-- Dos pedidos con la misma Idempotency-Key no pueden crear dos tickets.
CREATE UNIQUE INDEX uq_tickets_idempotency_key
    ON tickets (idempotency_key)
    WHERE idempotency_key IS NOT NULL;

CREATE INDEX idx_tickets_status ON tickets (status);
CREATE INDEX idx_tickets_sla_deadline ON tickets (sla_deadline);
CREATE INDEX idx_tickets_assignee ON tickets (assignee_id);

CREATE TABLE app_users (
    id            UUID PRIMARY KEY,
    username      VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL
);
