ALTER TABLE patients ADD COLUMN phone VARCHAR(20);

-- Hibernate Envers audit table must mirror the audited entity's columns
ALTER TABLE patients_aud ADD COLUMN phone VARCHAR(20);
