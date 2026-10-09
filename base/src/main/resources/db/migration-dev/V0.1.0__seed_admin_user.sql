INSERT INTO users (full_name, email, password, principal_role, roles, active, email_verified_at, created_at)
VALUES ('${seedAdminName}', '${seedAdminEmail}', '${seedAdminPasswordHash}',
        'ROLE_ADMIN', '{ROLE_ADMIN}', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
