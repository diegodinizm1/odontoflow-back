package com.diego.odontoflowbackend.entity.dto.response;

import com.diego.odontoflowbackend.entity.User;

import java.util.UUID;

public record PublicDentistResponse(UUID id, String fullName) {
    public static PublicDentistResponse from(User user) {
        return new PublicDentistResponse(user.getId(), user.getFullName());
    }
}
