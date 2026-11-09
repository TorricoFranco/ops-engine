-- Una decisión puede terminar en un tercer lugar: pedirle más información al cliente.
--
-- Es distinto de mandar a revisión. Un ticket que no se entiende no necesita que una persona
-- lo lea para descubrir que no dice nada: necesita que le pidan detalles. Separarlos deja la
-- bandeja de revisión para lo que de verdad es dudoso.

ALTER TABLE decision_records DROP CONSTRAINT chk_decision_outcome;

ALTER TABLE decision_records
    ADD CONSTRAINT chk_decision_outcome
    CHECK (outcome IN ('ASSIGNED', 'SENT_TO_REVIEW', 'NEEDS_MORE_INFORMATION'));
