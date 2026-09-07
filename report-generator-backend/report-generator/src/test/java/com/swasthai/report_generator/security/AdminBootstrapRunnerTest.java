package com.swasthai.report_generator.security;

import com.swasthai.report_generator.auth.bootstrap.AdminBootstrapRunner;
import com.swasthai.report_generator.user.entity.Role;
import com.swasthai.report_generator.user.entity.User;
import com.swasthai.report_generator.user.entity.UserStatus;
import com.swasthai.report_generator.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapRunnerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminBootstrapRunner bootstrapRunner;

    @Test
    @DisplayName("Bootstrap: Disabled by default, skips execution without inspecting DB or creating admin")
    void testBootstrapDisabledByDefault_DoesNothing() {
        ReflectionTestUtils.setField(bootstrapRunner, "bootstrapEnabled", false);

        bootstrapRunner.run();

        verify(userRepository, never()).countByRole(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Bootstrap: Enabled but missing email throws IllegalStateException to prevent silent unsafe state")
    void testBootstrapEnabled_MissingEmailThrows() {
        ReflectionTestUtils.setField(bootstrapRunner, "bootstrapEnabled", true);
        ReflectionTestUtils.setField(bootstrapRunner, "adminEmail", "");
        ReflectionTestUtils.setField(bootstrapRunner, "adminPassword", "SecurePass123!");

        assertThatThrownBy(() -> bootstrapRunner.run())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("BOOTSTRAP_ADMIN_EMAIL or BOOTSTRAP_ADMIN_PASSWORD was not provided");
    }

    @Test
    @DisplayName("Bootstrap: Enabled but short password (< 8 chars) throws IllegalStateException")
    void testBootstrapEnabled_ShortPasswordThrows() {
        ReflectionTestUtils.setField(bootstrapRunner, "bootstrapEnabled", true);
        ReflectionTestUtils.setField(bootstrapRunner, "adminEmail", "admin@swasthai.com");
        ReflectionTestUtils.setField(bootstrapRunner, "adminPassword", "short");

        assertThatThrownBy(() -> bootstrapRunner.run())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 8 characters long");
    }

    @Test
    @DisplayName("Bootstrap: Successfully seeds root SUPER_ADMIN when enabled and no admin exists")
    void testBootstrapEnabled_CreatesSuperAdmin() {
        ReflectionTestUtils.setField(bootstrapRunner, "bootstrapEnabled", true);
        ReflectionTestUtils.setField(bootstrapRunner, "adminEmail", "root@swasthai.com");
        ReflectionTestUtils.setField(bootstrapRunner, "adminPassword", "SuperSecretPass123!");
        ReflectionTestUtils.setField(bootstrapRunner, "adminName", "Root Administrator");

        when(userRepository.findByEmailIgnoreCase("root@swasthai.com")).thenReturn(Optional.empty());
        when(userRepository.countByRole(Role.SUPER_ADMIN)).thenReturn(0L);
        when(passwordEncoder.encode("SuperSecretPass123!")).thenReturn("encoded_hash_123");

        bootstrapRunner.run();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User savedAdmin = captor.getValue();

        assertThat(savedAdmin.getEmail()).isEqualTo("root@swasthai.com");
        assertThat(savedAdmin.getName()).isEqualTo("Root Administrator");
        assertThat(savedAdmin.getRole()).isEqualTo(Role.SUPER_ADMIN);
        assertThat(savedAdmin.getPasswordHash()).isEqualTo("encoded_hash_123");
        assertThat(savedAdmin.getOrganization()).isNull();
    }

    @Test
    @DisplayName("Bootstrap: Synchronizes credentials when matching SUPER_ADMIN already exists in database")
    void testBootstrapEnabled_SyncsMatchingSuperAdmin() {
        ReflectionTestUtils.setField(bootstrapRunner, "bootstrapEnabled", true);
        ReflectionTestUtils.setField(bootstrapRunner, "adminEmail", "admin@swasthai.com");
        ReflectionTestUtils.setField(bootstrapRunner, "adminPassword", "UpdatedPass123!");
        ReflectionTestUtils.setField(bootstrapRunner, "adminName", "Updated Admin Name");

        User existingAdmin = User.builder()
                .name("Old Name")
                .email("admin@swasthai.com")
                .passwordHash("old_hash")
                .role(Role.SUPER_ADMIN)
                .status(UserStatus.ACTIVE)
                .build();

        when(userRepository.findByEmailIgnoreCase("admin@swasthai.com")).thenReturn(Optional.of(existingAdmin));
        when(passwordEncoder.encode("UpdatedPass123!")).thenReturn("new_hash_456");

        bootstrapRunner.run();

        verify(userRepository).save(existingAdmin);
        assertThat(existingAdmin.getPasswordHash()).isEqualTo("new_hash_456");
        assertThat(existingAdmin.getName()).isEqualTo("Updated Admin Name");
    }

    @Test
    @DisplayName("Bootstrap: Skips if a different SUPER_ADMIN already exists")
    void testBootstrapEnabled_SkipsIfDifferentSuperAdminExists() {
        ReflectionTestUtils.setField(bootstrapRunner, "bootstrapEnabled", true);
        ReflectionTestUtils.setField(bootstrapRunner, "adminEmail", "newadmin@swasthai.com");
        ReflectionTestUtils.setField(bootstrapRunner, "adminPassword", "SuperSecretPass123!");

        when(userRepository.findByEmailIgnoreCase("newadmin@swasthai.com")).thenReturn(Optional.empty());
        when(userRepository.countByRole(Role.SUPER_ADMIN)).thenReturn(1L);

        bootstrapRunner.run();

        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }
}
