package com.swasthai.report_generator.test.service;

import tools.jackson.databind.ObjectMapper;
import com.swasthai.report_generator.common.exception.ResourceNotFoundException;
import com.swasthai.report_generator.test.calculation.CalculationException;
import com.swasthai.report_generator.test.dto.request.CalculateTestRequest;
import com.swasthai.report_generator.test.dto.request.ParameterValueInput;
import com.swasthai.report_generator.test.dto.response.CalculatedParameterItemResponse;
import com.swasthai.report_generator.test.dto.response.CalculatedTestResponse;
import com.swasthai.report_generator.test.entity.CalculationType;
import com.swasthai.report_generator.test.entity.ParameterInputType;
import com.swasthai.report_generator.test.entity.ResultFlag;
import com.swasthai.report_generator.test.entity.TestParameterDataType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TestCalculationSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TestCalculationService testCalculationService;

    private CalculateTestRequest sampleRequest() {
        return new CalculateTestRequest(List.of(
                new ParameterValueInput("HGB", null, "15"),
                new ParameterValueInput("RBC", null, "5"),
                new ParameterValueInput("HCT", null, "45")
        ));
    }

    private CalculatedTestResponse sampleResponse() {
        return new CalculatedTestResponse(
                "TEST-CBC123",
                "CBC",
                "Complete Blood Count",
                List.of(
                        new CalculatedParameterItemResponse(
                                "PARAM-MCV01",
                                "MCV",
                                "Mean Corpuscular Volume",
                                ParameterInputType.CALCULATED,
                                CalculationType.MCV,
                                TestParameterDataType.DECIMAL,
                                "fL",
                                "90",
                                new BigDecimal("90"),
                                ResultFlag.NORMAL,
                                new BigDecimal("80.0"),
                                new BigDecimal("100.0"),
                                null,
                                null,
                                4
                        )
                )
        );
    }

    @Test
    @DisplayName("Endpoint: Unauthenticated request is rejected with 401 Unauthorized")
    void testCalculate_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(post("/api/v1/tests/TEST-CBC123/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Endpoint: Unauthorized role is rejected with 403 Forbidden")
    void testCalculate_UnauthorizedRole_Returns403() throws Exception {
        mockMvc.perform(post("/api/v1/tests/TEST-CBC123/calculate")
                        .with(user("patient").roles("PATIENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Endpoint: SUPER_ADMIN is authorized and receives 200 OK")
    void testCalculate_SuperAdmin_Returns200() throws Exception {
        when(testCalculationService.calculateParameters(eq("TEST-CBC123"), any()))
                .thenReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/tests/TEST-CBC123/calculate")
                        .with(user("superadmin").roles("SUPER_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.testCode").value("CBC"))
                .andExpect(jsonPath("$.data.parameters[0].parameterCode").value("MCV"));
    }

    @Test
    @DisplayName("Endpoint: ORG_ADMIN is authorized and receives 200 OK")
    void testCalculate_OrgAdmin_Returns200() throws Exception {
        when(testCalculationService.calculateParameters(eq("TEST-CBC123"), any()))
                .thenReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/tests/TEST-CBC123/calculate")
                        .with(user("orgadmin").roles("ORG_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Endpoint: LAB_STAFF is authorized and receives 200 OK")
    void testCalculate_LabStaff_Returns200() throws Exception {
        when(testCalculationService.calculateParameters(eq("TEST-CBC123"), any()))
                .thenReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/tests/TEST-CBC123/calculate")
                        .with(user("labstaff").roles("LAB_STAFF"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Endpoint: CalculationException maps to 400 Bad Request with code CALCULATION_ERROR")
    void testCalculate_CalculationException_Returns400() throws Exception {
        when(testCalculationService.calculateParameters(eq("TEST-CBC123"), any()))
                .thenThrow(new CalculationException("Cannot calculate MCV because RBC is zero"));

        mockMvc.perform(post("/api/v1/tests/TEST-CBC123/calculate")
                        .with(user("orgadmin").roles("ORG_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("CALCULATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Cannot calculate MCV because RBC is zero"));
    }

    @Test
    @DisplayName("Endpoint: Calculated parameter manual override attack maps to 400 Bad Request")
    void testCalculate_ManualOverrideAttack_Returns400() throws Exception {
        when(testCalculationService.calculateParameters(eq("TEST-CBC123"), any()))
                .thenThrow(new IllegalArgumentException("Calculated parameters cannot be manually provided: MCV"));

        mockMvc.perform(post("/api/v1/tests/TEST-CBC123/calculate")
                        .with(user("orgadmin").roles("ORG_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Calculated parameters cannot be manually provided: MCV"));
    }

    @Test
    @DisplayName("Endpoint: AccessDeniedException maps to 403 Forbidden")
    void testCalculate_AccessDeniedException_Returns403() throws Exception {
        when(testCalculationService.calculateParameters(eq("TEST-CBC123"), any()))
                .thenThrow(new AccessDeniedException("Organization does not have active access to test"));

        mockMvc.perform(post("/api/v1/tests/TEST-CBC123/calculate")
                        .with(user("orgadmin").roles("ORG_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("Endpoint: ResourceNotFoundException maps to 404 Not Found")
    void testCalculate_ResourceNotFoundException_Returns404() throws Exception {
        when(testCalculationService.calculateParameters(eq("TEST-CBC999"), any()))
                .thenThrow(new ResourceNotFoundException("Test not found with refId: TEST-CBC999"));

        mockMvc.perform(post("/api/v1/tests/TEST-CBC999/calculate")
                        .with(user("orgadmin").roles("ORG_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }
}
