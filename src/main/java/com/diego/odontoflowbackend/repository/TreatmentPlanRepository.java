package com.diego.odontoflowbackend.repository;

import com.diego.odontoflowbackend.entity.TreatmentPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TreatmentPlanRepository extends JpaRepository<TreatmentPlan, UUID> {

    @Query("""
            select distinct p from TreatmentPlan p
            left join fetch p.items
            join fetch p.createdBy
            where p.patient.id = :patientId and p.tenantId = :tenantId
            order by p.createdAt desc
            """)
    List<TreatmentPlan> findByPatient(@Param("patientId") UUID patientId, @Param("tenantId") UUID tenantId);

    @Query("""
            select p from TreatmentPlan p
            left join fetch p.items
            join fetch p.createdBy
            where p.id = :id and p.tenantId = :tenantId
            """)
    Optional<TreatmentPlan> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);
}
