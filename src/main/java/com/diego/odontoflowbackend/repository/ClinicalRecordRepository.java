package com.diego.odontoflowbackend.repository;

import com.diego.odontoflowbackend.entity.ClinicalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ClinicalRecordRepository extends JpaRepository<ClinicalRecord, UUID> {

    @Query("""
            select r from ClinicalRecord r
            join fetch r.createdBy
            where r.patient.id = :patientId and r.tenantId = :tenantId
            order by r.createdAt desc
            """)
    List<ClinicalRecord> findHistory(@Param("patientId") UUID patientId, @Param("tenantId") UUID tenantId);
}
