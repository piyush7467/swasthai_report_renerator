package com.swasthai.report_generator.security.jwt;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "security.jwt")
public class JwtProperties {

    @NotBlank(message = "security.jwt.private-key-path must not be blank")
    private String privateKeyPath;

    @NotBlank(message = "security.jwt.public-key-path must not be blank")
    private String publicKeyPath;

    @Min(value = 60, message = "security.jwt.access-token-expiration-seconds must be at least 60")
    private long accessTokenExpirationSeconds = 900;

    @NotBlank(message = "security.jwt.issuer must not be blank")
    private String issuer = "swasthai-report-generator";
}
