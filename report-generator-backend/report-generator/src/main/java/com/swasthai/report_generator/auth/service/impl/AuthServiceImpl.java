package com.swasthai.report_generator.auth.service.impl;

import com.swasthai.report_generator.auth.dto.request.LoginRequest;
import com.swasthai.report_generator.auth.dto.request.RefreshTokenRequest;
import com.swasthai.report_generator.auth.dto.response.LoginResponse;
import com.swasthai.report_generator.auth.entity.RefreshToken;
import com.swasthai.report_generator.security.jwt.JwtService;
import com.swasthai.report_generator.auth.repository.RefreshTokenRepository;
import com.swasthai.report_generator.auth.service.AuthService;
import com.swasthai.report_generator.common.exception.ResourceNotFoundException;
import com.swasthai.report_generator.common.security.RateLimiterService;
import com.swasthai.report_generator.user.entity.Role;
import com.swasthai.report_generator.user.entity.User;
import com.swasthai.report_generator.user.entity.UserStatus;
import com.swasthai.report_generator.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.swasthai.report_generator.auth.config.AuthRateLimitProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private static final long REFRESH_TOKEN_EXPIRATION_SECONDS =
            60L * 60 * 24 * 30; // 30 days

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private static final String DUMMY_BCRYPT_HASH =
            "$2a$12$e8wDcw0h.u1WvHl0uJvO.u1Y2Z9oP.o9V8g7f6e5d4c3b2a1Z0Y9X";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RateLimiterService rateLimiterService;
    private final com.swasthai.report_generator.auth.service.CurrentUserService currentUserService;
    private final AuthRateLimitProperties authRateLimitProperties;

    @Autowired
    public AuthServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenRepository refreshTokenRepository,
            RateLimiterService rateLimiterService,
            com.swasthai.report_generator.auth.service.CurrentUserService currentUserService,
            @Autowired(required = false) AuthRateLimitProperties authRateLimitProperties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.rateLimiterService = rateLimiterService;
        this.currentUserService = currentUserService;
        this.authRateLimitProperties = authRateLimitProperties != null ? authRateLimitProperties : new AuthRateLimitProperties();
    }

    public AuthServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenRepository refreshTokenRepository,
            RateLimiterService rateLimiterService,
            com.swasthai.report_generator.auth.service.CurrentUserService currentUserService) {
        this(userRepository, passwordEncoder, jwtService, refreshTokenRepository, rateLimiterService, currentUserService, new AuthRateLimitProperties());
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        return login(request, null);
    }

    @Override
    public LoginResponse login(LoginRequest request, String clientIp) {

        String email = request.getEmail()
                .trim()
                .toLowerCase();

        int maxAttempts = authRateLimitProperties.getMaxAttempts();
        long windowSec = authRateLimitProperties.getWindowSeconds();
        long lockSec = authRateLimitProperties.getLockSeconds();

        String emailKey = "login:email:" + email;
        String ipKey = clientIp != null && !clientIp.isBlank() ? "login:ip:" + clientIp.trim() : null;
        String compositeKey = ipKey != null ? "login:composite:" + email + ":" + clientIp.trim() : null;

        if (ipKey != null && !rateLimiterService.isAllowed(ipKey, maxAttempts * 2, windowSec, lockSec)) {
            throw new IllegalStateException("Too many login attempts from this network. Please try again later.");
        }

        if (!rateLimiterService.isAllowed(emailKey, maxAttempts, windowSec, lockSec)) {
            throw new IllegalStateException("Too many login attempts for this account. Please try again in 5 minutes.");
        }

        if (compositeKey != null && !rateLimiterService.isAllowed(compositeKey, maxAttempts, windowSec, lockSec)) {
            throw new IllegalStateException("Too many login attempts. Please try again in 5 minutes.");
        }

        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);

        if (user == null) {
            // Mitigate timing attack by executing dummy password comparison
            passwordEncoder.matches(request.getPassword(), DUMMY_BCRYPT_HASH);
            throw new IllegalArgumentException("Invalid email or password.");
        }

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPasswordHash()
        )) {
            throw new IllegalArgumentException(
                    "Invalid email or password."
            );
        }

        validateAccountActive(user);

        // Reset rate limiters on successful login
        rateLimiterService.reset(emailKey);
        if (ipKey != null) {
            rateLimiterService.reset(ipKey);
        }
        if (compositeKey != null) {
            rateLimiterService.reset(compositeKey);
        }

        Instant now = Instant.now();

        user.setLastLoginAt(now);
        userRepository.save(user);

        String accessToken =
                jwtService.generateAccessToken(user);

        String refreshToken =
                generateRefreshToken();

        saveRefreshToken(
                user,
                refreshToken
        );

        Instant accessTokenExpiresAt =
                now.plusSeconds(
                        jwtService.getAccessTokenExpirationSeconds()
                );

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .accessTokenExpiresAt(accessTokenExpiresAt)
                .userRefId(user.getRefId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .organizationRefId(
                        user.getOrganization() != null
                                ? user.getOrganization().getRefId()
                                : null
                )
                .build();
    }

    @Override
    public LoginResponse refreshAccessToken(
            RefreshTokenRequest request
    ) {
        return refreshAccessToken(request, null);
    }

    @Override
    public LoginResponse refreshAccessToken(
            RefreshTokenRequest request,
            String clientIp
    ) {

        String rawRefreshToken =
                request.getRefreshToken().trim();

        String tokenHash =
                hashToken(rawRefreshToken);

        if (clientIp != null && !rateLimiterService.isAllowed("refresh:ip:" + clientIp, 20, 60)) {
            throw new IllegalStateException("Too many refresh attempts from this network. Please try again later.");
        }

        if (!rateLimiterService.isAllowed("refresh:token:" + tokenHash, 10, 60)) {
            throw new IllegalStateException("Too many refresh attempts. Please try again later.");
        }

        // PESSIMISTIC_WRITE lock prevents concurrent refresh race conditions
        RefreshToken storedToken = refreshTokenRepository.findByTokenHashForUpdate(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired refresh token."));

        // RFC 6819: Detect Refresh Token Reuse (token theft)
        if (storedToken.getRotatedAt() != null) {
            log.warn("SECURITY ALERT: Compromised refresh token reuse detected for user ID: {}. Invalidating token family.",
                    storedToken.getUser().getId());
            refreshTokenRepository.revokeAllActiveByUserId(storedToken.getUser().getId(), Instant.now());
            throw new SecurityException("Invalid or expired refresh token.");
        }

        if (storedToken.getRevokedAt() != null || storedToken.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Invalid or expired refresh token.");
        }

        User user = storedToken.getUser();
        validateAccountActive(user);

        /*
         * Refresh-token rotation.
         * The old refresh token becomes unusable as soon as it is exchanged for a new one.
         */
        storedToken.setRotatedAt(Instant.now());

        String newRefreshToken =
                generateRefreshToken();

        saveRefreshToken(
                user,
                newRefreshToken
        );

        String newAccessToken =
                jwtService.generateAccessToken(user);

        Instant accessTokenExpiresAt =
                Instant.now().plusSeconds(
                        jwtService.getAccessTokenExpirationSeconds()
                );

        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .accessTokenExpiresAt(accessTokenExpiresAt)
                .userRefId(user.getRefId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .organizationRefId(
                        user.getOrganization() != null
                                ? user.getOrganization().getRefId()
                                : null
                )
                .build();
    }

    @Override
    public void logout(
            RefreshTokenRequest request
    ) {

        String rawRefreshToken =
                request.getRefreshToken().trim();

        String tokenHash =
                hashToken(rawRefreshToken);

        RefreshToken refreshToken =
                refreshTokenRepository
                        .findByTokenHash(tokenHash)
                        .orElse(null);

        // Safe idempotent response if token does not exist (does not leak token validity)
        if (refreshToken == null) {
            return;
        }

        // Verify token ownership against authenticated user
        User currentUser = currentUserService.getCurrentUser();
        if (!refreshToken.getUser().getId().equals(currentUser.getId())) {
            log.warn("SECURITY ALERT: User {} attempted to revoke refresh token belonging to another user {}",
                    currentUser.getId(), refreshToken.getUser().getId());
            throw new com.swasthai.report_generator.common.exception.ForbiddenException(
                    "Cannot revoke a refresh token belonging to another user."
            );
        }

        if (refreshToken.getRevokedAt() == null) {
            refreshToken.setRevokedAt(
                    Instant.now()
            );
            refreshTokenRepository.save(refreshToken);
        }
    }

    private void validateAccountActive(User user) {
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalStateException("User account is not active.");
        }

        if (user.getRole() != Role.SUPER_ADMIN) {
            if (user.getOrganization() == null) {
                throw new IllegalStateException("User is not associated with an organization.");
            }
            if (user.getOrganization().getStatus() != com.swasthai.report_generator.organization.entity.OrganizationStatus.ACTIVE) {
                throw new IllegalStateException("Organization account is not active.");
            }
        }
    }

    private void saveRefreshToken(
            User user,
            String rawRefreshToken
    ) {

        Instant now = Instant.now();

        RefreshToken refreshToken =
                RefreshToken.builder()
                        .user(user)
                        .tokenHash(
                                hashToken(rawRefreshToken)
                        )
                        .expiresAt(
                                now.plusSeconds(
                                        REFRESH_TOKEN_EXPIRATION_SECONDS
                                )
                        )
                        .build();

        refreshTokenRepository.save(refreshToken);
    }

    private String generateRefreshToken() {

        byte[] randomBytes = new byte[64];

        SECURE_RANDOM.nextBytes(randomBytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);
    }

    private String hashToken(String token) {

        try {

            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash =
                    digest.digest(
                            token.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(hash);

        } catch (NoSuchAlgorithmException exception) {

            throw new IllegalStateException(
                    "SHA-256 algorithm is not available.",
                    exception
            );
        }
    }
}