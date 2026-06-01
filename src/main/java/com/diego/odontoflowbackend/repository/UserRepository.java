package com.diego.odontoflowbackend.repository;

import com.diego.odontoflowbackend.entity.User;
import com.diego.odontoflowbackend.entity.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByTenantIdAndEmail(UUID tenantId, String email);
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    List<User> findByTenantIdOrderByCreatedAtAsc(UUID tenantId);
    Optional<User> findByIdAndTenantId(UUID id, UUID tenantId);
    boolean existsByTenantIdAndEmail(UUID tenantId, String email);
    long countByTenantIdAndRole(UUID tenantId, Role role);
}
