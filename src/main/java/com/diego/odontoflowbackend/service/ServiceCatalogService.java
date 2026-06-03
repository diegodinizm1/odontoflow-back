package com.diego.odontoflowbackend.service;

import com.diego.odontoflowbackend.entity.ClinicService;
import com.diego.odontoflowbackend.entity.dto.request.CreateServiceRequest;
import com.diego.odontoflowbackend.entity.dto.response.ServiceResponse;
import com.diego.odontoflowbackend.exception.NotFoundException;
import com.diego.odontoflowbackend.repository.ServiceRepository;
import com.diego.odontoflowbackend.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ServiceCatalogService {

    private final ServiceRepository serviceRepository;

    /** Starter catalogue provisioned for every new clinic. */
    private static final List<ClinicService> DEFAULTS = List.of(
            template("Avaliação / Consulta", 30, "0.00"),
            template("Limpeza (profilaxia)", 40, "150.00"),
            template("Restauração", 50, "250.00"),
            template("Tratamento de canal", 60, "800.00"),
            template("Clareamento", 60, "600.00")
    );

    private static ClinicService template(String name, int dur, String price) {
        return ClinicService.builder().name(name).durationMinutes(dur).price(new BigDecimal(price)).build();
    }

    public List<ServiceResponse> list() {
        return serviceRepository.findByTenantIdAndActiveTrueOrderByName(SecurityUtils.currentTenantId())
                .stream().map(ServiceResponse::from).toList();
    }

    @Transactional
    public ServiceResponse create(CreateServiceRequest request) {
        ClinicService saved = serviceRepository.save(ClinicService.builder()
                .tenantId(SecurityUtils.currentTenantId())
                .name(request.name().trim())
                .durationMinutes(request.durationMinutes())
                .price(request.price())
                .active(true)
                .build());
        return ServiceResponse.from(saved);
    }

    @Transactional
    public ServiceResponse update(UUID id, CreateServiceRequest request) {
        ClinicService service = getOrThrow(id);
        service.setName(request.name().trim());
        service.setDurationMinutes(request.durationMinutes());
        service.setPrice(request.price());
        return ServiceResponse.from(serviceRepository.save(service));
    }

    /** Soft-delete: keeps the row so past appointments referencing it stay valid. */
    @Transactional
    public void delete(UUID id) {
        ClinicService service = getOrThrow(id);
        service.setActive(false);
        serviceRepository.save(service);
    }

    /** Provisions the starter catalogue for a freshly registered clinic. */
    @Transactional
    public void createDefaultsFor(UUID tenantId) {
        for (ClinicService d : DEFAULTS) {
            serviceRepository.save(ClinicService.builder()
                    .tenantId(tenantId)
                    .name(d.getName())
                    .durationMinutes(d.getDurationMinutes())
                    .price(d.getPrice())
                    .active(true)
                    .build());
        }
    }

    private ClinicService getOrThrow(UUID id) {
        return serviceRepository.findByIdAndTenantId(id, SecurityUtils.currentTenantId())
                .orElseThrow(() -> new NotFoundException("Serviço não encontrado."));
    }
}
