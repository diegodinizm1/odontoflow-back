package com.diego.odontoflowbackend.service;

import com.diego.odontoflowbackend.entity.Tenant;
import com.diego.odontoflowbackend.entity.User;
import com.diego.odontoflowbackend.entity.dto.request.LoginRequest;
import com.diego.odontoflowbackend.entity.dto.request.RegisterTenantRequest;
import com.diego.odontoflowbackend.entity.dto.response.AuthResponse;
import com.diego.odontoflowbackend.entity.enums.Role;
import com.diego.odontoflowbackend.repository.TenantRepository;
import com.diego.odontoflowbackend.repository.UserRepository;
import com.diego.odontoflowbackend.security.JwtUtil;
import com.diego.odontoflowbackend.exception.ConflictException;
import com.diego.odontoflowbackend.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public AuthResponse registerTenant(RegisterTenantRequest request) {
        if (tenantRepository.existsByDocument(request.document())) {
            throw new ConflictException("Clínica já cadastrada com este documento.");
        }

        Tenant tenant = tenantRepository.save(Tenant.builder()
                .clinicName(request.clinicName())
                .document(request.document())
                .build());

        User founder = userRepository.save(User.builder()
                .tenantId(tenant.getId())
                .fullName(request.fullName())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(Role.DENTIST)
                .build());

        return new AuthResponse(jwtUtil.generateToken(founder));
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UnauthorizedException("Credenciais inválidas."));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Credenciais inválidas.");
        }

        return new AuthResponse(jwtUtil.generateToken(user));
    }
}
