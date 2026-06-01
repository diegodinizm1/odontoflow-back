package com.diego.odontoflowbackend.service;

import com.diego.odontoflowbackend.entity.Subscription;
import com.diego.odontoflowbackend.entity.User;
import com.diego.odontoflowbackend.entity.dto.request.InviteUserRequest;
import com.diego.odontoflowbackend.entity.dto.response.UserResponse;
import com.diego.odontoflowbackend.entity.enums.Plan;
import com.diego.odontoflowbackend.entity.enums.Role;
import com.diego.odontoflowbackend.exception.BadRequestException;
import com.diego.odontoflowbackend.exception.ConflictException;
import com.diego.odontoflowbackend.exception.NotFoundException;
import com.diego.odontoflowbackend.exception.PlanLimitExceededException;
import com.diego.odontoflowbackend.repository.SubscriptionRepository;
import com.diego.odontoflowbackend.repository.UserRepository;
import com.diego.odontoflowbackend.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PasswordEncoder passwordEncoder;

    public List<UserResponse> list() {
        return userRepository.findByTenantIdOrderByCreatedAtAsc(SecurityUtils.currentTenantId())
                .stream().map(UserResponse::from).toList();
    }

    @Transactional
    public UserResponse invite(InviteUserRequest request) {
        UUID tenantId = SecurityUtils.currentTenantId();

        if (userRepository.existsByTenantIdAndEmail(tenantId, request.email())) {
            throw new ConflictException("E-mail já cadastrado nesta clínica.");
        }
        if (request.role() == Role.DENTIST) {
            enforceDentistLimit(tenantId);
        }

        User user = User.builder()
                .tenantId(tenantId)
                .fullName(request.fullName())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(request.role())
                .build();

        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public void remove(UUID id) {
        UUID tenantId = SecurityUtils.currentTenantId();
        if (id.equals(SecurityUtils.currentUserId())) {
            throw new BadRequestException("Você não pode remover a si mesmo.");
        }
        User user = userRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado."));
        userRepository.delete(user);
    }

    private void enforceDentistLimit(UUID tenantId) {
        Plan plan = subscriptionRepository.findByTenantId(tenantId)
                .map(Subscription::getPlan).orElse(Plan.FREE);
        long dentists = userRepository.countByTenantIdAndRole(tenantId, Role.DENTIST);
        if (dentists >= plan.getMaxDentists()) {
            throw new PlanLimitExceededException(
                    "Limite de %d dentista(s) do plano %s atingido. Faça upgrade do plano."
                            .formatted(plan.getMaxDentists(), plan.getDisplayName()));
        }
    }
}
