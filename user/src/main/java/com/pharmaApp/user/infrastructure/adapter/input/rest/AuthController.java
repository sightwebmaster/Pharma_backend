package com.pharmaApp.user.infrastructure.adapter.input.rest;

import com.pharmaApp.user.application.dto.request.LoginRequest;
import com.pharmaApp.user.application.dto.request.RegisterPatientRequest;
import com.pharmaApp.user.application.dto.request.ChangePasswordRequest;
import com.pharmaApp.user.application.dto.response.AuthResponse;
import com.pharmaApp.user.application.dto.response.PatientProfileResponse;
import com.pharmaApp.user.domain.port.input.AuthUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthUseCase authUseCase;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterPatientRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(authUseCase.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authUseCase.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<PatientProfileResponse> me(
            @RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.ok(authUseCase.getMe(authHeader));
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody ChangePasswordRequest request) {
        authUseCase.changePassword(userId, request);
        return ResponseEntity.noContent().build();
    }
}
