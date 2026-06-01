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
@Tag(name = "Auth", description = "Registro de clínica e autenticação")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/tenant")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrar clínica", description = "Cria um novo tenant e provisiona o dentista fundador como administrador")
    public AuthResponse registerTenant(@Valid @RequestBody RegisterTenantRequest request) {
        return authService.registerTenant(request);
    }

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Autentica o usuário e retorna um JWT com user_id, tenant_id e role")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
