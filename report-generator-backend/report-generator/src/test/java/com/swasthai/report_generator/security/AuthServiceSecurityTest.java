package com.swasthai.report_generator.security;

import com.swasthai.report_generator.auth.dto.request.LoginRequest;
import com.swasthai.report_generator.auth.dto.request.RefreshTokenRequest;
import com.swasthai.report_generator.auth.dto.response.LoginResponse;
import com.swasthai.report_generator.auth.entity.RefreshToken;
import com.swasthai.report_generator.security.jwt.JwtService;
import com.swasthai.report_generator.auth.repository.RefreshTokenRepository;
import com.swasthai.report_generator.auth.service.CurrentUserService;
import com.swasthai.report_generator.auth.service.impl.AuthServiceImpl;
import com.swasthai.report_generator.common.exception.ForbiddenException;
import com.swasthai.report_generator.common.security.RateLimiterService;
import com.swasthai.report_generator.organization.entity.Organization;
import com.swasthai.report_generator.organization.entity.OrganizationStatus;
import com.swasthai.report_generator.user.entity.Role;
import com.swasthai.report_generator.user.entity.User;
import com.swasthai.report_generator.user.entity.UserStatus;
import com.swasthai.report_generator.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceSecurityTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private CurrentUserService currentUserService;

    private RateLimiterService rateLimiterService;
    private AuthServiceImpl authService;

    private Organization activeOrg;
    private Organization suspendedOrg;
    private User activeUser;
    private User otherUser;

    @BeforeEach
    void setUp() {
        rateLimiterService = new RateLimiterService();
        authService = new AuthServiceImpl(
                userRepository,
                passwordEncoder,
                jwtService,
                refreshTokenRepository,
                rateLimiterService,
                currentUserService
        );

        activeOrg = Organization.builder()
                .id(UUID.randomUUID())
                .refId("ORG-active1234")
                .name("Active Diagnostic")
                .code("ACT-01")
                .status(OrganizationStatus.ACTIVE)
                .build();

        suspendedOrg = Organization.builder()
                .id(UUID.randomUUID())
                .refId("ORG-susp12345")
                .name("Suspended Diagnostic")
                .code("SUS-01")
                .status(OrganizationStatus.SUSPENDED)
                .build();

        activeUser = User.builder()
                .id(UUID.randomUUID())
                .refId("USR-user12345")
                .email("doctor@activediag.com")
                .passwordHash("hashed_pwd")
                .name("Dr. Active")
                .role(Role.ORG_ADMIN)
                .status(UserStatus.ACTIVE)
                .organization(activeOrg)
                .build();

        otherUser = User.builder()
                .id(UUID.randomUUID())
                .refId("USR-other6789")
                .email("other@activediag.com")
                .passwordHash("hashed_pwd2")
                .name("Dr. Other")
                .role(Role.LAB_STAFF)
                .status(UserStatus.ACTIVE)
                .organization(activeOrg)
                .build();
    }

    @Test
    @DisplayName("Login: Executes dummy BCrypt hash when email not found to prevent timing attack")
    void testLoginExecutesDummyHashWhenEmailNotFoundToPreventTimingAttack() {
        LoginRequest request = new LoginRequest();
        request.setEmail("unknown@domain.com");
        request.setPassword("SomePassword123!");

        when(userRepository.findByEmailIgnoreCase("unknown@domain.com")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> authService.login(request));

        // Verify passwordEncoder.matches was called with dummy hash to prevent timing oracle
        verify(passwordEncoder).matches(eq("SomePassword123!"), contains("$2a$12$"));
    }

    @Test
    @DisplayName("Login: Rejects authentication when organization is suspended")
    void testLoginRejectsSuspendedOrganization() {
        User userInSuspendedOrg = User.builder()
                .id(UUID.randomUUID())
                .refId("USR-susp12345")
                .email("doctor@suspended.com")
                .passwordHash("hashed_pwd")
                .name("Dr. Suspended")
                .role(Role.ORG_ADMIN)
                .status(UserStatus.ACTIVE)
                .organization(suspendedOrg)
                .build();

        LoginRequest request = new LoginRequest();
        request.setEmail("doctor@suspended.com");
        request.setPassword("ValidPassword123!");

        when(userRepository.findByEmailIgnoreCase("doctor@suspended.com"))
                .thenReturn(Optional.of(userInSuspendedOrg));
        when(passwordEncoder.matches("ValidPassword123!", "hashed_pwd")).thenReturn(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> authService.login(request));
        assertEquals("Organization account is not active.", ex.getMessage());
    }

    @Test
    @DisplayName("Login: Rejects authentication when user is inactive")
    void testLoginRejectsInactiveUser() {
        User inactiveUser = User.builder()
                .id(UUID.randomUUID())
                .refId("USR-inact1234")
                .email("doctor@inactive.com")
                .passwordHash("hashed_pwd")
                .name("Dr. Inactive")
                .role(Role.LAB_STAFF)
                .status(UserStatus.INACTIVE)
                .organization(activeOrg)
                .build();

        LoginRequest request = new LoginRequest();
        request.setEmail("doctor@inactive.com");
        request.setPassword("ValidPassword123!");

        when(userRepository.findByEmailIgnoreCase("doctor@inactive.com"))
                .thenReturn(Optional.of(inactiveUser));
        when(passwordEncoder.matches("ValidPassword123!", "hashed_pwd")).thenReturn(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> authService.login(request));
        assertEquals("User account is not active.", ex.getMessage());
    }

    @Test
    @DisplayName("Refresh: Token reuse detection invalidates full token family with PESSIMISTIC_WRITE lock")
    void testRefreshTokenReuseTriggersFullTokenFamilyRevocation() {
        RefreshToken rotatedToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .refId("RT-rotated123")
                .user(activeUser)
                .tokenHash("hash_of_old_token")
                .expiresAt(Instant.now().plusSeconds(3600))
                .rotatedAt(Instant.now().minusSeconds(100)) // Already rotated!
                .build();

        when(refreshTokenRepository.findByTokenHashForUpdate(anyString()))
                .thenReturn(Optional.of(rotatedToken));

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("raw_compromised_token");

        // Attempting to use an already-rotated token indicates token theft (RFC 6819)
        SecurityException ex = assertThrows(SecurityException.class, () -> authService.refreshAccessToken(request));
        assertEquals("Invalid or expired refresh token.", ex.getMessage());

        // Verify that ALL active tokens for this user were revoked
        verify(refreshTokenRepository).revokeAllActiveByUserId(eq(activeUser.getId()), any(Instant.class));
    }

    @Test
    @DisplayName("Refresh: Valid refresh token rotates cleanly and updates rotatedAt atomically")
    void testValidRefreshTokenRotatesCleanly() {
        RefreshToken validToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .refId("RT-valid12345")
                .user(activeUser)
                .tokenHash("hash_of_valid_token")
                .expiresAt(Instant.now().plusSeconds(3600))
                .rotatedAt(null)
                .revokedAt(null)
                .build();

        when(refreshTokenRepository.findByTokenHashForUpdate(anyString()))
                .thenReturn(Optional.of(validToken));
        when(jwtService.generateAccessToken(activeUser)).thenReturn("new_jwt_access_token");
        when(jwtService.getAccessTokenExpirationSeconds()).thenReturn(900L);

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("raw_valid_refresh_token");

        LoginResponse response = authService.refreshAccessToken(request);

        assertNotNull(response);
        assertEquals("new_jwt_access_token", response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        assertNotNull(validToken.getRotatedAt(), "Old token must be marked as rotated");
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Logout: User revokes their own refresh token successfully")
    void testLogout_OwnTokenRevokedSuccessfully() {
        RefreshToken token = RefreshToken.builder()
                .id(UUID.randomUUID())
                .refId("RT-mytoken")
                .user(activeUser)
                .tokenHash("some_token_hash")
                .expiresAt(Instant.now().plusSeconds(3600))
                .revokedAt(null)
                .build();

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));
        when(currentUserService.getCurrentUser()).thenReturn(activeUser);

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("my_valid_refresh_token");

        authService.logout(request);

        assertNotNull(token.getRevokedAt());
        verify(refreshTokenRepository).save(token);
    }

    @Test
    @DisplayName("Logout: User attempting to revoke another user's refresh token is denied with 403 ForbiddenException")
    void testLogout_CannotRevokeAnotherUsersToken() {
        RefreshToken tokenBelongingToOtherUser = RefreshToken.builder()
                .id(UUID.randomUUID())
                .refId("RT-victim-token")
                .user(otherUser) // Belongs to otherUser
                .tokenHash("some_other_hash")
                .expiresAt(Instant.now().plusSeconds(3600))
                .revokedAt(null)
                .build();

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(tokenBelongingToOtherUser));
        when(currentUserService.getCurrentUser()).thenReturn(activeUser); // Current user is activeUser

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("victims_refresh_token");

        assertThrows(ForbiddenException.class, () -> authService.logout(request));
        assertNull(tokenBelongingToOtherUser.getRevokedAt(), "Victim token must NOT be revoked");
        verify(refreshTokenRepository, never()).save(tokenBelongingToOtherUser);
    }

    @Test
    @DisplayName("Logout: Nonexistent or invalid token returns safe idempotent response without error")
    void testLogout_NonexistentTokenReturnsSafely() {
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("nonexistent_refresh_token");

        assertDoesNotThrow(() -> authService.logout(request));
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("Logout: Already revoked token is idempotent and returns safely")
    void testLogout_AlreadyRevokedTokenIsIdempotent() {
        Instant revokedTime = Instant.now().minusSeconds(100);
        RefreshToken alreadyRevoked = RefreshToken.builder()
                .id(UUID.randomUUID())
                .refId("RT-revoked")
                .user(activeUser)
                .tokenHash("revoked_hash")
                .expiresAt(Instant.now().plusSeconds(3600))
                .revokedAt(revokedTime)
                .build();

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(alreadyRevoked));
        when(currentUserService.getCurrentUser()).thenReturn(activeUser);

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("already_revoked_token");

        assertDoesNotThrow(() -> authService.logout(request));
        assertEquals(revokedTime, alreadyRevoked.getRevokedAt());
        verify(refreshTokenRepository, never()).save(alreadyRevoked);
    }

    @Test
    @DisplayName("Rate Limiting: Exceeding login attempts triggers throttling")
    void testLoginRateLimitingThrottlesAfterThreshold() {
        LoginRequest request = new LoginRequest();
        request.setEmail("target@swasthai.com");
        request.setPassword("Attempt123!");

        when(userRepository.findByEmailIgnoreCase("target@swasthai.com")).thenReturn(Optional.empty());

        // Perform 5 attempts (allowed)
        for (int i = 0; i < 5; i++) {
            assertThrows(IllegalArgumentException.class, () -> authService.login(request));
        }

        // 6th attempt should be blocked by rate limiter
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> authService.login(request));
        assertTrue(ex.getMessage().contains("Too many login attempts"));
    }
}
