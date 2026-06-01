CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE tenants
(
    id          UUID         NOT NULL DEFAULT gen_random_uuid(),
    clinic_name VARCHAR(255) NOT NULL,
    document    VARCHAR(20)  NOT NULL,
    created_at  TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_tenants PRIMARY KEY (id),
    CONSTRAINT uq_tenants_document UNIQUE (document)
);

CREATE TABLE users
(
    id            UUID         NOT NULL DEFAULT gen_random_uuid(),
    tenant_id     UUID         NOT NULL,
    full_name     VARCHAR(255) NOT NULL,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    created_at    TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_tenant_email UNIQUE (tenant_id, email),
    CONSTRAINT fk_users_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id)
);

CREATE INDEX idx_users_tenant_id ON users (tenant_id);
