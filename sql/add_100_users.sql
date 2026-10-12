SET AUTOCOMMIT FALSE;

INSERT INTO users (
    version, first_name, last_name, email, password, gender, birth_date,
    enabled, verification_pending, preferred_locale, status,
    status_manually_set, last_activity
)
SELECT
    0, t.first_name, t.last_name,
    'user' || (t.nr + batch.x * 20) || '@example.test',
    '$2a$10$O7FdhSaxWB6XOlmel.f.tOaD1UZBr5XQ9SUxm0vC5Vs6drG9hxKGC',
    t.gender, t.birth_date,
    TRUE, FALSE, 'de', 'OFFLINE', FALSE, CURRENT_TIMESTAMP
FROM (VALUES
    ( 1, 'Lena',   'Bauer',     'FEMALE', DATE '1994-03-12'),
    ( 2, 'Jonas',  'Fischer',   'MALE',   DATE '1988-07-24'),
    ( 3, 'Alex',   'Weber',     'OTHER',  DATE '1997-11-05'),
    ( 4, 'Mia',    'Schneider', 'FEMALE', DATE '2000-01-19'),
    ( 5, 'Felix',  'Wagner',    'MALE',   DATE '1982-09-08'),
    ( 6, 'Sara',   'Hoffmann',  'FEMALE', DATE '1991-05-30'),
    ( 7, 'Noah',   'Becker',    'MALE',   DATE '1999-12-14'),
    ( 8, 'Kim',    'Schulz',    'OTHER',  DATE '1995-08-21'),
    ( 9, 'Emma',   'Richter',   'FEMALE', DATE '1986-02-17'),
    (10, 'Paul',   'Klein',     'MALE',   DATE '1993-06-03'),
    (11, 'Nora',   'Wolf',      'FEMALE', DATE '2002-10-09'),
    (12, 'Leon',   'Neumann',   'MALE',   DATE '1980-04-26'),
    (13, 'Robin',  'Schwarz',   'OTHER',  DATE '1996-09-15'),
    (14, 'Clara',  'Zimmer',    'FEMALE', DATE '1989-11-28'),
    (15, 'Ben',    'Braun',     'MALE',   DATE '1998-07-06'),
    (16, 'Julia',  'Hartmann',  'FEMALE', DATE '1984-12-02'),
    (17, 'Tim',    'Lange',     'MALE',   DATE '2001-03-23'),
    (18, 'Sam',    'Werner',    'OTHER',  DATE '1992-01-11'),
    (19, 'Hannah', 'Koch',      'FEMALE', DATE '1987-05-18'),
    (20, 'David',  'Krueger',   'MALE',   DATE '1990-08-04')
) AS t(nr, first_name, last_name, gender, birth_date)
CROSS JOIN SYSTEM_RANGE(0, 4) batch
WHERE NOT EXISTS (
    SELECT 1 FROM users u
    WHERE u.email = 'user' || (t.nr + batch.x * 20) || '@example.test'
)
AND EXISTS (
    SELECT 1 FROM roles WHERE name = 'ROLE_USER'
);

INSERT INTO users_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN SYSTEM_RANGE(1, 100) n
    ON u.email = 'user' || n.x || '@example.test'
CROSS JOIN roles r
WHERE r.name = 'ROLE_USER'
AND NOT EXISTS (
    SELECT 1 FROM users_roles ur
    WHERE ur.user_id = u.id AND ur.role_id = r.id
);

COMMIT;
SET AUTOCOMMIT TRUE;

-- Kontrolle: Erwartet werden 100 Zeilen.
SELECT u.email, u.first_name, u.last_name, u.birth_date,
       u.gender, u.enabled, r.name AS role
FROM users u
JOIN SYSTEM_RANGE(1, 100) n
    ON u.email = 'user' || n.x || '@example.test'
JOIN users_roles ur ON ur.user_id = u.id
JOIN roles r ON r.id = ur.role_id AND r.name = 'ROLE_USER'
ORDER BY n.x;