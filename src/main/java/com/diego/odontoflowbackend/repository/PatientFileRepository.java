package com.diego.odontoflowbackend.repository;

import com.diego.odontoflowbackend.entity.PatientFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PatientFileRepository extends JpaRepository<PatientFile, UUID> {

    @Query("""
            select f from PatientFile f
            join fetch f.uploadedBy
            where f.patient.id = :patientId and f.tenantId = :tenantId
            order by f.createdAt desc
            """)
    List<PatientFile> findByPatient(@Param("patientId") UUID patientId, @Param("tenantId") UUID tenantId);

    Optional<PatientFile> findByIdAndTenantId(UUID id, UUID tenantId);
}
