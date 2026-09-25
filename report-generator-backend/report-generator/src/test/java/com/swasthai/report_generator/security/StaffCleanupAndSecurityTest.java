package com.swasthai.report_generator.security;

import com.swasthai.report_generator.auth.dto.request.LoginRequest;
import com.swasthai.report_generator.auth.dto.request.RefreshTokenRequest;
import com.swasthai.report_generator.auth.entity.RefreshToken;
import com.swasthai.report_generator.auth.repository.PasswordResetTokenRepository;
import com.swasthai.report_generator.auth.repository.RefreshTokenRepository;
import com.swasthai.report_generator.auth.service.CurrentUserService;
import com.swasthai.report_generator.auth.service.impl.AuthServiceImpl;
import com.swasthai.report_generator.common.security.RateLimiterService;
import com.swasthai.report_generator.organization.entity.Organization;
import com.swasthai.report_generator.organization.entity.OrganizationStatus;
import com.swasthai.report_generator.security.jwt.JwtAuthenticationFilter;
import com.swasthai.report_generator.security.jwt.JwtService;
import com.swasthai.report_generator.user.config.StaffCleanupProperties;
import com.swasthai.report_generator.user.entity.Role;
import com.swasthai.report_generator.user.entity.User;
import com.swasthai.report_generator.user.entity.UserStatus;
import com.swasthai.report_generator.user.repository.UserRepository;
import com.swasthai.report_generator.user.service.StaffCleanupBatchExecutor;
import com.swasthai.report_generator.user.service.StaffCleanupService;
import com.swasthai.report_generator.user.service.impl.StaffCleanupBatchExecutorImpl;
import com.swasthai.report_generator.user.service.impl.StaffCleanupServiceImpl;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import jakarta.persistence.EntityManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StaffCleanupAndSecurityTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private EntityManager entityManager;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private RateLimiterService rateLimiterService;

    @Mock
    private CurrentUserService currentUserService;

    private StaffCleanupBatchExecutor batchExecutor;
    private StaffCleanupService staffCleanupService;
    private StaffCleanupProperties cleanupProperties;

    private Organization org;
    private User inactiveStaff;

    @BeforeEach
    void setUp() {
        batchExecutor = new StaffCleanupBatchExecutorImpl(
                userRepository,
                refreshTokenRepository,
                passwordResetTokenRepository,
                entityManager
        );

        cleanupProperties = new StaffCleanupProperties();
        cleanupProperties.setEnabled(true);
        cleanupProperties.setRetentionDays(10);
        cleanupProperties.setBatchSize(50);

        staffCleanupService = new StaffCleanupServiceImpl(cleanupProperties, batchExecutor);

        org = Organization.builder()
                .id(UUID.randomUUID())
                .refId("ORG-ALPHA")
                .name("Alpha Lab")
                .status(OrganizationStatus.ACTIVE)
                .build();

        inactiveStaff = User.builder()
                .id(UUID.randomUUID())
                .refId("USR-INACTIVE-1")
                .name("Jane Doe")
                .email("jane@alpha.com")
                .role(Role.LAB_STAFF)
                .status(UserStatus.INACTIVE)
                .organization(org)
                .inactiveAt(Instant.now().minus(11, ChronoUnit.DAYS))
                .build();
    }

    // ============================================================
    // 1. CLEANUP TESTS (10-DAY RETENTION & PERMANENT PURGE)
    // ============================================================

    @Test
    @DisplayName("Cleanup purges inactive staff accounts eligible after 10 days")
    void cleanup_purgesEligibleInactiveStaff_after10Days() {
        Slice<User> eligibleSlice = new SliceImpl<>(List.of(inactiveStaff));
        when(userRepository.findEligibleForCleanup(eq(Role.LAB_STAFF), eq(UserStatus.INACTIVE), any(Instant.class), any(Pageable.class)))
                .thenReturn(eligibleSlice);

        int purgedCount = staffCleanupService.purgeEligibleInactiveStaff();

        assertThat(purgedCount).isEqualTo(1);
        verify(refreshTokenRepository).deleteByUser_Id(inactiveStaff.getId());
        verify(passwordResetTokenRepository).deleteByUser_Id(inactiveStaff.getId());
        verify(userRepository).delete(inactiveStaff);
        verify(userRepository).flush();
        verify(entityManager).clear();
    }

    @Test
    @DisplayName("Cleanup does not purge inactive staff deactivated less than 10 days ago")
    void cleanup_doesNotPurge_whenDeactivatedLessThan10Days() {
        // Query returns empty because cutoff filter (now - 10 days) excludes recent deactivations
        when(userRepository.findEligibleForCleanup(eq(Role.LAB_STAFF), eq(UserStatus.INACTIVE), any(Instant.class), any(Pageable.class)))
                .thenReturn(new SliceImpl<>(Collections.emptyList()));

        int purgedCount = staffCleanupService.purgeEligibleInactiveStaff();

        assertThat(purgedCount).isEqualTo(0);
        verify(userRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Cleanup is idempotent: multiple executions handle empty remaining slices safely")
    void cleanup_isIdempotent() {
        when(userRepository.findEligibleForCleanup(eq(Role.LAB_STAFF), eq(UserStatus.INACTIVE), any(Instant.class), any(Pageable.class)))
                .thenReturn(new SliceImpl<>(Collections.emptyList()));

        int firstRun = staffCleanupService.purgeEligibleInactiveStaff();
        int secondRun = staffCleanupService.purgeEligibleInactiveStaff();

        assertThat(firstRun).isEqualTo(0);
        assertThat(secondRun).isEqualTo(0);
        verify(userRepository, never()).delete(any());
    }

    // ============================================================
    // 2. IMMEDIATE JWT & REFRESH TOKEN ACCESS BLOCK TESTS
    // ============================================================

    @Test
    @DisplayName("JwtAuthenticationFilter immediately rejects requests if user status in DB is INACTIVE (even with valid JWT)")
    void jwtFilter_immediatelyRejectsRequest_whenUserIsInactiveInDatabase() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, userRepository);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        when(request.getHeader("Authorization")).thenReturn("Bearer valid.signed.jwt");

        @SuppressWarnings("unchecked")
        Jws<Claims> jwsClaims = mock(Jws.class);
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("USR-INACTIVE-1");
        when(jwsClaims.getPayload()).thenReturn(claims);
        when(jwtService.validateToken("valid.signed.jwt")).thenReturn(jwsClaims);

        // Database lookup returns user with INACTIVE status
        when(userRepository.findByRefIdWithOrganization("USR-INACTIVE-1")).thenReturn(Optional.of(inactiveStaff));

        SecurityContextHolder.getContext().setAuthentication(null);

        filter.doFilter(request, response, filterChain);

        // Context must NOT be set with authentication
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("AuthService login rejects inactive user accounts")
    void login_fails_whenUserIsInactive() {
        AuthServiceImpl authService = new AuthServiceImpl(
                userRepository,
                passwordEncoder,
                jwtService,
                refreshTokenRepository,
                rateLimiterService,
                currentUserService
        );

        LoginRequest loginRequest = new LoginRequest("jane@alpha.com", "password123");
        when(rateLimiterService.isAllowed(anyString(), anyInt(), anyLong(), anyLong())).thenReturn(true);
        when(userRepository.findByEmailIgnoreCase("jane@alpha.com")).thenReturn(Optional.of(inactiveStaff));
        when(passwordEncoder.matches("password123", inactiveStaff.getPasswordHash())).thenReturn(true);

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("User account is not active");
    }

    @Test
    @DisplayName("AuthService refreshAccessToken rejects refresh attempts for inactive accounts")
    void refreshAccessToken_fails_whenUserIsInactive() {
        AuthServiceImpl authService = new AuthServiceImpl(
                userRepository,
                passwordEncoder,
                jwtService,
                refreshTokenRepository,
                rateLimiterService,
                currentUserService
        );

        RefreshToken storedToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .user(inactiveStaff)
                .tokenHash("hash123")
                .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .build();

        when(rateLimiterService.isAllowed(anyString(), anyInt(), anyLong())).thenReturn(true);
        when(refreshTokenRepository.findByTokenHashForUpdate(anyString())).thenReturn(Optional.of(storedToken));

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("someRawRefreshToken");

        assertThatThrownBy(() -> authService.refreshAccessToken(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("User account is not active");
    }
}
