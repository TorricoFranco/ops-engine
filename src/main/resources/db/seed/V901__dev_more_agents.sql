-- Más agentes de desarrollo, para que el ruteo tenga a quién descartar.
--
-- Con un agente por área, la lista de candidatos de cada decisión tiene un solo nombre y no
-- se ve lo que hace interesante al motor: por qué quedó afuera cada uno. Acá hay, por área,
-- alguien elegible y alguien que no, por un motivo distinto en cada caso.

INSERT INTO employees (id, name, team_id, availability, max_open_tickets, open_tickets, clearance) VALUES
    -- Sin habilitación: queda afuera de los incidentes críticos.
    ('aaaaaaaa-0000-0000-0000-000000000006', 'Federico Ruiz',
     '44444444-4444-4444-4444-444444444444', 'AVAILABLE', 4, 0, 'STANDARD'),
    -- Lleno: ya está en su tope.
    ('aaaaaaaa-0000-0000-0000-000000000007', 'Gabriela Soto',
     '22222222-2222-2222-2222-222222222222', 'AVAILABLE', 2, 2, 'STANDARD'),
    -- De licencia.
    ('aaaaaaaa-0000-0000-0000-000000000008', 'Hernán Castro',
     '11111111-1111-1111-1111-111111111111', 'UNAVAILABLE', 5, 0, 'STANDARD'),
    -- Elegible, para que haya competencia real por el ticket.
    ('aaaaaaaa-0000-0000-0000-000000000009', 'Irina Molina',
     '33333333-3333-3333-3333-333333333333', 'AVAILABLE', 6, 1, 'CRITICAL'),
    ('aaaaaaaa-0000-0000-0000-00000000000a', 'Julián Paz',
     '22222222-2222-2222-2222-222222222222', 'AVAILABLE', 5, 3, 'STANDARD');

INSERT INTO employee_skills (employee_id, skill_code) VALUES
    ('aaaaaaaa-0000-0000-0000-000000000006', 'INFRASTRUCTURE'),
    ('aaaaaaaa-0000-0000-0000-000000000007', 'BILLING'),
    ('aaaaaaaa-0000-0000-0000-000000000008', 'ACCESS_IDENTITY'),
    ('aaaaaaaa-0000-0000-0000-000000000009', 'INTEGRATIONS'),
    ('aaaaaaaa-0000-0000-0000-000000000009', 'INFRASTRUCTURE'),
    ('aaaaaaaa-0000-0000-0000-00000000000a', 'BILLING'),
    ('aaaaaaaa-0000-0000-0000-00000000000a', 'ACCOUNT_MANAGEMENT');
