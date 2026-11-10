-- Nombre corto de cada área: el que ve una persona en la consola. La descripción sigue siendo
-- la que lee el modelo. Agregar un área nueva es una fila con su código, su descripción y su
-- nombre; ninguna lista de áreas vive en el código ni en el frontend.
ALTER TABLE skills ADD COLUMN display_name VARCHAR(60);

UPDATE skills SET display_name = 'Accesos e identidad' WHERE code = 'ACCESS_IDENTITY';
UPDATE skills SET display_name = 'Facturación'         WHERE code = 'BILLING';
UPDATE skills SET display_name = 'Integraciones'       WHERE code = 'INTEGRATIONS';
UPDATE skills SET display_name = 'Infraestructura'     WHERE code = 'INFRASTRUCTURE';
UPDATE skills SET display_name = 'Gestión de cuentas'  WHERE code = 'ACCOUNT_MANAGEMENT';

-- Un área que nadie nombró muestra su código: se nota en la pantalla y se corrige con una fila.
UPDATE skills SET display_name = code WHERE display_name IS NULL;

ALTER TABLE skills ALTER COLUMN display_name SET NOT NULL;
