-- Fase 5: auditoría de decisiones y resolución de revisiones.

-- Una fila por decisión de ruteo, se haya asignado el ticket o haya ido a revisión.
--
-- No se copia el texto del ticket: está en `tickets` y duplicarlo multiplicaría los datos
-- personales sin necesidad (RNF-04). Lo que se guarda es lo que hace falta para reconstruir
-- la decisión: qué devolvió el proveedor, con qué reglas se evaluó y qué se hizo.
CREATE TABLE decision_records (
    id                  UUID PRIMARY KEY,
    ticket_id           UUID         NOT NULL REFERENCES tickets (id),
    provider            VARCHAR(50)  NOT NULL,
    policy_version      VARCHAR(50)  NOT NULL,

    -- Nulos cuando el proveedor no respondió: no hubo juicio que registrar.
    suggested_skill     VARCHAR(50),
    skill_confidence    DOUBLE PRECISION,
    skill_recognized    DOUBLE PRECISION,
    urgency             DOUBLE PRECISION,
    urgency_levels      INTEGER,
    multiple_customers  DOUBLE PRECISION,
    service_unavailable DOUBLE PRECISION,

    outcome             VARCHAR(30)  NOT NULL,
    assigned_employee_id UUID        REFERENCES employees (id),
    review_reason       VARCHAR(50),
    rationale           TEXT         NOT NULL,
    latency_ms          BIGINT       NOT NULL,
    decided_at          TIMESTAMPTZ  NOT NULL,

    CONSTRAINT chk_decision_outcome CHECK (outcome IN ('ASSIGNED', 'SENT_TO_REVIEW'))
);

CREATE INDEX idx_decision_records_ticket ON decision_records (ticket_id, decided_at DESC);
CREATE INDEX idx_decision_records_decided_at ON decision_records (decided_at);

-- Un candidato evaluado. En tabla aparte y no como JSON, para poder contar después con qué
-- frecuencia descarta cada motivo.
CREATE TABLE decision_candidates (
    decision_record_id UUID        NOT NULL REFERENCES decision_records (id) ON DELETE CASCADE,
    employee_id        UUID        NOT NULL,
    employee_name      VARCHAR(150) NOT NULL,
    eligible           BOOLEAN     NOT NULL,
    rejection_reason   VARCHAR(50),
    open_tickets       INTEGER     NOT NULL,
    max_open_tickets   INTEGER     NOT NULL,
    PRIMARY KEY (decision_record_id, employee_id),
    CONSTRAINT chk_candidate_reason CHECK (
        (eligible AND rejection_reason IS NULL) OR (NOT eligible AND rejection_reason IS NOT NULL))
);

CREATE INDEX idx_decision_candidates_reason ON decision_candidates (rejection_reason);

-- Qué hizo la persona con un ticket que cayó en revisión.
--
-- `agreed_with_suggestion` es nulo cuando no hubo sugerencia que evaluar, que es el caso en
-- que el proveedor no respondió. Distinguirlo de un desacuerdo importa: mezclarlos ensuciaría
-- la tasa de override, que es la métrica con la que se compara al modelo contra las personas.
CREATE TABLE review_resolutions (
    id                     UUID PRIMARY KEY,
    ticket_id              UUID         NOT NULL REFERENCES tickets (id),
    decision_record_id     UUID         REFERENCES decision_records (id),
    resolved_by            VARCHAR(100) NOT NULL,
    resolution             VARCHAR(30)  NOT NULL,
    assigned_employee_id   UUID         REFERENCES employees (id),
    agreed_with_suggestion BOOLEAN,
    disagreement_reason    TEXT,
    resolved_at            TIMESTAMPTZ  NOT NULL,

    CONSTRAINT chk_review_resolution CHECK (resolution IN ('ASSIGNED', 'CLOSED')),
    CONSTRAINT chk_review_assignee CHECK (
        (resolution = 'ASSIGNED' AND assigned_employee_id IS NOT NULL)
        OR (resolution = 'CLOSED' AND assigned_employee_id IS NULL)),
    -- Un desacuerdo sin motivo no sirve para nada: es justo el dato que se quiere medir.
    CONSTRAINT chk_disagreement_reason CHECK (
        agreed_with_suggestion IS NOT FALSE OR disagreement_reason IS NOT NULL)
);

CREATE INDEX idx_review_resolutions_ticket ON review_resolutions (ticket_id);
CREATE INDEX idx_review_resolutions_resolved_at ON review_resolutions (resolved_at);
