CREATE TABLE treatment_plans
(
    id                 UUID        NOT NULL DEFAULT gen_random_uuid(),
    tenant_id          UUID        NOT NULL,
    patient_id         UUID        NOT NULL,
    title              VARCHAR(255) NOT NULL,
    status             VARCHAR(20) NOT NULL,
    created_by_user_id UUID        NOT NULL,
    created_at         TIMESTAMP WITHOUT TIME ZONE,
    updated_at         TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_treatment_plans PRIMARY KEY (id),
    CONSTRAINT fk_treatment_plans_tenant  FOREIGN KEY (tenant_id)          REFERENCES tenants (id),
    CONSTRAINT fk_treatment_plans_patient FOREIGN KEY (patient_id)         REFERENCES patients (id),
    CONSTRAINT fk_treatment_plans_user    FOREIGN KEY (created_by_user_id) REFERENCES users (id)
);

CREATE TABLE treatment_items
(
    id                UUID           NOT NULL DEFAULT gen_random_uuid(),
    treatment_plan_id UUID           NOT NULL,
    description       VARCHAR(255)   NOT NULL,
    tooth             VARCHAR(5),
    amount            NUMERIC(12, 2) NOT NULL,
    status            VARCHAR(20)    NOT NULL,
    charge_id         UUID,
    created_at        TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_treatment_items PRIMARY KEY (id),
    CONSTRAINT fk_treatment_items_plan   FOREIGN KEY (treatment_plan_id) REFERENCES treatment_plans (id),
    CONSTRAINT fk_treatment_items_charge FOREIGN KEY (charge_id)         REFERENCES charges (id)
);

CREATE INDEX idx_treatment_plans_tenant_id  ON treatment_plans (tenant_id);
CREATE INDEX idx_treatment_plans_patient_id ON treatment_plans (patient_id);
CREATE INDEX idx_treatment_items_plan_id    ON treatment_items (treatment_plan_id);
