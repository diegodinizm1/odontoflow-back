package com.diego.odontoflowbackend.service;

import com.diego.odontoflowbackend.entity.ClinicService;
import com.diego.odontoflowbackend.entity.dto.request.CreateServiceRequest;
import com.diego.odontoflowbackend.entity.dto.response.ServiceResponse;
import com.diego.odontoflowbackend.entity.enums.DentalSpecialty;
import com.diego.odontoflowbackend.entity.enums.Role;
import com.diego.odontoflowbackend.exception.BadRequestException;
import com.diego.odontoflowbackend.exception.NotFoundException;
import com.diego.odontoflowbackend.repository.ServiceRepository;
import com.diego.odontoflowbackend.repository.UserRepository;
import com.diego.odontoflowbackend.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ServiceCatalogService {

    private final ServiceRepository serviceRepository;
    private final UserRepository userRepository;

    private record Default(String name, DentalSpecialty category, int dur, String price) {}

    /** Starter catalogue provisioned for every new clinic (all assigned to the founding dentist). */
    private static final List<Default> DEFAULTS = List.of(
            new Default("Avaliação / Consulta", DentalSpecialty.GENERAL, 30, "0.00"),
            new Default("Limpeza (profilaxia)", DentalSpecialty.PERIODONTICS, 40, "150.00"),
            new Default("Restauração", DentalSpecialty.GENERAL, 50, "250.00"),
            new Default("Tratamento de canal", DentalSpecialty.ENDODONTICS, 60, "800.00"),
            new Default("Clareamento", DentalSpecialty.AESTHETICS, 60, "600.00")
    );

    public List<ServiceResponse> list() {
        return serviceRepository.findByTenantIdAndActiveTrueOrderByName(SecurityUtils.currentTenantId())
                .stream().map(ServiceResponse::from).toList();
    }

    @Transactional
    public ServiceResponse create(CreateServiceRequest request) {
        UUID tenantId = SecurityUtils.currentTenantId();
        ClinicService saved = serviceRepository.save(ClinicService.builder()
                .tenantId(tenantId)
                .name(request.name().trim())
                .category(request.category())
                .durationMinutes(request.durationMinutes())
                .price(request.price())
                .dentistIds(validatedDentists(tenantId, request.dentistIds()))
                .active(true)
                .build());
        return ServiceResponse.from(saved);
    }

    @Transactional
    public ServiceResponse update(UUID id, CreateServiceRequest request) {
        UUID tenantId = SecurityUtils.currentTenantId();
        ClinicService service = getOrThrow(id);
        service.setName(request.name().trim());
        service.setCategory(request.category());
        service.setDurationMinutes(request.durationMinutes());
        service.setPrice(request.price());
        service.setDentistIds(validatedDentists(tenantId, request.dentistIds()));
        return ServiceResponse.from(serviceRepository.save(service));
    }

    /** Soft-delete: keeps the row so past appointments referencing it stay valid. */
    @Transactional
    public void delete(UUID id) {
        ClinicService service = getOrThrow(id);
        service.setActive(false);
        serviceRepository.save(service);
    }

    /** Provisions the starter catalogue for a freshly registered clinic, assigned to its founder. */
    @Transactional
    public void createDefaultsFor(UUID tenantId, UUID founderDentistId) {
        for (Default d : DEFAULTS) {
            serviceRepository.save(ClinicService.builder()
                    .tenantId(tenantId)
                    .name(d.name())
                    .category(d.category())
                    .durationMinutes(d.dur())
                    .price(new BigDecimal(d.price()))
                    .dentistIds(new HashSet<>(Set.of(founderDentistId)))
                    .active(true)
                    .build());
        }
    }

    /** Keeps only ids that are dentists of this tenant. */
    private Set<UUID> validatedDentists(UUID tenantId, List<UUID> ids) {
        Set<UUID> result = new HashSet<>();
        if (ids == null) return result;
        for (UUID id : ids) {
            userRepository.findByIdAndTenantId(id, tenantId)
                    .filter(u -> u.getRole() == Role.DENTIST)
                    .orElseThrow(() -> new BadRequestException("Dentista inválido para este serviço."));
            result.add(id);
        }
        return result;
    }

    private ClinicService getOrThrow(UUID id) {
        return serviceRepository.findByIdAndTenantId(id, SecurityUtils.currentTenantId())
                .orElseThrow(() -> new NotFoundException("Serviço não encontrado."));
    }
}
