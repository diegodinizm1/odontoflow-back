package com.diego.odontoflowbackend.controller;

import com.diego.odontoflowbackend.entity.dto.request.ChangePasswordRequest;
import com.diego.odontoflowbackend.security.SecurityUtils;
import com.diego.odontoflowbackend.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** Self-service account actions for the authenticated user. */
@RestController
@RequestMapping("/account")
@RequiredArgsConstructor
@Tag(name = "Account", description = "Authenticated user's own account")
@SecurityRequirement(name = "bearerAuth")
public class AccountController {

    private final AuthService authService;

    @PatchMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Change my password")
    public void changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(SecurityUtils.currentUserId(), request);
    }
}
