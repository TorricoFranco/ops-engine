-- Vuelven las descripciones de V2 y V4 para BILLING y ACCOUNT_MANAGEMENT. La propuesta de V8
-- (datos fiscales y medios de pago en BILLING) se evaluó: bajó los aciertos en el set sintético,
-- cuyas etiquetas ponen esos temas en ACCOUNT_MANAGEMENT, y no mejoró el set de validación.
-- Ver docs/evaluation/README.md, sección "Decisiones de catálogo".

UPDATE skills
   SET description = 'Cobros, facturas, reembolsos y problemas con los pagos.'
 WHERE code = 'BILLING';

UPDATE skills
   SET description = 'Planes, precios, contratación, cambios de medio de pago y datos de facturación.'
 WHERE code = 'ACCOUNT_MANAGEMENT';
