package com.swasthai.report_generator.test;

import tools.jackson.databind.ObjectMapper;
import com.swasthai.report_generator.common.exception.ResourceNotFoundException;
import com.swasthai.report_generator.test.dto.request.CreateTestParameterRequest;
import com.swasthai.report_generator.test.dto.request.UpdateTestParameterRequest;
import com.swasthai.report_generator.test.dto.response.TestParameterResponse;
import com.swasthai.report_generator.test.entity.TestParameterDataType;
import com.swasthai.report_generator.test.entity.TestParameterStatus;
import com.swasthai.report_generator.test.service.TestParameterService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TestParameterSecurityAndRbacTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TestParameterService testParameterService;

    private CreateTestParameterRequest sampleCreateRequest() {
        return new CreateTestParameterRequest(
                "HB",
                "Hemoglobin",
                "Blood Hemoglobin",
                TestParameterDataType.DECIMAL,
                "g/dL",
                true,
                1,
                new BigDecimal("12.0"),
                new BigDecimal("17.0"),
                new BigDecimal("7.0"),
                new BigDecimal("20.0"),
                "Hemoglobin test parameter",
                "Guidance note"
        );
    }

    private TestParameterResponse sampleResponse() {
        return TestParameterResponse.builder()
                .refId("PARAM-abc1234567")
                .testRefId("TEST-aeaIltCzRnrf")
                .testCode("CBC")
                .testName("Complete Blood Count")
                .code("HB")
                .name("Hemoglobin")
                .description("Blood Hemoglobin")
                .dataType(TestParameterDataType.DECIMAL)
                .unit("g/dL")
                .required(true)
                .displayOrder(1)
                .referenceMin(new BigDecimal("12.0"))
                .referenceMax(new BigDecimal("17.0"))
                .criticalLow(new BigDecimal("7.0"))
                .criticalHigh(new BigDecimal("20.0"))
                .reportDescription("Hemoglobin test parameter")
                .interpretationGuidance("Guidance note")
                .status(TestParameterStatus.ACTIVE)
                .version(1)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    // -------------------------------------------------------------
    // 1. POST /api/v1/tests/{testRefId}/parameters
    // -------------------------------------------------------------

    @Test
    @DisplayName("Create Parameter: Unauthenticated returns 401")
    void testCreateParameter_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(post("/api/v1/tests/TEST-aeaIltCzRnrf/parameters")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleCreateRequest())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        verify(testParameterService, never()).createParameter(any(), any());
    }

    @Test
    @DisplayName("Create Parameter: Non-SUPER_ADMIN (ORG_ADMIN) returns 403")
    void testCreateParameter_OrgAdmin_Returns403() throws Exception {
        mockMvc.perform(post("/api/v1/tests/TEST-aeaIltCzRnrf/parameters")
                        .with(user("orgadmin").roles("ORG_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleCreateRequest())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        verify(testParameterService, never()).createParameter(any(), any());
    }

    @Test
    @DisplayName("Create Parameter: Non-SUPER_ADMIN (LAB_STAFF) returns 403")
    void testCreateParameter_LabStaff_Returns403() throws Exception {
        mockMvc.perform(post("/api/v1/tests/TEST-aeaIltCzRnrf/parameters")
                        .with(user("labstaff").roles("LAB_STAFF"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleCreateRequest())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        verify(testParameterService, never()).createParameter(any(), any());
    }

    @Test
    @DisplayName("Create Parameter: SUPER_ADMIN succeeds with 200")
    void testCreateParameter_SuperAdmin_Returns200() throws Exception {
        when(testParameterService.createParameter(eq("TEST-aeaIltCzRnrf"), any())).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/tests/TEST-aeaIltCzRnrf/parameters")
                        .with(user("admin").roles("SUPER_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleCreateRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Test parameter created successfully"))
                .andExpect(jsonPath("$.data.refId").value("PARAM-abc1234567"))
                .andExpect(jsonPath("$.data.code").value("HB"));

        verify(testParameterService, times(1)).createParameter(eq("TEST-aeaIltCzRnrf"), any());
    }

    // -------------------------------------------------------------
    // 2. POST /api/v1/tests/{testRefId}/parameters/bulk
    // -------------------------------------------------------------

    @Test
    @DisplayName("Bulk Create Parameters: Unauthenticated returns 401")
    void testCreateParametersBulk_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(post("/api/v1/tests/TEST-aeaIltCzRnrf/parameters/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(sampleCreateRequest()))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        verify(testParameterService, never()).createParametersBulk(any(), any());
    }

    @Test
    @DisplayName("Bulk Create Parameters: ORG_ADMIN returns 403")
    void testCreateParametersBulk_OrgAdmin_Returns403() throws Exception {
        mockMvc.perform(post("/api/v1/tests/TEST-aeaIltCzRnrf/parameters/bulk")
                        .with(user("orgadmin").roles("ORG_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(sampleCreateRequest()))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        verify(testParameterService, never()).createParametersBulk(any(), any());
    }

    @Test
    @DisplayName("Bulk Create Parameters: SUPER_ADMIN succeeds with 200")
    void testCreateParametersBulk_SuperAdmin_Returns200() throws Exception {
        when(testParameterService.createParametersBulk(eq("TEST-aeaIltCzRnrf"), any())).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(post("/api/v1/tests/TEST-aeaIltCzRnrf/parameters/bulk")
                        .with(user("admin").roles("SUPER_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(sampleCreateRequest()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Test parameters created successfully"))
                .andExpect(jsonPath("$.data[0].refId").value("PARAM-abc1234567"));

        verify(testParameterService, times(1)).createParametersBulk(eq("TEST-aeaIltCzRnrf"), any());
    }

    // -------------------------------------------------------------
    // 3. GET /api/v1/tests/{testRefId}/parameters
    // -------------------------------------------------------------

    @Test
    @DisplayName("Get Test Parameters: Unauthenticated returns 401")
    void testGetTestParameters_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/tests/TEST-aeaIltCzRnrf/parameters"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("Get Test Parameters: ORG_ADMIN and LAB_STAFF permitted by endpoint RBAC")
    void testGetTestParameters_OrgAdminAndLabStaff_Returns200() throws Exception {
        when(testParameterService.getTestParameters(eq("TEST-aeaIltCzRnrf"), any(), any()))
                .thenReturn(new PageImpl<>(List.of(sampleResponse())));

        mockMvc.perform(get("/api/v1/tests/TEST-aeaIltCzRnrf/parameters")
                        .with(user("orgadmin").roles("ORG_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/v1/tests/TEST-aeaIltCzRnrf/parameters")
                        .with(user("labstaff").roles("LAB_STAFF")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // -------------------------------------------------------------
    // 4. GET /api/v1/test-parameters/{parameterRefId}
    // -------------------------------------------------------------

    @Test
    @DisplayName("Get Single Parameter: SUPER_ADMIN and ORG_ADMIN permitted by endpoint RBAC")
    void testGetParameter_Rbac() throws Exception {
        when(testParameterService.getParameter(eq("PARAM-abc1234567"))).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/v1/test-parameters/PARAM-abc1234567")
                        .with(user("admin").roles("SUPER_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.refId").value("PARAM-abc1234567"));

        mockMvc.perform(get("/api/v1/test-parameters/PARAM-abc1234567")
                        .with(user("orgadmin").roles("ORG_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.refId").value("PARAM-abc1234567"));
    }

    // -------------------------------------------------------------
    // 5. PATCH /api/v1/test-parameters/{parameterRefId}
    // -------------------------------------------------------------

    @Test
    @DisplayName("Update Parameter: ORG_ADMIN returns 403")
    void testUpdateParameter_OrgAdmin_Returns403() throws Exception {
        UpdateTestParameterRequest request = new UpdateTestParameterRequest(
                null, "New Name", null, null, null, null, null, null, null, null, null, null, null, null
        );

        mockMvc.perform(patch("/api/v1/test-parameters/PARAM-abc1234567")
                        .with(user("orgadmin").roles("ORG_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(testParameterService, never()).updateParameter(any(), any());
    }

    @Test
    @DisplayName("Update Parameter: SUPER_ADMIN succeeds with 200")
    void testUpdateParameter_SuperAdmin_Returns200() throws Exception {
        UpdateTestParameterRequest request = new UpdateTestParameterRequest(
                null, "New Name", null, null, null, null, null, null, null, null, null, null, null, null
        );
        when(testParameterService.updateParameter(eq("PARAM-abc1234567"), any())).thenReturn(sampleResponse());

        mockMvc.perform(patch("/api/v1/test-parameters/PARAM-abc1234567")
                        .with(user("admin").roles("SUPER_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(testParameterService, times(1)).updateParameter(eq("PARAM-abc1234567"), any());
    }

    // -------------------------------------------------------------
    // 6. DELETE /api/v1/test-parameters/{parameterRefId}
    // -------------------------------------------------------------

    @Test
    @DisplayName("Delete Parameter: ORG_ADMIN returns 403")
    void testDeleteParameter_OrgAdmin_Returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/test-parameters/PARAM-abc1234567")
                        .with(user("orgadmin").roles("ORG_ADMIN")))
                .andExpect(status().isForbidden());

        verify(testParameterService, never()).deactivateParameter(any());
    }

    @Test
    @DisplayName("Delete Parameter: SUPER_ADMIN succeeds with 200")
    void testDeleteParameter_SuperAdmin_Returns200() throws Exception {
        doNothing().when(testParameterService).deactivateParameter("PARAM-abc1234567");

        mockMvc.perform(delete("/api/v1/test-parameters/PARAM-abc1234567")
                        .with(user("admin").roles("SUPER_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Test parameter deactivated successfully"));

        verify(testParameterService, times(1)).deactivateParameter("PARAM-abc1234567");
    }
}
