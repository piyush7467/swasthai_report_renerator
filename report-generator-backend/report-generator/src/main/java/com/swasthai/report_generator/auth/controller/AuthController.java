package com.swasthai.report_generator.auth.controller;

import com.swasthai.report_generator.auth.dto.request.LoginRequest;
import com.swasthai.report_generator.auth.dto.request.RefreshTokenRequest;
import com.swasthai.report_generator.auth.dto.response.LoginResponse;
import com.swasthai.report_generator.auth.service.AuthService;
import com.swasthai.report_generator.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

        private final AuthService authService;

        @GetMapping("/health")
        public ResponseEntity<String> health() {
                return ResponseEntity.ok("UP");
        }

        @PostMapping("/login")
        public ResponseEntity<ApiResponse<LoginResponse>> login(
                        @Valid @RequestBody LoginRequest request,
                        jakarta.servlet.http.HttpServletRequest httpRequest) {

                String clientIp = extractClientIp(httpRequest);
                LoginResponse response = authService.login(request, clientIp);

                return ResponseEntity.ok(
                                ApiResponse.<LoginResponse>builder()
                                                .success(true)
                                                .message("Login successful.")
                                                .data(response)
                                                .build());
        }

        @PostMapping("/refresh")
        public ResponseEntity<ApiResponse<LoginResponse>> refresh(
                        @Valid @RequestBody RefreshTokenRequest request,
                        jakarta.servlet.http.HttpServletRequest httpRequest) {

                String clientIp = extractClientIp(httpRequest);
                LoginResponse response = authService.refreshAccessToken(request, clientIp);

                return ResponseEntity.ok(
                                ApiResponse.<LoginResponse>builder()
                                                .success(true)
                                                .message("Access token refreshed successfully.")
                                                .data(response)
                                                .build());
        }

        private String extractClientIp(jakarta.servlet.http.HttpServletRequest request) {
                if (request == null) {
                        return null;
                }
                // Rely exclusively on verified peer socket address to prevent spoofed
                // X-Forwarded-For rate-limit bypass
                return request.getRemoteAddr();
        }

        @PostMapping("/logout")
        public ResponseEntity<ApiResponse<Void>> logout(
                        @Valid @RequestBody RefreshTokenRequest request) {

                authService.logout(request);

                return ResponseEntity.ok(
                                ApiResponse.<Void>builder()
                                                .success(true)
                                                .message("Logout successful.")
                                                .data(null)
                                                .build());
        }
}