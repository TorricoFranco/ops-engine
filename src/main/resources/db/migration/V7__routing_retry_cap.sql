-- Cuántas veces falló el ruteo de un ticket. Al llegar al tope, el ticket pasa a revisión
-- humana en lugar de reintentarse para siempre (ADR-016).
ALTER TABLE tickets
    ADD COLUMN routing_attempts INTEGER NOT NULL DEFAULT 0,
    ADD CONSTRAINT chk_tickets_routing_attempts CHECK (routing_attempts >= 0);
