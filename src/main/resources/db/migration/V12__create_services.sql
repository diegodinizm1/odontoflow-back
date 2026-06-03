-- Bookable procedures/services offered by each clinic.
CREATE TABLE services
(
    id               UUID         NOT NULL DEFAULT gen_random_uuid(),
    tenant_id        UUID         NOT NULL,
    name             VARCHAR(120) NOT NULL,
    duration_minutes INT          NOT NULL,
    price            NUMERIC(12, 2) NOT NULL DEFAULT 0,
    active           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_services PRIMARY KEY (id),
    CONSTRAINT fk_services_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id)
);

CREATE INDEX idx_services_tenant_id ON services (tenant_id);

-- Link an appointment to the booked service (optional; manual scheduling leaves it null).
ALTER TABLE appointments ADD COLUMN service_id UUID;
ALTER TABLE appointments ADD CONSTRAINT fk_appointments_service FOREIGN KEY (service_id) REFERENCES services (id);

-- Seed a starter catalogue for every existing clinic so the public flow works out of the box.
INSERT INTO services (tenant_id, name, duration_minutes, price, active, created_at)
SELECT t.id, s.name, s.dur, s.price, TRUE, now()
FROM tenants t
CROSS JOIN (VALUES
    ('Avaliação / Consulta', 30, 0.00),
    ('Limpeza (profilaxia)', 40, 150.00),
    ('Restauração', 50, 250.00),
    ('Tratamento de canal', 60, 800.00),
    ('Clareamento', 60, 600.00)
) AS s(name, dur, price);
