-- Cambios de medio de pago y datos fiscales son de cobros y facturación, no de cuenta.
-- ACCOUNT_MANAGEMENT queda con lo comercial. El modelo lee estas descripciones como criterio,
-- así que tienen que coincidir con el criterio con el que se etiquetan los casos.

UPDATE skills
   SET description = 'Cobros, facturas, notas de crédito, reembolsos, medios de pago y datos fiscales.'
 WHERE code = 'BILLING';

UPDATE skills
   SET description = 'Consultas comerciales: planes, precios del plan, contratación, altas y bajas de servicios.'
 WHERE code = 'ACCOUNT_MANAGEMENT';
