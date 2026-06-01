package com.diego.odontoflowbackend.controller;

import com.diego.odontoflowbackend.entity.dto.request.InviteUserRequest;
import com.diego.odontoflowbackend.entity.dto.response.UserResponse;
import com.diego.odontoflowbackend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "Team", description = "Clinic team members (dentists and receptionists)")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "List team members of the tenant")
    public List<UserResponse> list() {
        return userService.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('DENTIST')")
    @Operation(summary = "Invite a team member (dentists only)")
    public UserResponse invite(@Valid @RequestBody InviteUserRequest request) {
        return userService.invite(request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('DENTIST')")
    @Operation(summary = "Remove a team member (dentists only)")
    public void remove(@PathVariable UUID id) {
        userService.remove(id);
    }
}
