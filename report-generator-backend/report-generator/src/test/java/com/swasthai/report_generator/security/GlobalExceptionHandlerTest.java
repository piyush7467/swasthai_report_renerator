package com.swasthai.report_generator.security;

import com.swasthai.report_generator.common.exception.GlobalExceptionHandler;
import com.swasthai.report_generator.common.response.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/test");
    }

    @Test
    void testValidationExceptionReturnsFieldErrorsMap() throws Exception {
        Object target = new Object();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(target, "testObject");
        bindingResult.addError(new FieldError("testObject", "email", "Invalid email format"));
        bindingResult.addError(new FieldError("testObject", "name", "Name is required"));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<ErrorResponse> responseEntity = exceptionHandler.handleValidationException(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        ErrorResponse body = responseEntity.getBody();
        assertNotNull(body);
        assertFalse(body.isSuccess());
        assertEquals("VALIDATION_ERROR", body.getCode());
        assertNotNull(body.getErrors(), "Errors map must not be null");
        assertEquals(2, body.getErrors().size());
        assertEquals("Invalid email format", body.getErrors().get("email"));
        assertEquals("Name is required", body.getErrors().get("name"));
    }

    @Test
    void testIllegalArgumentReturnsBadRequest() {
        IllegalArgumentException ex = new IllegalArgumentException("Invalid input parameter");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleIllegalArgument(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("BAD_REQUEST", response.getBody().getCode());
        assertEquals("Invalid input parameter", response.getBody().getMessage());
    }

    @Test
    void testAccessDeniedReturnsForbidden() {
        AccessDeniedException ex = new AccessDeniedException("Access denied");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleAccessDenied(ex, request);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("FORBIDDEN", response.getBody().getCode());
    }

    @Test
    void testSecurityExceptionReturnsSanitizedUnauthorized() {
        SecurityException ex = new SecurityException("Token reuse detected");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleSecurityException(ex, request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("UNAUTHORIZED", response.getBody().getCode());
        assertEquals("Invalid or expired refresh token.", response.getBody().getMessage());
    }

    @Test
    void testAuthenticationExceptionReturnsGeneric401() {
        org.springframework.security.authentication.BadCredentialsException ex =
                new org.springframework.security.authentication.BadCredentialsException("Bad credentials with internal details");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleAuthenticationException(ex, request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("UNAUTHORIZED", response.getBody().getCode());
        assertEquals("Authentication failed.", response.getBody().getMessage());
    }

    @Test
    void testUnexpectedExceptionReturnsSanitized500() {
        RuntimeException ex = new RuntimeException("Database connection timeout - internal details");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleUnexpectedException(ex, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("INTERNAL_SERVER_ERROR", response.getBody().getCode());
        assertEquals("An unexpected error occurred.", response.getBody().getMessage(),
                "Internal exception details must never be exposed");
    }
}
