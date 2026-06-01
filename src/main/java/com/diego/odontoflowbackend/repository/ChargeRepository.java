package com.diego.odontoflowbackend.repository;

import com.diego.odontoflowbackend.entity.Charge;
import com.diego.odontoflowbackend.entity.enums.ChargeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChargeRepository extends JpaRepository<Charge, UUID> {

    @Query("""
            select c from Charge c
            join fetch c.patient
            where c.tenantId = :tenantId
            order by c.createdAt desc
            """)
    List<Charge> findAllByTenant(@Param("tenantId") UUID tenantId);

    Optional<Charge> findByIdAndTenantId(UUID id, UUID tenantId);

    @Query("""
            select coalesce(sum(c.amount), 0) from Charge c
            where c.tenantId = :tenantId and c.status = :status
              and c.paidAt >= :start and c.paidAt < :end
            """)
    BigDecimal sumPaidInPeriod(@Param("tenantId") UUID tenantId,
                               @Param("status") ChargeStatus status,
                               @Param("start") LocalDateTime start,
                               @Param("end") LocalDateTime end);

    @Query("select coalesce(sum(c.amount), 0) from Charge c where c.tenantId = :tenantId and c.status = :status")
    BigDecimal sumByStatus(@Param("tenantId") UUID tenantId, @Param("status") ChargeStatus status);
}
