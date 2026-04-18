package com.pharmaApp.user.domain.port.input;

import com.pharmaApp.user.application.dto.request.LoginRequest;
import com.pharmaApp.user.application.dto.request.RegisterPatientRequest;
import com.pharmaApp.user.application.dto.request.ChangePasswordRequest;
import com.pharmaApp.user.application.dto.response.AuthResponse;
import com.pharmaApp.user.application.dto.response.PatientProfileResponse;

public interface AuthUseCase {
    AuthResponse register(RegisterPatientRequest request);
    AuthResponse login(LoginRequest request);
    PatientProfileResponse getMe(String authHeader);
    void changePassword(String userId, ChangePasswordRequest request);
}
