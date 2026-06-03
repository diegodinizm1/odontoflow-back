-- Public online-booking page identifier for each clinic.
ALTER TABLE tenants ADD COLUMN public_slug VARCHAR(80);

-- Backfill existing clinics with a deterministic, unique slug derived from their id.
UPDATE tenants
SET public_slug = 'clinica-' || left(replace(id::text, '-', ''), 10)
WHERE public_slug IS NULL;

ALTER TABLE tenants ALTER COLUMN public_slug SET NOT NULL;
ALTER TABLE tenants ADD CONSTRAINT uq_tenants_public_slug UNIQUE (public_slug);
