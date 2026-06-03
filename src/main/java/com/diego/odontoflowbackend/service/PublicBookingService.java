package com.diego.odontoflowbackend.service;

import com.diego.odontoflowbackend.entity.Appointment;
import com.diego.odontoflowbackend.entity.ClinicService;
import com.diego.odontoflowbackend.entity.Patient;
import com.diego.odontoflowbackend.entity.Tenant;
import com.diego.odontoflowbackend.entity.User;
import com.diego.odontoflowbackend.entity.dto.request.CreateBookingRequest;
import com.diego.odontoflowbackend.entity.dto.response.AvailabilityResponse;
import com.diego.odontoflowbackend.entity.dto.response.BookingConfirmationResponse;
import com.diego.odontoflowbackend.entity.dto.response.ClinicSummaryResponse;
import com.diego.odontoflowbackend.entity.dto.response.PublicClinicResponse;
import com.diego.odontoflowbackend.entity.dto.response.PublicDentistResponse;
import com.diego.odontoflowbackend.entity.dto.response.PublicServiceResponse;
import com.diego.odontoflowbackend.entity.enums.AppointmentStatus;
import com.diego.odontoflowbackend.entity.enums.Role;
import com.diego.odontoflowbackend.exception.BadRequestException;
import com.diego.odontoflowbackend.exception.ConflictException;
import com.diego.odontoflowbackend.exception.NotFoundException;
import com.diego.odontoflowbackend.repository.AppointmentRepository;
import com.diego.odontoflowbackend.repository.PatientRepository;
import com.diego.odontoflowbackend.repository.ServiceRepository;
import com.diego.odontoflowbackend.repository.TenantRepository;
import com.diego.odontoflowbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Unauthenticated booking flow for the public marketplace. Patients browse all clinics,
 * open a clinic, pick a service + dentist + slot and request an appointment. A booking is
 * created as {@link AppointmentStatus#PENDING} and reserves the slot (the overlap check
 * counts PENDING) until the clinic confirms or rejects it. The slot length is the service's
 * duration.
 */
@Service
@RequiredArgsConstructor
public class PublicBookingService {

    static final int WORK_START_HOUR = 8;
    static final int WORK_END_HOUR = 18;
    static final int SLOT_STEP_MINUTES = 30;

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final ServiceRepository serviceRepository;

    /** Public directory: every clinic with its bookable dentist/service counts. */
    public List<ClinicSummaryResponse> clinics() {
        return tenantRepository.findAll().stream()
                .map(t -> new ClinicSummaryResponse(
                        t.getClinicName(),
                        t.getPublicSlug(),
                        countDentists(t.getId()),
                        serviceRepository.findByTenantIdAndActiveTrueOrderByName(t.getId()).size()))
                .sorted(Comparator.comparing(ClinicSummaryResponse::clinicName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public PublicClinicResponse clinic(String slug) {
        Tenant tenant = getTenant(slug);
        List<PublicDentistResponse> dentists = userRepository
                .findByTenantIdOrderByCreatedAtAsc(tenant.getId()).stream()
                .filter(u -> u.getRole() == Role.DENTIST)
                .map(PublicDentistResponse::from)
                .toList();
        List<PublicServiceResponse> services = serviceRepository
                .findByTenantIdAndActiveTrueOrderByName(tenant.getId()).stream()
                .map(PublicServiceResponse::from)
                .toList();
        return new PublicClinicResponse(tenant.getClinicName(), tenant.getPublicSlug(), dentists, services);
    }

    public AvailabilityResponse availability(String slug, UUID dentistId, UUID serviceId, LocalDate date) {
        Tenant tenant = getTenant(slug);
        ensureDentist(tenant.getId(), dentistId);
        int duration = resolveService(tenant.getId(), serviceId).getDurationMinutes();

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();
        List<Appointment> busy = appointmentRepository
                .findInRangeByDentist(tenant.getId(), dentistId, startOfDay, endOfDay).stream()
                .filter(a -> a.getStatus() == AppointmentStatus.SCHEDULED
                        || a.getStatus() == AppointmentStatus.PENDING)
                .toList();

        LocalDateTime now = LocalDateTime.now();
        LocalTime close = LocalTime.of(WORK_END_HOUR, 0);
        List<String> slots = new ArrayList<>();
        for (LocalTime t = LocalTime.of(WORK_START_HOUR, 0);
             !t.plusMinutes(duration).isAfter(close);
             t = t.plusMinutes(SLOT_STEP_MINUTES)) {

            LocalDateTime start = date.atTime(t);
            LocalDateTime end = start.plusMinutes(duration);
            if (!start.isAfter(now)) continue; // never offer a slot in the past

            boolean taken = busy.stream()
                    .anyMatch(a -> a.getStartTime().isBefore(end) && a.getEndTime().isAfter(start));
            if (!taken) slots.add(t.toString());
        }
        return new AvailabilityResponse(dentistId, date, slots);
    }

    @Transactional
    public BookingConfirmationResponse book(String slug, CreateBookingRequest request) {
        Tenant tenant = getTenant(slug);
        User dentist = ensureDentist(tenant.getId(), request.dentistId());
        ClinicService service = resolveService(tenant.getId(), request.serviceId());

        LocalDateTime start = request.date().atTime(request.time());
        LocalDateTime end = start.plusMinutes(service.getDurationMinutes());
        validateWithinWorkingHours(request.time(), end.toLocalTime());
        if (!start.isAfter(LocalDateTime.now())) {
            throw new BadRequestException("Escolha um horário futuro.");
        }

        if (appointmentRepository.existsOverlap(tenant.getId(), dentist.getId(), start, end, null)) {
            throw new ConflictException("Esse horário acabou de ser preenchido. Escolha outro.");
        }

        Patient patient = patientRepository
                .findFirstByTenantIdAndPhone(tenant.getId(), request.patientPhone())
                .orElseGet(() -> patientRepository.save(Patient.builder()
                        .tenantId(tenant.getId())
                        .fullName(request.patientName().trim())
                        .phone(request.patientPhone())
                        .build()));

        Appointment appointment = appointmentRepository.save(Appointment.builder()
                .tenantId(tenant.getId())
                .patient(patient)
                .dentistId(dentist.getId())
                .service(service)
                .startTime(start)
                .endTime(end)
                .status(AppointmentStatus.PENDING)
                .build());

        return new BookingConfirmationResponse(
                appointment.getId(), dentist.getFullName(), start, end, appointment.getStatus());
    }

    private void validateWithinWorkingHours(LocalTime start, LocalTime end) {
        LocalTime open = LocalTime.of(WORK_START_HOUR, 0);
        LocalTime close = LocalTime.of(WORK_END_HOUR, 0);
        boolean offGrid = start.getMinute() % SLOT_STEP_MINUTES != 0;
        if (start.isBefore(open) || end.isAfter(close) || offGrid) {
            throw new BadRequestException("Horário fora do expediente de atendimento.");
        }
    }

    private long countDentists(UUID tenantId) {
        return userRepository.findByTenantIdOrderByCreatedAtAsc(tenantId).stream()
                .filter(u -> u.getRole() == Role.DENTIST)
                .count();
    }

    private Tenant getTenant(String slug) {
        return tenantRepository.findByPublicSlug(slug)
                .orElseThrow(() -> new NotFoundException("Clínica não encontrada."));
    }

    private User ensureDentist(UUID tenantId, UUID dentistId) {
        User user = userRepository.findByIdAndTenantId(dentistId, tenantId)
                .orElseThrow(() -> new NotFoundException("Dentista não encontrado."));
        if (user.getRole() != Role.DENTIST) {
            throw new BadRequestException("Profissional inválido.");
        }
        return user;
    }

    private ClinicService resolveService(UUID tenantId, UUID serviceId) {
        ClinicService service = serviceRepository.findByIdAndTenantId(serviceId, tenantId)
                .orElseThrow(() -> new NotFoundException("Serviço não encontrado."));
        if (!service.isActive()) {
            throw new BadRequestException("Serviço indisponível.");
        }
        return service;
    }
}
