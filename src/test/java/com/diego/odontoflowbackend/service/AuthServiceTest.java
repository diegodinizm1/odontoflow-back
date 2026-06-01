package com.diego.odontoflowbackend.service;

import com.diego.odontoflowbackend.entity.Tenant;
import com.diego.odontoflowbackend.entity.User;
import com.diego.odontoflowbackend.entity.dto.request.LoginRequest;
import com.diego.odontoflowbackend.entity.dto.request.RegisterTenantRequest;
import com.diego.odontoflowbackend.entity.dto.response.AuthResponse;
import com.diego.odontoflowbackend.entity.enums.Role;
import com.diego.odontoflowbackend.exception.ConflictException;
import com.diego.odontoflowbackend.exception.UnauthorizedException;
import com.diego.odontoflowbackend.repository.TenantRepository;
import com.diego.odontoflowbackend.repository.UserRepository;
import com.diego.odontoflowbackend.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock TenantRepository tenantRepository;
    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtUtil jwtUtil;

    @InjectMocks AuthService authService;

    private final RegisterTenantRequest validRegister = new RegisterTenantRequest(
            "Clínica Teste", "12.345.678/0001-99",
            "Dr. Diego", "diego@teste.com", "senha1234"
    );

    @Test
    void registerTenant_success() {
        UUID tenantId = UUID.randomUUID();
        Tenant savedTenant = Tenant.builder().id(tenantId).build();
        User savedUser = User.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .role(Role.DENTIST)
                .email("diego@teste.com")
                .build();

        when(tenantRepository.existsByDocument(any())).thenReturn(false);
        when(tenantRepository.save(any())).thenReturn(savedTenant);
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        when(userRepository.save(any())).thenReturn(savedUser);
        when(jwtUtil.generateToken(savedUser)).thenReturn("jwt-token");

        AuthResponse response = authService.registerTenant(validRegister);

        assertThat(response.token()).isEqualTo("jwt-token");
        verify(tenantRepository).save(any());
        verify(userRepository).save(any());
    }

    @Test
    void registerTenant_duplicateDocument_throwsConflict() {
        when(tenantRepository.existsByDocument("12.345.678/0001-99")).thenReturn(true);

        assertThatThrownBy(() -> authService.registerTenant(validRegister))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("documento");
    }

    @Test
    void login_success() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .tenantId(UUID.randomUUID())
                .email("diego@teste.com")
                .passwordHash("hashed")
                .role(Role.DENTIST)
                .build();

        when(userRepository.findByEmail("diego@teste.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("senha1234", "hashed")).thenReturn(true);
        when(jwtUtil.generateToken(user)).thenReturn("jwt-token");

        AuthResponse response = authService.login(new LoginRequest("diego@teste.com", "senha1234"));

        assertThat(response.token()).isEqualTo("jwt-token");
    }

    @Test
    void login_userNotFound_throwsUnauthorized() {
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("naoexiste@teste.com", "qualquer")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Credenciais inválidas");
    }

    @Test
    void login_wrongPassword_throwsUnauthorized() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("diego@teste.com")
                .passwordHash("hashed")
                .build();

        when(userRepository.findByEmail("diego@teste.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("errada", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("diego@teste.com", "errada")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Credenciais inválidas");
    }
}
