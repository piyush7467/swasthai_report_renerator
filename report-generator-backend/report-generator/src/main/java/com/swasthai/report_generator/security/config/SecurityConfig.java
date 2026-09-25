package com.swasthai.report_generator.security.config;

import com.swasthai.report_generator.security.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

        private final JwtAuthenticationFilter jwtAuthenticationFilter;
        private final RestAuthenticationEntryPoint authenticationEntryPoint;
        private final RestAccessDeniedHandler accessDeniedHandler;

        @Value("${security.cors.allowed-origins:http://localhost:3000,http://localhost:5173,http://localhost:8080}")
        private String allowedOrigins;

        @Bean
        public SecurityFilterChain securityFilterChain(
                        HttpSecurity http) throws Exception {

                http
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                                .csrf(csrf -> csrf.disable())

                                .headers(headers -> headers
                                                .frameOptions(frame -> frame.deny())
                                                .contentTypeOptions(Customizer.withDefaults())
                                                .referrerPolicy(referrer -> referrer.policy(
                                                                ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                                                .contentSecurityPolicy(csp -> csp.policyDirectives(
                                                                "default-src 'self'; frame-ancestors 'none'; object-src 'none';")))

                                .sessionManagement(session -> session.sessionCreationPolicy(
                                                SessionCreationPolicy.STATELESS))

                                .exceptionHandling(exception -> exception
                                                .authenticationEntryPoint(
                                                                authenticationEntryPoint)
                                                .accessDeniedHandler(
                                                                accessDeniedHandler))

                                .authorizeHttpRequests(auth -> auth

                                                .requestMatchers(
                                                                "/api/v1/auth/login",
                                                                "/api/v1/auth/refresh")
                                                .permitAll()

                                                .requestMatchers(
                                                                HttpMethod.GET,
                                                                "/api/v1/public/reports/*/verify",
                                                                "/api/v1/shared/reports/**")
                                                .permitAll()

                                                .requestMatchers(
                                                                "/actuator/health")
                                                .permitAll()

                                                .requestMatchers(
                                                                "/api/v1/plans/**",
                                                                "/api/v1/admin/analytics/**",
                                                                "/api/v1/admin/audit/**",
                                                                "/api/v1/admin/license/**")
                                                .hasRole("SUPER_ADMIN")

                                                .requestMatchers(
                                                                "/api/v1/organization-profile/me",
                                                                "/api/v1/organization-profile/me/**")
                                                .hasAnyRole(
                                                                "ORG_ADMIN",
                                                                "LAB_STAFF")

                                                .requestMatchers(
                                                                "/api/v1/organization-profile/organizations/**")
                                                .hasRole("SUPER_ADMIN")

                                                .requestMatchers(
                                                                "/api/v1/organizations/**")
                                                .hasRole("SUPER_ADMIN")

                                                .requestMatchers(
                                                                "/api/v1/license/**")
                                                .hasRole("ORG_ADMIN")

                                                .requestMatchers(
                                                                "/api/v1/users/**")
                                                .hasAnyRole(
                                                                "SUPER_ADMIN",
                                                                "ORG_ADMIN")

                                                .requestMatchers(
                                                                "/api/v1/patients/**")
                                                .hasAnyRole(
                                                                "ORG_ADMIN",
                                                                "LAB_STAFF")

                                                .requestMatchers(
                                                                org.springframework.http.HttpMethod.GET,
                                                                "/api/v1/organization-tests/my")
                                                .hasAnyRole(
                                                                "ORG_ADMIN",
                                                                "LAB_STAFF")

                                                .requestMatchers(
                                                                org.springframework.http.HttpMethod.GET,
                                                                "/api/v1/organization-tests/*")
                                                .hasAnyRole(
                                                                "SUPER_ADMIN",
                                                                "ORG_ADMIN",
                                                                "LAB_STAFF")

                                                .requestMatchers(
                                                                "/api/v1/organization-tests/**")
                                                .hasRole("SUPER_ADMIN")

                                                .requestMatchers(
                                                                org.springframework.http.HttpMethod.POST,
                                                                "/api/v1/reports/*/break-glass")
                                                .hasRole("SUPER_ADMIN")

                                                .requestMatchers(
                                                                "/api/v1/reports/**")
                                                .hasAnyRole(
                                                                "ORG_ADMIN",
                                                                "LAB_STAFF")

                                                .anyRequest().authenticated())

                                .addFilterBefore(
                                                jwtAuthenticationFilter,
                                                UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();

                List<String> origins = Arrays.stream(allowedOrigins.split(","))
                                .map(String::trim)
                                .filter(s -> !s.isBlank())
                                .toList();

                configuration.setAllowedOrigins(origins);
                configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With", "Accept"));
                configuration.setExposedHeaders(List.of());
                configuration.setAllowCredentials(false);
                configuration.setMaxAge(3600L);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);
                return source;
        }
}
