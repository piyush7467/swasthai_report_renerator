package com.swasthai.report_generator.security.jwt;

import com.swasthai.report_generator.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.SecurityException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties jwtProperties;

    private PrivateKey privateKey;
    private PublicKey publicKey;

    @PostConstruct
    public void initializeKeys() {

        try {
            privateKey = loadPrivateKey(
                    jwtProperties.getPrivateKeyPath());

            publicKey = loadPublicKey(
                    jwtProperties.getPublicKeyPath());

        } catch (Exception exception) {

            throw new IllegalStateException(
                    "Failed to load JWT RSA keys.",
                    exception);
        }
    }

    /**
     * Generates an access token for the supplied user.
     */
    public String generateAccessToken(User user) {

        Instant issuedAt = Instant.now();

        Instant expirationTime = issuedAt.plusSeconds(
                jwtProperties
                        .getAccessTokenExpirationSeconds());

        String userRefId = user.getRefId();

        String organizationRefId = user.getOrganization() != null
                ? user.getOrganization().getRefId()
                : null;

        String tokenId = UUID.randomUUID().toString();

        var builder = Jwts.builder()
                .subject(userRefId)
                .issuer(jwtProperties.getIssuer())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expirationTime))
                .id(tokenId)
                .claim("role", user.getRole().name());

        /*
         * SUPER_ADMIN does not belong to an organization.
         *
         * Therefore the "org" claim is omitted for SUPER_ADMIN.
         */
        if (organizationRefId != null) {
            builder.claim("org", organizationRefId);
        }

        return builder
                .signWith(privateKey)
                .compact();
    }

    /**
     * Validates the access token and returns its claims.
     *
     * Signature, expiration and issuer are verified.
     */
    public Jws<Claims> validateToken(String token) {

        try {

            return Jwts.parser()
                    .verifyWith(publicKey)
                    .requireIssuer(
                            jwtProperties.getIssuer())
                    .build()
                    .parseSignedClaims(token);

        } catch (SecurityException | io.jsonwebtoken.ExpiredJwtException | io.jsonwebtoken.MalformedJwtException
                | io.jsonwebtoken.UnsupportedJwtException | IllegalArgumentException exception) {

            throw new IllegalArgumentException(
                    "Invalid access token.",
                    exception);
        }
    }

    /**
     * Extracts the user public reference ID from a token.
     */
    public String extractUserRefId(String token) {

        return validateToken(token)
                .getPayload()
                .getSubject();
    }

    /**
     * Extracts the role from a token.
     */
    public String extractRole(String token) {

        return validateToken(token)
                .getPayload()
                .get("role", String.class);
    }

    public long getAccessTokenExpirationSeconds() {
        return jwtProperties.getAccessTokenExpirationSeconds();
    }

    /**
     * Extracts the organization public reference ID.
     *
     * Returns null for SUPER_ADMIN.
     */
    public String extractOrganizationRefId(String token) {

        return validateToken(token)
                .getPayload()
                .get("org", String.class);
    }

    /**
     * Loads an RSA private key from a PEM file.
     */
    private PrivateKey loadPrivateKey(
            String filePath) throws Exception {

        String pem = readPemFile(filePath);

        String privateKeyContent = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");

        byte[] keyBytes = Base64.getDecoder()
                .decode(privateKeyContent);

        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        return keyFactory.generatePrivate(keySpec);
    }

    /**
     * Loads an RSA public key from a PEM file.
     */
    private PublicKey loadPublicKey(
            String filePath) throws Exception {

        String pem = readPemFile(filePath);

        String publicKeyContent = pem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");

        byte[] keyBytes = Base64.getDecoder()
                .decode(publicKeyContent);

        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        return keyFactory.generatePublic(keySpec);
    }

    /**
     * Reads a PEM file from the configured path.
     */
    private String readPemFile(
            String filePath) throws IOException {

        if (filePath == null || filePath.isBlank()) {
            throw new IllegalArgumentException(
                    "JWT key path is not configured.");
        }

        Path path = Path.of(filePath);

        if (!Files.exists(path)) {
            throw new IllegalArgumentException(
                    "JWT key file does not exist: "
                            + filePath);
        }

        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException(
                    "JWT key path is not a regular file: "
                            + filePath);
        }

        return Files.readString(
                path,
                StandardCharsets.UTF_8);
    }
}
