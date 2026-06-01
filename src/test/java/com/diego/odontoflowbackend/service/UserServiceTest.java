package com.diego.odontoflowbackend.service;

import com.diego.odontoflowbackend.entity.Subscription;
import com.diego.odontoflowbackend.entity.User;
import com.diego.odontoflowbackend.entity.dto.request.InviteUserRequest;
import com.diego.odontoflowbackend.entity.dto.response.UserResponse;
import com.diego.odontoflowbackend.entity.enums.Plan;
import com.diego.odontoflowbackend.entity.enums.Role;
import com.diego.odontoflowbackend.entity.enums.SubscriptionStatus;
import com.diego.odontoflowbackend.exception.BadRequestException;
import com.diego.odontoflowbackend.exception.ConflictException;
import com.diego.odontoflowbackend.exception.PlanLimitExceededException;
import com.diego.odontoflowbackend.repository.SubscriptionRepository;
import com.diego.odontoflowbackend.repository.UserRepository;
import com.diego.odontoflowbackend.security.SecurityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock SubscriptionRepository subscriptionRepository;
    @Mock PasswordEncoder passwordEncoder;
    @InjectMocks UserService service;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID userId   = UUID.randomUUID();
    private MockedStatic<SecurityUtils> security;

    @BeforeEach
    void setUp() {
        security = mockStatic(SecurityUtils.class);
        security.when(SecurityUtils::currentTenantId).thenReturn(tenantId);
    }

    @AfterEach
    void tearDown() { security.close(); }

    private Subscription sub(Plan plan) {
        return Subscription.builder().tenantId(tenantId).plan(plan).status(SubscriptionStatus.ACTIVE).build();
    }

    @Test
    void invite_receptionist_succeeds() {
        when(userRepository.existsByTenantIdAndEmail(tenantId, "rec@c.com")).thenReturn(false);
        when(passwordEncoder.encode("senha1234")).thenReturn("hash");
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        UserResponse res = service.invite(new InviteUserRequest("Recepção", "rec@c.com", Role.RECEPTIONIST, "senha1234"));

        assertThat(res.role()).isEqualTo(Role.RECEPTIONIST);
        // receptionists do not consume the dentist limit
        verify(subscriptionRepository, never()).findByTenantId(any());
    }

    @Test
    void invite_duplicateEmail_throwsConflict() {
        when(userRepository.existsByTenantIdAndEmail(tenantId, "dup@c.com")).thenReturn(true);

        assertThatThrownBy(() -> service.invite(new InviteUserRequest("Dup", "dup@c.com", Role.RECEPTIONIST, "senha1234")))
                .isInstanceOf(ConflictException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void invite_dentist_overFreeLimit_throwsPlanLimit() {
        when(userRepository.existsByTenantIdAndEmail(tenantId, "dent@c.com")).thenReturn(false);
        when(subscriptionRepository.findByTenantId(tenantId)).thenReturn(Optional.of(sub(Plan.FREE)));
        when(userRepository.countByTenantIdAndRole(tenantId, Role.DENTIST)).thenReturn(1L); // FREE allows 1

        assertThatThrownBy(() -> service.invite(new InviteUserRequest("Dr. Novo", "dent@c.com", Role.DENTIST, "senha1234")))
                .isInstanceOf(PlanLimitExceededException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void invite_dentist_withinEssentialLimit_succeeds() {
        when(userRepository.existsByTenantIdAndEmail(tenantId, "dent@c.com")).thenReturn(false);
        when(subscriptionRepository.findByTenantId(tenantId)).thenReturn(Optional.of(sub(Plan.ESSENTIAL)));
        when(userRepository.countByTenantIdAndRole(tenantId, Role.DENTIST)).thenReturn(1L); // ESSENTIAL allows 3
        when(passwordEncoder.encode(any())).thenReturn("hash");
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        UserResponse res = service.invite(new InviteUserRequest("Dr. Novo", "dent@c.com", Role.DENTIST, "senha1234"));

        assertThat(res.role()).isEqualTo(Role.DENTIST);
    }

    @Test
    void remove_self_throwsBadRequest() {
        security.when(SecurityUtils::currentUserId).thenReturn(userId);

        assertThatThrownBy(() -> service.remove(userId)).isInstanceOf(BadRequestException.class);
        verify(userRepository, never()).delete(any());
    }
}
