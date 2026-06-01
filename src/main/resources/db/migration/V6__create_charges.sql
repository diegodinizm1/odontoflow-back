CREATE TABLE charges
(
    id             UUID           NOT NULL DEFAULT gen_random_uuid(),
    tenant_id      UUID           NOT NULL,
    patient_id     UUID           NOT NULL,
    appointment_id UUID,
    description    VARCHAR(255)   NOT NULL,
    amount         NUMERIC(12, 2) NOT NULL,
    status         VARCHAR(20)    NOT NULL,
    due_date       DATE,
    paid_at        TIMESTAMP WITHOUT TIME ZONE,
    created_at     TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_charges PRIMARY KEY (id),
    CONSTRAINT fk_charges_tenant      FOREIGN KEY (tenant_id)      REFERENCES tenants (id),
    CONSTRAINT fk_charges_patient     FOREIGN KEY (patient_id)     REFERENCES patients (id),
    CONSTRAINT fk_charges_appointment FOREIGN KEY (appointment_id) REFERENCES appointments (id)
);

CREATE INDEX idx_charges_tenant_id  ON charges (tenant_id);
CREATE INDEX idx_charges_patient_id ON charges (patient_id);
