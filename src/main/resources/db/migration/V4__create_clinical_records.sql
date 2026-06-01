CREATE TABLE clinical_records
(
    id                 UUID  NOT NULL DEFAULT gen_random_uuid(),
    tenant_id          UUID  NOT NULL,
    patient_id         UUID  NOT NULL,
    appointment_id     UUID,
    odontogram_data    JSONB NOT NULL DEFAULT '{}'::jsonb,
    clinical_notes     TEXT,
    created_by_user_id UUID  NOT NULL,
    created_at         TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_clinical_records PRIMARY KEY (id),
    CONSTRAINT fk_clinical_records_tenant      FOREIGN KEY (tenant_id)          REFERENCES tenants (id),
    CONSTRAINT fk_clinical_records_patient     FOREIGN KEY (patient_id)         REFERENCES patients (id),
    CONSTRAINT fk_clinical_records_appointment FOREIGN KEY (appointment_id)     REFERENCES appointments (id),
    CONSTRAINT fk_clinical_records_user        FOREIGN KEY (created_by_user_id) REFERENCES users (id)
);

CREATE INDEX idx_clinical_records_tenant_id  ON clinical_records (tenant_id);
CREATE INDEX idx_clinical_records_patient_id ON clinical_records (patient_id);
