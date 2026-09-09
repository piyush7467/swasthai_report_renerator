package com.swasthai.report_generator.auth.service;

import com.swasthai.report_generator.auth.dto.request.LoginRequest;
import com.swasthai.report_generator.auth.dto.request.RefreshTokenRequest;
import com.swasthai.report_generator.auth.dto.response.LoginResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    default LoginResponse login(LoginRequest request, String clientIp) {
        return login(request);
    }

    LoginResponse refreshAccessToken(
            RefreshTokenRequest request
    );

    default LoginResponse refreshAccessToken(
            RefreshTokenRequest request,
            String clientIp
    ) {
        return refreshAccessToken(request);
    }

    void logout(
            RefreshTokenRequest request
    );
}