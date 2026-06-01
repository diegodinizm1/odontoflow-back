package com.diego.odontoflowbackend.entity.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterTenantRequest(
        @NotBlank String clinicName,
        @NotBlank String document,
        @NotBlank String fullName,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8) String password
) {}
