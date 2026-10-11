package com.swasthai.report_generator.security;

import com.swasthai.report_generator.license.service.LicenseGuard;
import com.swasthai.report_generator.organization.entity.Organization;
import com.swasthai.report_generator.organization.entity.OrganizationStatus;
import com.swasthai.report_generator.organization.repository.OrganizationRepository;
import com.swasthai.report_generator.test.entity.OrganizationTest;
import com.swasthai.report_generator.test.entity.OrganizationTestStatus;
import com.swasthai.report_generator.test.entity.Test;
import com.swasthai.report_generator.test.entity.TestStatus;
import com.swasthai.report_generator.test.repository.OrganizationTestRepository;
import com.swasthai.report_generator.test.repository.TestRepository;
import com.swasthai.report_generator.test.service.OrganizationTestService;
import com.swasthai.report_generator.test.service.impl.OrganizationTestServiceImpl;
import com.swasthai.report_generator.user.entity.Role;
import com.swasthai.report_generator.user.entity.User;
import com.swasthai.report_generator.user.entity.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrganizationTestServiceSubscriptionSecurityTest {

    @Mock
    private OrganizationTestRepository organizationTestRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private TestRepository testRepository;

    @Mock
    private LicenseGuard licenseGuard;

    private OrganizationTestService organizationTestService;

    private Organization testOrg;
    private User orgAdminUser;
    private Test cbcTest;
    private OrganizationTest assignment;

    @BeforeEach
    void setUp() {
        organizationTestService = new OrganizationTestServiceImpl(
                organizationTestRepository,
                organizationRepository,
                testRepository,
                licenseGuard
        );

        testOrg = Organization.builder()
                .id(UUID.randomUUID())
                .name("Alpha Diagnostics")
                .code("ALPHA")
                .status(OrganizationStatus.ACTIVE)
                .build();

        orgAdminUser = User.builder()
                .id(UUID.randomUUID())
                .email("admin@alpha.com")
                .name("Admin Alpha")
                .role(Role.ORG_ADMIN)
                .status(UserStatus.ACTIVE)
                .organization(testOrg)
                .build();

        cbcTest = Test.builder()
                .id(UUID.randomUUID())
                .refId("TEST-CBC-001")
                .code("CBC")
                .name("Complete Blood Count")
                .status(TestStatus.ACTIVE)
                .build();

        assignment = OrganizationTest.builder()
                .id(UUID.randomUUID())
                .organization(testOrg)
                .test(cbcTest)
                .status(OrganizationTestStatus.ACTIVE)
                .effectiveFrom(LocalDate.now().minusDays(10))
                .effectiveUntil(LocalDate.now().plusDays(300))
                .build();

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                orgAdminUser.getRefId(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + orgAdminUser.getRole().name()))
        );
        auth.setDetails(orgAdminUser);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @org.junit.jupiter.api.Test
    @DisplayName("SEC-TEST-001: hasTestAccess returns false when organization subscription is inactive/expired")
    void testHasTestAccess_ReturnsFalseWhenSubscriptionInactive() {
        when(licenseGuard.isSubscriptionActive(testOrg.getId())).thenReturn(false);

        boolean hasAccess = organizationTestService.hasTestAccess(cbcTest.getRefId());

        assertThat(hasAccess).isFalse();
        // Database test lookups should not even be evaluated if subscription is inactive
        verify(testRepository, never()).findByRefId(any());
        verify(organizationTestRepository, never()).findByOrganization_IdAndTest_Id(any(), any());
    }

    @org.junit.jupiter.api.Test
    @DisplayName("SEC-TEST-002: hasTestAccess returns true when organization subscription is active and test is assigned")
    void testHasTestAccess_ReturnsTrueWhenSubscriptionActive() {
        when(licenseGuard.isSubscriptionActive(testOrg.getId())).thenReturn(true);
        when(testRepository.findByRefId(cbcTest.getRefId())).thenReturn(Optional.of(cbcTest));
        when(organizationTestRepository.findByOrganization_IdAndTest_Id(testOrg.getId(), cbcTest.getId()))
                .thenReturn(Optional.of(assignment));

        boolean hasAccess = organizationTestService.hasTestAccess(cbcTest.getRefId());

        assertThat(hasAccess).isTrue();
    }
}
