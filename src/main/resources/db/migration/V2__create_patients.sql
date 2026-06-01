CREATE TABLE patients
(
    id             UUID         NOT NULL DEFAULT gen_random_uuid(),
    tenant_id      UUID         NOT NULL,
    full_name      VARCHAR(255) NOT NULL,
    date_of_birth  DATE,
    medical_alerts TEXT,
    created_at     TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_patients PRIMARY KEY (id),
    CONSTRAINT fk_patients_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id)
);

CREATE INDEX idx_patients_tenant_id ON patients (tenant_id);
