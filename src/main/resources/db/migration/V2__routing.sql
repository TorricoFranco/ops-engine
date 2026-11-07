-- Fase 4: lo que el ruteo necesita además de lo que ya existía.

-- Catálogo de competencias. La descripción se le muestra al modelo como criterio del choice,
-- así que explica el área en lugar de repetir el código.
CREATE TABLE skills (
    code        VARCHAR(50) PRIMARY KEY,
    description VARCHAR(500) NOT NULL
);

INSERT INTO skills (code, description) VALUES
    ('ACCESS_IDENTITY', 'Inicio de sesión, contraseñas, permisos y accesos de usuarios.'),
    ('BILLING',         'Cobros, facturas, reembolsos y problemas con los pagos.'),
    ('INTEGRATIONS',    'Sincronización, webhooks y conexiones con sistemas externos.'),
    ('INFRASTRUCTURE',  'Caídas del servicio, lentitud, errores del servidor y capacidad.');

-- Habilitación del empleado. Un incidente crítico solo puede ir a quien la tiene:
-- es una restricción de la empresa, no algo que el modelo pueda decidir.
ALTER TABLE employees
    ADD COLUMN clearance VARCHAR(20) NOT NULL DEFAULT 'STANDARD';

ALTER TABLE employees
    ADD CONSTRAINT chk_employees_clearance CHECK (clearance IN ('STANDARD', 'CRITICAL'));

-- Outbox: el evento se escribe en la misma transacción que el cambio que lo produce,
-- así no puede quedar una asignación sin su evento ni un evento sin su asignación.
CREATE TABLE outbox_events (
    id           UUID PRIMARY KEY,
    type         VARCHAR(100) NOT NULL,
    aggregate_id UUID         NOT NULL,
    payload      TEXT         NOT NULL,
    occurred_at  TIMESTAMPTZ  NOT NULL,
    published_at TIMESTAMPTZ,
    attempts     INTEGER      NOT NULL DEFAULT 0,
    last_error   TEXT
);

-- Índice parcial: el despachador solo consulta los pendientes, que son pocos.
CREATE INDEX idx_outbox_pending
    ON outbox_events (occurred_at)
    WHERE published_at IS NULL;
