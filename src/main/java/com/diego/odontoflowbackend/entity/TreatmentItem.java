package com.diego.odontoflowbackend.entity;

import com.diego.odontoflowbackend.entity.enums.TreatmentItemStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "treatment_items", indexes = @Index(name = "idx_treatment_items_plan_id", columnList = "treatment_plan_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TreatmentItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "treatment_plan_id", nullable = false)
    private TreatmentPlan treatmentPlan;

    @Column(nullable = false)
    private String description;

    /** Optional FDI tooth number (e.g. "16"). */
    @Column(length = 5)
    private String tooth;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TreatmentItemStatus status;

    /** Charge generated when the item is completed (idempotency guard). */
    @Column(name = "charge_id")
    private UUID chargeId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
