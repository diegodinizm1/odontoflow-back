CREATE TABLE patient_files
(
    id                  UUID         NOT NULL DEFAULT gen_random_uuid(),
    tenant_id           UUID         NOT NULL,
    patient_id          UUID         NOT NULL,
    object_key          VARCHAR(512) NOT NULL,
    file_name           VARCHAR(255) NOT NULL,
    content_type        VARCHAR(120),
    uploaded_by_user_id UUID         NOT NULL,
    created_at          TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_patient_files PRIMARY KEY (id),
    CONSTRAINT fk_patient_files_tenant  FOREIGN KEY (tenant_id)           REFERENCES tenants (id),
    CONSTRAINT fk_patient_files_patient FOREIGN KEY (patient_id)          REFERENCES patients (id),
    CONSTRAINT fk_patient_files_user    FOREIGN KEY (uploaded_by_user_id) REFERENCES users (id)
);

CREATE INDEX idx_patient_files_tenant_id  ON patient_files (tenant_id);
CREATE INDEX idx_patient_files_patient_id ON patient_files (patient_id);
