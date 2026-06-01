package com.diego.odontoflowbackend.controller;

import com.diego.odontoflowbackend.entity.dto.request.LoginRequest;
import com.diego.odontoflowbackend.entity.dto.request.RegisterTenantRequest;
import com.diego.odontoflowbackend.entity.dto.response.AuthResponse;
import com.diego.odontoflowbackend.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Clinic registration and authentication")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/tenant")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register clinic", description = "Creates a new tenant and provisions the founding dentist as administrator")
    public AuthResponse registerTenant(@Valid @RequestBody RegisterTenantRequest request) {
        return authService.registerTenant(request);
    }

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Authenticates the user and returns a JWT carrying user_id, tenant_id and role")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
