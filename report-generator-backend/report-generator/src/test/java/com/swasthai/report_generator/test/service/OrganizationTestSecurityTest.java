package com.swasthai.report_generator.test.service;

import tools.jackson.databind.ObjectMapper;
import com.swasthai.report_generator.test.dto.request.AssignTestRequest;
import com.swasthai.report_generator.test.dto.response.OrganizationTestResponse;
import com.swasthai.report_generator.test.service.OrganizationTestService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OrganizationTestSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrganizationTestService organizationTestService;

    // -------------------------------------------------------------------------
    // 1. Unauthenticated requests -> 401
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("SEC-VULN-001: Unauthenticated request to GET /api/v1/organization-tests/my returns 401")
    void testGetMyTests_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/organization-tests/my"))
                .andExpect(status().isUnauthorized());

        verify(organizationTestService, never()).getMyOrganizationTests(any(), any());
    }

    @Test
    @DisplayName("SEC-VULN-001: Unauthenticated request to POST /api/v1/organization-tests returns 401")
    void testAssignTest_Unauthenticated_Returns401() throws Exception {
        AssignTestRequest request = new AssignTestRequest(
                "org-ref-123",
                "test-ref-456",
                null,
                null
        );

        mockMvc.perform(post("/api/v1/organization-tests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        verify(organizationTestService, never()).assignTest(any());
    }

    // -------------------------------------------------------------------------
    // 2. ORG_ADMIN / LAB_STAFF Tenant Endpoints
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("SEC-VULN-001: ORG_ADMIN can access GET /api/v1/organization-tests/my")
    void testGetMyTests_OrgAdmin_Returns200() throws Exception {
        when(organizationTestService.getMyOrganizationTests(any(), any()))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        mockMvc.perform(get("/api/v1/organization-tests/my")
                        .with(user("orgadmin").roles("ORG_ADMIN")))
                .andExpect(status().isOk());

        verify(organizationTestService).getMyOrganizationTests(any(), any());
    }

    @Test
    @DisplayName("SEC-VULN-001: LAB_STAFF can access GET /api/v1/organization-tests/my")
    void testGetMyTests_LabStaff_Returns200() throws Exception {
        when(organizationTestService.getMyOrganizationTests(any(), any()))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        mockMvc.perform(get("/api/v1/organization-tests/my")
                        .with(user("labstaff").roles("LAB_STAFF")))
                .andExpect(status().isOk());

        verify(organizationTestService).getMyOrganizationTests(any(), any());
    }

    @Test
    @DisplayName("SEC-VULN-001: SUPER_ADMIN cannot access GET /api/v1/organization-tests/my (tenant endpoint)")
    void testGetMyTests_SuperAdmin_Returns403() throws Exception {
        mockMvc.perform(get("/api/v1/organization-tests/my")
                        .with(user("superadmin").roles("SUPER_ADMIN")))
                .andExpect(status().isForbidden());

        verify(organizationTestService, never()).getMyOrganizationTests(any(), any());
    }

    // -------------------------------------------------------------------------
    // 3. GET /api/v1/organization-tests/{refId} (SUPER_ADMIN, ORG_ADMIN, LAB_STAFF)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("SEC-VULN-001: LAB_STAFF can read assignment by refId")
    void testGetAssignment_LabStaff_Returns200() throws Exception {
        when(organizationTestService.getAssignment(eq("ot-ref-123")))
                .thenReturn(OrganizationTestResponse.builder().refId("ot-ref-123").build());

        mockMvc.perform(get("/api/v1/organization-tests/ot-ref-123")
                        .with(user("labstaff").roles("LAB_STAFF")))
                .andExpect(status().isOk());

        verify(organizationTestService).getAssignment("ot-ref-123");
    }

    // -------------------------------------------------------------------------
    // 4. Mutation Endpoints (SUPER_ADMIN only)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("SEC-VULN-001: ORG_ADMIN cannot assign test via POST /api/v1/organization-tests (returns 403)")
    void testAssignTest_OrgAdmin_Returns403() throws Exception {
        AssignTestRequest request = new AssignTestRequest(
                "org-ref-123",
                "test-ref-456",
                null,
                null
        );

        mockMvc.perform(post("/api/v1/organization-tests")
                        .with(user("orgadmin").roles("ORG_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(organizationTestService, never()).assignTest(any());
    }

    @Test
    @DisplayName("SEC-VULN-001: LAB_STAFF cannot delete assignment via DELETE /api/v1/organization-tests/{refId}")
    void testDeactivateAssignment_LabStaff_Returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/organization-tests/ot-ref-123")
                        .with(user("labstaff").roles("LAB_STAFF")))
                .andExpect(status().isForbidden());

        verify(organizationTestService, never()).deactivateAssignment(any());
    }

    @Test
    @DisplayName("SEC-VULN-001: SUPER_ADMIN can assign test via POST /api/v1/organization-tests")
    void testAssignTest_SuperAdmin_Returns200() throws Exception {
        AssignTestRequest request = new AssignTestRequest(
                "org-ref-123",
                "test-ref-456",
                null,
                null
        );

        when(organizationTestService.assignTest(any()))
                .thenReturn(OrganizationTestResponse.builder().refId("ot-ref-new").build());

        mockMvc.perform(post("/api/v1/organization-tests")
                        .with(user("superadmin").roles("SUPER_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(organizationTestService).assignTest(any());
    }
}