package com.swasthai.report_generator.config;

import tools.jackson.databind.ObjectMapper;
import com.swasthai.report_generator.common.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

@Component
public class RestAccessDeniedHandler
        implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public RestAccessDeniedHandler(
            ObjectMapper objectMapper
    ) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {

        ErrorResponse errorResponse =
                ErrorResponse.builder()
                        .success(false)
                        .status(HttpServletResponse.SC_FORBIDDEN)
                        .code("ACCESS_DENIED")
                        .message(
                                "You do not have permission to perform this operation."
                        )
                        .path(request.getRequestURI())
                        .timestamp(Instant.now())
                        .errors(null)
                        .build();

        response.setStatus(
                HttpServletResponse.SC_FORBIDDEN
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