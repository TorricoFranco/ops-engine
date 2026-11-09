-- Un área nueva para consultas comerciales y de cuenta.
--
-- Hasta ahora las cuatro áreas eran técnicas, así que una consulta sobre planes o un cambio
-- de medio de pago no correspondía a ninguna y terminaba en revisión humana. Eso está bien
-- para un caso raro y mal para una categoría entera: con volumen, una persona se pasaría el
-- día reenviando lo mismo al mismo lugar. La revisión tiene que ser para lo que el sistema
-- no sabe, no para lo que sabe pero no tiene a dónde mandar.
--
-- Agregar un equipo es una fila: el catálogo es data y las reglas lo leen de acá.

INSERT INTO skills (code, description) VALUES
    ('ACCOUNT_MANAGEMENT',
     'Planes, precios, contratación, cambios de medio de pago y datos de facturación.');
