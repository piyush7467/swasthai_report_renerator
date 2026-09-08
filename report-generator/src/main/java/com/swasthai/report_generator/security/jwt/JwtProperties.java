package com.swasthai.report_generator.security.jwt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "security.jwt")
public class JwtProperties {

    private String privateKeyPath;

    private String publicKeyPath;

    private long accessTokenExpirationSeconds = 900;

    private String issuer = "swasthai-report-generator";
}
