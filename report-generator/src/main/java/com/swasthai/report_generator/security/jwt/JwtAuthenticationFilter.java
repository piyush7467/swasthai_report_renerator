package com.swasthai.report_generator.security.jwt;

import com.swasthai.report_generator.user.entity.User;
import com.swasthai.report_generator.user.entity.UserStatus;
import com.swasthai.report_generator.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authorizationHeader =
                request.getHeader("Authorization");

        if (authorizationHeader == null ||
                !authorizationHeader.startsWith("Bearer ")) {

            filterChain.doFilter(request, response);
            return;
        }

        String token =
                authorizationHeader.substring(7).trim();

        if (token.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {

            Jws<Claims> claims =
                    jwtService.validateToken(token);

            String userRefId =
                    claims.getPayload().getSubject();

            if (userRefId == null || userRefId.isBlank()) {
                filterChain.doFilter(request, response);
                return;
            }

            /*
             * Do not trust role/org from the JWT alone.
             *
             * The database remains the source of truth for:
             * - user status
             * - current role
             * - current organization
             */
            User user =
                    userRepository.findByRefId(userRefId)
                            .orElse(null);

            if (user == null || user.getStatus() != UserStatus.ACTIVE) {
                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }

            // Enforce organization status: suspended or disabled organizations cannot execute API requests
            if (user.getRole() != com.swasthai.report_generator.user.entity.Role.SUPER_ADMIN) {
                if (user.getOrganization() == null ||
                        user.getOrganization().getStatus() != com.swasthai.report_generator.organization.entity.OrganizationStatus.ACTIVE) {
                    SecurityContextHolder.clearContext();
                    filterChain.doFilter(request, response);
                    return;
                }
            }

            SimpleGrantedAuthority authority =
                    new SimpleGrantedAuthority(
                            "ROLE_" + user.getRole().name()
                    );

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userRefId,
                            null,
                            List.of(authority)
                    );

            authentication.setDetails(user);

            SecurityContextHolder
                    .getContext()
                    .setAuthentication(authentication);

        } catch (IllegalArgumentException exception) {
            // Expected validation failure for expired, malformed, or tampered tokens
            SecurityContextHolder.clearContext();
        } catch (Exception exception) {
            log.error("Unexpected error in JWT authentication filter: {}", exception.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
