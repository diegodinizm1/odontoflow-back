package com.diego.odontoflowbackend.controller;

import com.diego.odontoflowbackend.entity.dto.request.CreateServiceRequest;
import com.diego.odontoflowbackend.entity.dto.response.ServiceResponse;
import com.diego.odontoflowbackend.service.ServiceCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/services")
@RequiredArgsConstructor
@Tag(name = "Services", description = "Clinic procedure/service catalogue (bookable online)")
@SecurityRequirement(name = "bearerAuth")
public class ServiceController {

    private final ServiceCatalogService serviceCatalogService;

    @GetMapping
    @Operation(summary = "List the clinic's active services")
    public List<ServiceResponse> list() {
        return serviceCatalogService.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a service")
    public ServiceResponse create(@Valid @RequestBody CreateServiceRequest request) {
        return serviceCatalogService.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a service")
    public ServiceResponse update(@PathVariable UUID id, @Valid @RequestBody CreateServiceRequest request) {
        return serviceCatalogService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove a service (soft delete)")
    public void delete(@PathVariable UUID id) {
        serviceCatalogService.delete(id);
    }
}
