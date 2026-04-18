package com.pharmaApp.user.domain.port.input;

import com.pharmaApp.user.application.dto.request.*;
import com.pharmaApp.user.application.dto.response.*;
import java.util.List;

public interface PatientProfileUseCase {
    PatientProfileResponse createProfile(String userId, CreatePatientProfileRequest request);
    PatientProfileResponse getProfile(String userId);
    PatientProfileResponse updateProfile(String userId, UpdatePatientProfileRequest request);
    void deleteProfile(String userId);
    String getQrCode(String userId);

    ProcheResponse addProcheByEmail(String userId, AddProcheByEmailRequest request);   // ✅
    ProcheResponse addProcheByQrCode(String userId, AddProcheByQrCodeRequest request); // ✅
    List<ProcheResponse> getProches(String userId);
    List<ProcheResponse> getFollowers(String userId);
    void deleteProche(String userId, String procheId);
    PatientProfileResponse getProcheProfile(String userId, String procheId);           // ✅
}
