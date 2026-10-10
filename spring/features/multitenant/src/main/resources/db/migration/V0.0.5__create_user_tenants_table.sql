-- Vínculo usuário -> tenant (feature multitenant). A tabela dona do tenant (agência, cidade,
-- loja...) é domínio do projeto: crie-a numa migration sua e acrescente a FK de tenant_id.
CREATE TABLE user_tenants (
    user_id BIGINT PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    tenant_id BIGINT NOT NULL CHECK (tenant_id > 0)
);

CREATE INDEX idx_user_tenants_tenant ON user_tenants (tenant_id);
