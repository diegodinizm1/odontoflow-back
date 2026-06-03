package com.diego.odontoflowbackend.repository;

import com.diego.odontoflowbackend.entity.ClinicService;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceRepository extends JpaRepository<ClinicService, UUID> {
    List<ClinicService> findByTenantIdOrderByName(UUID tenantId);
    List<ClinicService> findByTenantIdAndActiveTrueOrderByName(UUID tenantId);
    Optional<ClinicService> findByIdAndTenantId(UUID id, UUID tenantId);
}
