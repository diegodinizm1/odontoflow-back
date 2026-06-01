package com.diego.odontoflowbackend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Audited
@Table(name = "clinical_records", indexes = {
        @Index(name = "idx_clinical_records_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_clinical_records_patient_id", columnList = "patient_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClinicalRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    private Patient patient;

    @Column(name = "appointment_id")
    private UUID appointmentId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "odontogram_data", columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private Map<String, ToothState> odontogramData = new HashMap<>();

    @Column(name = "clinical_notes", columnDefinition = "TEXT")
    private String clinicalNotes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
