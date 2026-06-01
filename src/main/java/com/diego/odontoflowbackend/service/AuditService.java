package com.diego.odontoflowbackend.service;

import com.diego.odontoflowbackend.audit.AuditRevision;
import com.diego.odontoflowbackend.entity.Patient;
import com.diego.odontoflowbackend.entity.dto.response.PatientAuditResponse;
import com.diego.odontoflowbackend.exception.NotFoundException;
import com.diego.odontoflowbackend.repository.PatientRepository;
import com.diego.odontoflowbackend.security.SecurityUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final PatientRepository patientRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public List<PatientAuditResponse> patientHistory(UUID patientId) {
        UUID tenantId = SecurityUtils.currentTenantId();
        if (patientRepository.findByIdAndTenantId(patientId, tenantId).isEmpty()) {
            throw new NotFoundException("Paciente não encontrado.");
        }

        AuditReader reader = AuditReaderFactory.get(entityManager);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = reader.createQuery()
                .forRevisionsOfEntity(Patient.class, false, true)
                .add(AuditEntity.id().eq(patientId))
                .addOrder(AuditEntity.revisionNumber().desc())
                .getResultList();

        return rows.stream().map(row -> {
            Patient snapshot = (Patient) row[0];
            AuditRevision revision = (AuditRevision) row[1];
            RevisionType type = (RevisionType) row[2];

            LocalDateTime changedAt = Instant.ofEpochMilli(revision.getTimestamp())
                    .atZone(ZoneId.systemDefault()).toLocalDateTime();

            return new PatientAuditResponse(
                    revision.getId(),
                    type.name(),
                    revision.getUserId(),
                    changedAt,
                    snapshot.getFullName(),
                    snapshot.getMedicalAlerts()
            );
        }).toList();
    }
}
