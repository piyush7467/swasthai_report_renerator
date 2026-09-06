package com.swasthai.report_generator.auth.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class LoginResponse {

    private String accessToken;

    private String refreshToken;

    private String tokenType;

    private Instant accessTokenExpiresAt;

    private String userRefId;

    private String name;

    private String email;

    private String role;

    private String organizationRefId;
}