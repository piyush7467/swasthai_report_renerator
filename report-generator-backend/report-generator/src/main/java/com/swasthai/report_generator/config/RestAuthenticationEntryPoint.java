package com.swasthai.report_generator.config;

import tools.jackson.databind.ObjectMapper;
import com.swasthai.report_generator.common.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

@Component
public class RestAuthenticationEntryPoint
        implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(
            ObjectMapper objectMapper
    ) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {

        ErrorResponse errorResponse =
                ErrorResponse.builder()
                        .success(false)
                        .status(HttpServletResponse.SC_UNAUTHORIZED)
                        .code("UNAUTHORIZED")
                        .message("Authentication failed.")
                        .path(request.getRequestURI())
                        .timestamp(Instant.now())
                        .errors(null)
                        .build();

        response.setStatus(
                HttpServletResponse.SC_UNAUTHORIZED
        );

        response.setContentType(
                MediaType.APPLICATION_JSON_VALUE
        );

        objectMapper.writeValue(
                response.getOutputStream(),
                errorResponse
        );
    }
}