package com.swasthai.report_generator.auth.bootstrap;

import com.swasthai.report_generator.user.entity.Role;
import com.swasthai.report_generator.user.entity.User;
import com.swasthai.report_generator.user.entity.UserStatus;
import com.swasthai.report_generator.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Ensures a single root SUPER_ADMIN is provisioned and kept synchronized with environment credentials.
 * Reads credentials securely from environment properties (e.g. .env).
 */
@Component
@RequiredArgsConstructor
public class AdminBootstrapRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${bootstrap.admin.enabled:false}")
    private boolean bootstrapEnabled;

    @Value("${bootstrap.admin.email:}")
    private String adminEmail;

    @Value("${bootstrap.admin.password:}")
    private String adminPassword;

    @Value("${bootstrap.admin.name:System Super Admin}")
    private String adminName;

    @Override
    public void run(String... args) {
        if (!bootstrapEnabled) {
            log.debug("Super admin bootstrapping is disabled.");
            return;
        }

        if (adminEmail == null || adminEmail.isBlank() || adminPassword == null || adminPassword.isBlank()) {
            throw new IllegalStateException("BOOTSTRAP_ADMIN_EMAIL or BOOTSTRAP_ADMIN_PASSWORD was not provided while bootstrapping is enabled.");
        }

        if (adminPassword.length() < 8) {
            throw new IllegalStateException("BOOTSTRAP_ADMIN_PASSWORD must be at least 8 characters long.");
        }

        String normalizedEmail = adminEmail.trim().toLowerCase();

        Optional<User> existingUserOpt = userRepository.findByEmailIgnoreCase(normalizedEmail);
        if (existingUserOpt.isPresent()) {
            User existingUser = existingUserOpt.get();
            if (existingUser.getRole() == Role.SUPER_ADMIN) {
                existingUser.setName(adminName != null && !adminName.isBlank() ? adminName.trim() : existingUser.getName());
                existingUser.setPasswordHash(passwordEncoder.encode(adminPassword));
                existingUser.setStatus(UserStatus.ACTIVE);
                userRepository.save(existingUser);
                log.info("Successfully synchronized SUPER_ADMIN credentials from environment for: {}", normalizedEmail);
            } else {
                log.warn("User with email {} already exists but is not a SUPER_ADMIN, skipping bootstrap.", normalizedEmail);
            }
            return;
        }

        if (userRepository.countByRole(Role.SUPER_ADMIN) > 0) {
            log.info("A different SUPER_ADMIN account already exists, skipping bootstrap.");
            return;
        }

        User superAdmin = User.builder()
                .name(adminName != null && !adminName.isBlank() ? adminName.trim() : "System Super Admin")
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .role(Role.SUPER_ADMIN)
                .status(UserStatus.ACTIVE)
                .organization(null)
                .build();

        userRepository.save(superAdmin);
        log.info("Successfully bootstrapped initial SUPER_ADMIN account with email: {}", normalizedEmail);
    }
}
