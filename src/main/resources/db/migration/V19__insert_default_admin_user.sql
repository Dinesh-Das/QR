-- V19__insert_default_admin_user.sql
-- Create a default admin user for initial login

-- Insert default admin user
INSERT INTO QRMFG_USERS (
    id, 
    username, 
    email, 
    password, 
    enabled, 
    status,
    email_verified,
    phone_verified,
    created_at, 
    updated_at,
    assigned_plants,
    primary_plant
) VALUES (
    QRMFG_USERS_SEQ.NEXTVAL,
    'admin',
    'admin@qrmfg.com',
    '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.',  -- BCrypt hash for 'password'
    1,
    'ACTIVE',
    1,
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    '["1102"]',
    '1102'
);

-- Create admin role if it doesn't exist
INSERT INTO QRMFG_ROLES (id, name, description, enabled, created_at, updated_at, type) 
SELECT QRMFG_ROLES_SEQ.NEXTVAL, 'ADMIN', 'Administrator role with full access', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SYSTEM'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM QRMFG_ROLES WHERE name = 'ADMIN');

-- Assign admin role to admin user
INSERT INTO QRMFG_USER_ROLES (user_id, role_id)
SELECT u.id, r.id
FROM QRMFG_USERS u, QRMFG_ROLES r
WHERE u.username = 'admin' 
  AND r.name = 'ADMIN'
  AND NOT EXISTS (
    SELECT 1 FROM QRMFG_USER_ROLES ur 
    WHERE ur.user_id = u.id AND ur.role_id = r.id
  );

COMMIT;