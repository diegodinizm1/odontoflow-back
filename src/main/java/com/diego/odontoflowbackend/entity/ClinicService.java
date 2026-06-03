package com.diego.odontoflowbackend.entity;

import com.diego.odontoflowbackend.entity.enums.DentalSpecialty;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** A bookable procedure/service offered by a clinic (shown on the public booking page). */
@Entity
@Table(name = "services", indexes = @Index(name = "idx_services_tenant_id", columnList = "tenant_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClinicService {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private String name;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private DentalSpecialty category;

    /** Dentists (users) who perform this service. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "service_dentists", joinColumns = @JoinColumn(name = "service_id"))
    @Column(name = "dentist_id")
    @Builder.Default
    private Set<UUID> dentistIds = new HashSet<>();

    @Column(nullable = false)
    private boolean active;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
