CREATE TABLE subscriptions
(
    id                   UUID        NOT NULL DEFAULT gen_random_uuid(),
    tenant_id            UUID        NOT NULL,
    plan                 VARCHAR(20) NOT NULL,
    status               VARCHAR(20) NOT NULL,
    current_period_end   DATE,
    external_customer_id VARCHAR(255),
    created_at           TIMESTAMP WITHOUT TIME ZONE,
    updated_at           TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_subscriptions PRIMARY KEY (id),
    CONSTRAINT uq_subscriptions_tenant UNIQUE (tenant_id),
    CONSTRAINT fk_subscriptions_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id)
);

CREATE TABLE invoices
(
    id                  UUID           NOT NULL DEFAULT gen_random_uuid(),
    tenant_id           UUID           NOT NULL,
    subscription_id     UUID           NOT NULL,
    description         VARCHAR(255)   NOT NULL,
    amount              NUMERIC(12, 2) NOT NULL,
    status              VARCHAR(20)    NOT NULL,
    due_date            DATE,
    paid_at             TIMESTAMP WITHOUT TIME ZONE,
    external_invoice_id VARCHAR(255),
    created_at          TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_invoices PRIMARY KEY (id),
    CONSTRAINT fk_invoices_tenant       FOREIGN KEY (tenant_id)       REFERENCES tenants (id),
    CONSTRAINT fk_invoices_subscription FOREIGN KEY (subscription_id) REFERENCES subscriptions (id)
);

CREATE INDEX idx_invoices_tenant_id   ON invoices (tenant_id);
CREATE INDEX idx_invoices_external_id ON invoices (external_invoice_id);
