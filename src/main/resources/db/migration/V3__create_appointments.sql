CREATE TABLE appointments
(
    id         UUID        NOT NULL DEFAULT gen_random_uuid(),
    tenant_id  UUID        NOT NULL,
    patient_id UUID        NOT NULL,
    dentist_id UUID        NOT NULL,
    start_time TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    end_time   TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    status     VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_appointments PRIMARY KEY (id),
    CONSTRAINT fk_appointments_tenant  FOREIGN KEY (tenant_id)  REFERENCES tenants (id),
    CONSTRAINT fk_appointments_patient FOREIGN KEY (patient_id) REFERENCES patients (id),
    CONSTRAINT fk_appointments_dentist FOREIGN KEY (dentist_id) REFERENCES users (id)
);

CREATE INDEX idx_appointments_tenant_id     ON appointments (tenant_id);
CREATE INDEX idx_appointments_dentist_time  ON appointments (dentist_id, start_time);
