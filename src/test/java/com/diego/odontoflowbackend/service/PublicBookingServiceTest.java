package com.diego.odontoflowbackend.service;

import com.diego.odontoflowbackend.entity.Appointment;
import com.diego.odontoflowbackend.entity.Patient;
import com.diego.odontoflowbackend.entity.Tenant;
import com.diego.odontoflowbackend.entity.User;
import com.diego.odontoflowbackend.entity.dto.request.CreateBookingRequest;
import com.diego.odontoflowbackend.entity.dto.response.AvailabilityResponse;
import com.diego.odontoflowbackend.entity.dto.response.BookingConfirmationResponse;
import com.diego.odontoflowbackend.entity.dto.response.PublicClinicResponse;
import com.diego.odontoflowbackend.entity.enums.AppointmentStatus;
import com.diego.odontoflowbackend.entity.enums.Role;
import com.diego.odontoflowbackend.exception.ConflictException;
import com.diego.odontoflowbackend.exception.NotFoundException;
import com.diego.odontoflowbackend.repository.AppointmentRepository;
import com.diego.odontoflowbackend.repository.PatientRepository;
import com.diego.odontoflowbackend.repository.TenantRepository;
import com.diego.odontoflowbackend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PublicBookingServiceTest {

    @Mock TenantRepository tenantRepository;
    @Mock UserRepository userRepository;
    @Mock PatientRepository patientRepository;
    @Mock AppointmentRepository appointmentRepository;
    @InjectMocks PublicBookingService service;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID dentistId = UUID.randomUUID();
    private final String slug = "clinica-sorriso";
    private final LocalDate futureDay = LocalDate.now().plusDays(7);

    private Tenant tenant;
    private User dentist;

    @BeforeEach
    void setUp() {
        tenant = Tenant.builder().id(tenantId).clinicName("Clínica Sorriso").publicSlug(slug).build();
        dentist = User.builder().id(dentistId).tenantId(tenantId).fullName("Dra. Ana").role(Role.DENTIST).build();
    }

    @Test
    void clinic_returnsOnlyDentists() {
        User receptionist = User.builder().id(UUID.randomUUID()).tenantId(tenantId)
                .fullName("Recep").role(Role.RECEPTIONIST).build();
        when(tenantRepository.findByPublicSlug(slug)).thenReturn(Optional.of(tenant));
        when(userRepository.findByTenantIdOrderByCreatedAtAsc(tenantId))
                .thenReturn(List.of(dentist, receptionist));

        PublicClinicResponse res = service.clinic(slug);

        assertThat(res.clinicName()).isEqualTo("Clínica Sorriso");
        assertThat(res.dentists()).hasSize(1);
        assertThat(res.dentists().get(0).id()).isEqualTo(dentistId);
    }

    @Test
    void clinic_unknownSlug_throwsNotFound() {
        when(tenantRepository.findByPublicSlug("nope")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.clinic("nope")).isInstanceOf(NotFoundException.class);
    }

    @Test
    void availability_excludesTakenSlots() {
        when(tenantRepository.findByPublicSlug(slug)).thenReturn(Optional.of(tenant));
        when(userRepository.findByIdAndTenantId(dentistId, tenantId)).thenReturn(Optional.of(dentist));

        Appointment busy = Appointment.builder()
                .tenantId(tenantId).dentistId(dentistId)
                .startTime(futureDay.atTime(9, 0)).endTime(futureDay.atTime(10, 0))
                .status(AppointmentStatus.SCHEDULED).build();
        when(appointmentRepository.findInRangeByDentist(eq(tenantId), eq(dentistId), any(), any()))
                .thenReturn(List.of(busy));

        AvailabilityResponse res = service.availability(slug, dentistId, futureDay);

        assertThat(res.slots()).contains("08:00", "08:30", "10:00", "10:30");
        assertThat(res.slots()).doesNotContain("09:00", "09:30");
    }

    @Test
    void book_whenFree_createsPendingAndDedupsNewPatient() {
        when(tenantRepository.findByPublicSlug(slug)).thenReturn(Optional.of(tenant));
        when(userRepository.findByIdAndTenantId(dentistId, tenantId)).thenReturn(Optional.of(dentist));
        when(appointmentRepository.existsOverlap(eq(tenantId), eq(dentistId), any(), any(), isNull()))
                .thenReturn(false);
        when(patientRepository.findFirstByTenantIdAndPhone(tenantId, "11999990000"))
                .thenReturn(Optional.empty());
        when(patientRepository.save(any(Patient.class))).thenAnswer(inv -> {
            Patient p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> {
            Appointment a = inv.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });

        BookingConfirmationResponse res = service.book(slug, new CreateBookingRequest(
                dentistId, futureDay, LocalTime.of(9, 0), "Carlos Paciente", "11999990000"));

        assertThat(res.status()).isEqualTo(AppointmentStatus.PENDING);
        assertThat(res.dentistName()).isEqualTo("Dra. Ana");
        verify(patientRepository).save(any(Patient.class)); // new patient created
        verify(appointmentRepository).save(argThat(a -> a.getStatus() == AppointmentStatus.PENDING));
    }

    @Test
    void book_reusesExistingPatientByPhone() {
        Patient existing = Patient.builder().id(UUID.randomUUID()).tenantId(tenantId)
                .fullName("Carlos").phone("11999990000").build();
        when(tenantRepository.findByPublicSlug(slug)).thenReturn(Optional.of(tenant));
        when(userRepository.findByIdAndTenantId(dentistId, tenantId)).thenReturn(Optional.of(dentist));
        when(appointmentRepository.existsOverlap(any(), any(), any(), any(), isNull())).thenReturn(false);
        when(patientRepository.findFirstByTenantIdAndPhone(tenantId, "11999990000"))
                .thenReturn(Optional.of(existing));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        service.book(slug, new CreateBookingRequest(
                dentistId, futureDay, LocalTime.of(11, 0), "Carlos", "11999990000"));

        verify(patientRepository, never()).save(any());
    }

    @Test
    void book_whenSlotTaken_throwsConflict() {
        when(tenantRepository.findByPublicSlug(slug)).thenReturn(Optional.of(tenant));
        when(userRepository.findByIdAndTenantId(dentistId, tenantId)).thenReturn(Optional.of(dentist));
        when(appointmentRepository.existsOverlap(any(), any(), any(), any(), isNull())).thenReturn(true);

        assertThatThrownBy(() -> service.book(slug, new CreateBookingRequest(
                dentistId, futureDay, LocalTime.of(9, 0), "Carlos", "11999990000")))
                .isInstanceOf(ConflictException.class);
        verify(appointmentRepository, never()).save(any());
    }
}
