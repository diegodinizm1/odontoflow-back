package com.diego.odontoflowbackend.repository;

import com.diego.odontoflowbackend.entity.Appointment;
import com.diego.odontoflowbackend.entity.enums.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    Optional<Appointment> findByIdAndTenantId(UUID id, UUID tenantId);

    long countByTenantIdAndStatus(UUID tenantId, AppointmentStatus status);

    @Query("""
            select a from Appointment a
            join fetch a.patient
            where a.tenantId = :tenantId
              and a.startTime < :end
              and a.endTime > :start
            order by a.startTime
            """)
    List<Appointment> findInRange(@Param("tenantId") UUID tenantId,
                                  @Param("start") LocalDateTime start,
                                  @Param("end") LocalDateTime end);

    @Query("""
            select a from Appointment a
            join fetch a.patient
            where a.tenantId = :tenantId
              and a.dentistId = :dentistId
              and a.startTime < :end
              and a.endTime > :start
            order by a.startTime
            """)
    List<Appointment> findInRangeByDentist(@Param("tenantId") UUID tenantId,
                                           @Param("dentistId") UUID dentistId,
                                           @Param("start") LocalDateTime start,
                                           @Param("end") LocalDateTime end);

    @Query("""
            select count(a) > 0 from Appointment a
            where a.tenantId = :tenantId
              and a.dentistId = :dentistId
              and a.status in (
                    com.diego.odontoflowbackend.entity.enums.AppointmentStatus.SCHEDULED,
                    com.diego.odontoflowbackend.entity.enums.AppointmentStatus.PENDING)
              and (:excludeId is null or a.id <> :excludeId)
              and a.startTime < :end
              and a.endTime > :start
            """)
    boolean existsOverlap(@Param("tenantId") UUID tenantId,
                          @Param("dentistId") UUID dentistId,
                          @Param("start") LocalDateTime start,
                          @Param("end") LocalDateTime end,
                          @Param("excludeId") UUID excludeId);
}
