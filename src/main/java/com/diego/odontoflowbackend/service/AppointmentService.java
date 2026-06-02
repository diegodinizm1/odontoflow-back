package com.diego.odontoflowbackend.service;

import com.diego.odontoflowbackend.entity.Appointment;
import com.diego.odontoflowbackend.entity.Patient;
import com.diego.odontoflowbackend.entity.dto.request.CreateAppointmentRequest;
import com.diego.odontoflowbackend.entity.dto.request.RescheduleAppointmentRequest;
import com.diego.odontoflowbackend.entity.dto.request.UpdateAppointmentStatusRequest;
import com.diego.odontoflowbackend.entity.dto.response.AppointmentResponse;
import com.diego.odontoflowbackend.entity.enums.AppointmentStatus;
import com.diego.odontoflowbackend.exception.BadRequestException;
import com.diego.odontoflowbackend.exception.ConflictException;
import com.diego.odontoflowbackend.exception.NotFoundException;
import com.diego.odontoflowbackend.repository.AppointmentRepository;
import com.diego.odontoflowbackend.repository.PatientRepository;
import com.diego.odontoflowbackend.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;

    /**
     * Lists appointments in the range. A dentist only ever sees their own agenda;
     * a receptionist sees everyone and may filter by {@code dentistId}.
     */
    public List<AppointmentResponse> listInRange(LocalDateTime start, LocalDateTime end, UUID dentistId) {
        if (end.isBefore(start)) {
            throw new BadRequestException("O fim do período deve ser após o início.");
        }
        UUID tenantId = SecurityUtils.currentTenantId();

        UUID effectiveDentistId = "DENTIST".equals(SecurityUtils.currentRole())
                ? SecurityUtils.currentUserId()   // dentists are always scoped to themselves
                : dentistId;                       // receptionists: optional filter (null = all)

        List<Appointment> appointments = effectiveDentistId != null
                ? appointmentRepository.findInRangeByDentist(tenantId, effectiveDentistId, start, end)
                : appointmentRepository.findInRange(tenantId, start, end);

        return appointments.stream().map(AppointmentResponse::from).toList();
    }

    @Transactional
    public AppointmentResponse create(CreateAppointmentRequest request) {
        UUID tenantId = SecurityUtils.currentTenantId();
        UUID dentistId = request.dentistId() != null ? request.dentistId() : SecurityUtils.currentUserId();

        validateInterval(request.startTime(), request.endTime());

        Patient patient = patientRepository.findByIdAndTenantId(request.patientId(), tenantId)
                .orElseThrow(() -> new NotFoundException("Paciente não encontrado."));

        ensureNoOverlap(tenantId, dentistId, request.startTime(), request.endTime(), null);

        Appointment appointment = Appointment.builder()
                .tenantId(tenantId)
                .patient(patient)
                .dentistId(dentistId)
                .startTime(request.startTime())
                .endTime(request.endTime())
                .status(AppointmentStatus.SCHEDULED)
                .build();

        return AppointmentResponse.from(appointmentRepository.save(appointment));
    }

    @Transactional
    public AppointmentResponse reschedule(UUID id, RescheduleAppointmentRequest request) {
        Appointment appointment = getOrThrow(id);

        if (appointment.getStatus() == AppointmentStatus.CANCELED) {
            throw new BadRequestException("Não é possível reagendar uma consulta cancelada.");
        }

        validateInterval(request.startTime(), request.endTime());
        ensureNoOverlap(appointment.getTenantId(), appointment.getDentistId(),
                request.startTime(), request.endTime(), appointment.getId());

        appointment.setStartTime(request.startTime());
        appointment.setEndTime(request.endTime());
        return AppointmentResponse.from(appointmentRepository.save(appointment));
    }

    @Transactional
    public AppointmentResponse updateStatus(UUID id, UpdateAppointmentStatusRequest request) {
        Appointment appointment = getOrThrow(id);
        appointment.setStatus(request.status());
        return AppointmentResponse.from(appointmentRepository.save(appointment));
    }

    private void validateInterval(LocalDateTime start, LocalDateTime end) {
        if (!end.isAfter(start)) {
            throw new BadRequestException("O horário de término deve ser após o de início.");
        }
    }

    private void ensureNoOverlap(UUID tenantId, UUID dentistId, LocalDateTime start, LocalDateTime end, UUID excludeId) {
        if (appointmentRepository.existsOverlap(tenantId, dentistId, start, end, excludeId)) {
            throw new ConflictException("Já existe uma consulta agendada para esse dentista nesse horário.");
        }
    }

    private Appointment getOrThrow(UUID id) {
        return appointmentRepository.findByIdAndTenantId(id, SecurityUtils.currentTenantId())
                .orElseThrow(() -> new NotFoundException("Agendamento não encontrado."));
    }
}
