-- Which dentists perform each service.
CREATE TABLE service_dentists
(
    service_id UUID NOT NULL,
    dentist_id UUID NOT NULL,
    CONSTRAINT pk_service_dentists PRIMARY KEY (service_id, dentist_id),
    CONSTRAINT fk_service_dentists_service FOREIGN KEY (service_id) REFERENCES services (id) ON DELETE CASCADE,
    CONSTRAINT fk_service_dentists_dentist FOREIGN KEY (dentist_id) REFERENCES users (id)
);

-- Backfill: every existing service is performed by all dentists of its tenant.
INSERT INTO service_dentists (service_id, dentist_id)
SELECT s.id, u.id
FROM services s
JOIN users u ON u.tenant_id = s.tenant_id AND u.role = 'DENTIST';

-- Area of dentistry for each service.
ALTER TABLE services ADD COLUMN category VARCHAR(40) NOT NULL DEFAULT 'GENERAL';

-- Sensible categories for the seeded starter catalogue.
UPDATE services SET category = 'ENDODONTICS' WHERE name = 'Tratamento de canal';
UPDATE services SET category = 'AESTHETICS'  WHERE name = 'Clareamento';
UPDATE services SET category = 'PERIODONTICS' WHERE name = 'Limpeza (profilaxia)';
