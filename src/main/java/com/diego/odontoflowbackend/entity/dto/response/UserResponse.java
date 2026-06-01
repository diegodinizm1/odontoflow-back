package com.diego.odontoflowbackend.entity.dto.response;

import com.diego.odontoflowbackend.entity.User;
import com.diego.odontoflowbackend.entity.enums.Role;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String fullName,
        String email,
        Role role
) {
    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getFullName(), u.getEmail(), u.getRole());
    }
}
