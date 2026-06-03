package com.diego.odontoflowbackend.repository;

import com.diego.odontoflowbackend.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {
    boolean existsByDocument(String document);
    Optional<Tenant> findByDocument(String document);
    boolean existsByPublicSlug(String publicSlug);
    Optional<Tenant> findByPublicSlug(String publicSlug);
}
