package com.diego.odontoflowbackend.repository;

import com.diego.odontoflowbackend.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PatientRepository extends JpaRepository<Patient, UUID> {
    List<Patient> findAllByTenantId(UUID tenantId);
    Optional<Patient> findByIdAndTenantId(UUID id, UUID tenantId);
    Optional<Patient> findFirstByTenantIdAndPhone(UUID tenantId, String phone);
    boolean existsByIdAndTenantId(UUID id, UUID tenantId);
    long countByTenantId(UUID tenantId);
}
