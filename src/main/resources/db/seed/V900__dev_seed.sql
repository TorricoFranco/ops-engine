-- Datos de desarrollo. Solo se aplican con el perfil `dev`.
-- Los usuarios NO se crean acá: los inserta DevUserSeeder, que hashea la contraseña
-- con el PasswordEncoder de la aplicación en lugar de dejar un hash fijo en el repositorio.

INSERT INTO teams (id, name) VALUES
    ('11111111-1111-1111-1111-111111111111', 'Accesos e identidad'),
    ('22222222-2222-2222-2222-222222222222', 'Facturación'),
    ('33333333-3333-3333-3333-333333333333', 'Integraciones'),
    ('44444444-4444-4444-4444-444444444444', 'Infraestructura'),
    ('55555555-5555-5555-5555-555555555555', 'Cuentas y comercial');

INSERT INTO employees (id, name, team_id, availability, max_open_tickets, open_tickets) VALUES
    ('aaaaaaaa-0000-0000-0000-000000000001', 'Ana Pérez',      '11111111-1111-1111-1111-111111111111', 'AVAILABLE', 5, 0),
    ('aaaaaaaa-0000-0000-0000-000000000002', 'Bruno Giménez',  '22222222-2222-2222-2222-222222222222', 'AVAILABLE', 5, 0),
    ('aaaaaaaa-0000-0000-0000-000000000003', 'Carla Suárez',   '33333333-3333-3333-3333-333333333333', 'AVAILABLE', 4, 0),
    ('aaaaaaaa-0000-0000-0000-000000000004', 'Diego Ramírez',  '44444444-4444-4444-4444-444444444444', 'AVAILABLE', 4, 0),
    ('aaaaaaaa-0000-0000-0000-000000000005', 'Elena Vidal',    '55555555-5555-5555-5555-555555555555', 'AVAILABLE', 6, 0);

-- Diego atiende infraestructura y tiene habilitación para incidentes críticos.
UPDATE employees SET clearance = 'CRITICAL'
 WHERE id = 'aaaaaaaa-0000-0000-0000-000000000004';

INSERT INTO employee_skills (employee_id, skill_code) VALUES
    ('aaaaaaaa-0000-0000-0000-000000000001', 'ACCESS_IDENTITY'),
    ('aaaaaaaa-0000-0000-0000-000000000002', 'BILLING'),
    ('aaaaaaaa-0000-0000-0000-000000000003', 'INTEGRATIONS'),
    ('aaaaaaaa-0000-0000-0000-000000000003', 'BILLING'),
    ('aaaaaaaa-0000-0000-0000-000000000004', 'INFRASTRUCTURE'),
    ('aaaaaaaa-0000-0000-0000-000000000005', 'ACCOUNT_MANAGEMENT');
