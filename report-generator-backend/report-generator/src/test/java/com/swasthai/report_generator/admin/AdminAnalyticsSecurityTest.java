package com.swasthai.report_generator.admin;

import com.swasthai.report_generator.organization.entity.Organization;
import com.swasthai.report_generator.organization.entity.OrganizationStatus;
import com.swasthai.report_generator.organization.repository.OrganizationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminAnalyticsSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrganizationRepository organizationRepository;

    private String testOrgRefId;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        Organization testOrg = organizationRepository.save(Organization.builder()
                .name("Analytics Lab " + suffix)
                .code("ALAB-" + suffix)
                .status(OrganizationStatus.ACTIVE)
                .build());
        testOrgRefId = testOrg.getRefId();
    }

    @Test
    @DisplayName("SUPER_ADMIN can access overview analytics")
    void superAdminCanAccessOverview() throws Exception {
        mockMvc.perform(get("/api/v1/admin/analytics/overview")
                        .with(user("admin@swasthai.com").roles("SUPER_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalReports").isNumber());
    }

    @Test
    @DisplayName("SUPER_ADMIN can access report trend")
    void superAdminCanAccessTrend() throws Exception {
        mockMvc.perform(get("/api/v1/admin/analytics/reports/trend")
                        .param("days", "7")
                        .with(user("admin@swasthai.com").roles("SUPER_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("SUPER_ADMIN can access organization report activity")
    void superAdminCanAccessOrganizationActivity() throws Exception {
        mockMvc.perform(get("/api/v1/admin/analytics/reports/organizations")
                        .with(user("admin@swasthai.com").roles("SUPER_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @DisplayName("SUPER_ADMIN can access test usage statistics")
    void superAdminCanAccessTestUsage() throws Exception {
        mockMvc.perform(get("/api/v1/admin/analytics/tests/usage")
                        .param("limit", "10")
                        .with(user("admin@swasthai.com").roles("SUPER_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("SUPER_ADMIN can access security audit logs")
    void superAdminCanAccessSecurityAudit() throws Exception {
        mockMvc.perform(get("/api/v1/admin/audit/security")
                        .with(user("admin@swasthai.com").roles("SUPER_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @DisplayName("ORG_ADMIN is forbidden from accessing overview analytics")
    void orgAdminForbiddenFromOverview() throws Exception {
        mockMvc.perform(get("/api/v1/admin/analytics/overview")
                        .with(user("orgadmin@swasthai.com").roles("ORG_ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ORG_ADMIN is forbidden from accessing audit logs")
    void orgAdminForbiddenFromAudit() throws Exception {
        mockMvc.perform(get("/api/v1/admin/audit/security")
                        .with(user("orgadmin@swasthai.com").roles("ORG_ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("LAB_STAFF is forbidden from accessing overview analytics")
    void labStaffForbiddenFromOverview() throws Exception {
        mockMvc.perform(get("/api/v1/admin/analytics/overview")
                        .with(user("staff@swasthai.com").roles("LAB_STAFF")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated request is unauthorized")
    void unauthenticatedRequestUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/admin/analytics/overview"))
                .andExpect(status().isUnauthorized());
    }
}
