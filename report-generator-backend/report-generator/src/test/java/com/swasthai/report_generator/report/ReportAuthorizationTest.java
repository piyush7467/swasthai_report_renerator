package com.swasthai.report_generator.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swasthai.report_generator.auth.dto.request.LoginRequest;
import com.swasthai.report_generator.auth.dto.response.LoginResponse;
import com.swasthai.report_generator.auth.service.AuthService;
import com.swasthai.report_generator.organization.entity.Organization;
import com.swasthai.report_generator.organization.entity.OrganizationStatus;
import com.swasthai.report_generator.organization.repository.OrganizationRepository;
import com.swasthai.report_generator.patient.entity.Gender;
import com.swasthai.report_generator.patient.entity.Patient;
import com.swasthai.report_generator.patient.entity.Salutation;
import com.swasthai.report_generator.patient.repository.PatientRepository;
import com.swasthai.report_generator.report.dto.request.CreateReportRequest;
import com.swasthai.report_generator.user.entity.Role;
import com.swasthai.report_generator.user.entity.User;
import com.swasthai.report_generator.user.entity.UserStatus;
import com.swasthai.report_generator.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReportAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules();

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthService authService;

    @Autowired
    private com.swasthai.report_generator.license.repository.PlanRepository planRepository;

    @Autowired
    private com.swasthai.report_generator.license.repository.LicenseRepository licenseRepository;

    private Organization org;
    private User orgAdmin;
    private Patient patient;
    private String rawPassword = "Password123!";

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        org = organizationRepository.save(Organization.builder()
                .name("Auth Org " + suffix)
                .code("AO-" + suffix)
                .status(OrganizationStatus.ACTIVE)
                .build());

        orgAdmin = userRepository.save(User.builder()
                .email("admin-" + suffix + "@authtest.com")
                .name("Org Admin " + suffix)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .role(Role.ORG_ADMIN)
                .status(UserStatus.ACTIVE)
                .organization(org)
                .build());

        patient = patientRepository.save(Patient.builder()
                .organization(org)
                .salutation(Salutation.MR)
                .name("Test Patient " + suffix)
                .patientCode("P-" + suffix)
                .dateOfBirthKnown(true)
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .gender(Gender.MALE)
                .build());

        com.swasthai.report_generator.license.entity.Plan testPlan = planRepository.findByCodeIgnoreCase("STARTER")
                .orElseGet(() -> planRepository.save(com.swasthai.report_generator.license.entity.Plan.builder()
                        .code("STARTER")
                        .name("Starter Plan")
                        .annualPrice(new BigDecimal("9990.00"))
                        .currency("INR")
                        .active(true)
                        .build()));

        licenseRepository.save(com.swasthai.report_generator.license.entity.License.builder()
                .organization(org)
                .plan(testPlan)
                .status(com.swasthai.report_generator.license.entity.LicenseStatus.ACTIVE)
                .startedAt(Instant.now().minusSeconds(3600))
                .expiresAt(Instant.now().plusSeconds(86400 * 365))
                .build());
    }

    @Test
    @DisplayName("Test POST /api/v1/reports with real JWT login token succeeds (200)")
    void testPostReports_WithJwtToken() throws Exception {
        LoginRequest loginReq = new LoginRequest();
        loginReq.setEmail(orgAdmin.getEmail());
        loginReq.setPassword(rawPassword);
        LoginResponse loginResponse = authService.login(loginReq, "127.0.0.1");

        CreateReportRequest request = new CreateReportRequest(patient.getRefId());

        mockMvc.perform(post("/api/v1/reports")
                        .header("Authorization", "Bearer " + loginResponse.getAccessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.patientRefId").value(patient.getRefId()))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));
    }

    @Test
    @DisplayName("Test POST /api/v1/reports with mock user succeeds (200)")
    void testPostReports_WithMockUser() throws Exception {
        CreateReportRequest request = new CreateReportRequest(patient.getRefId());

        mockMvc.perform(post("/api/v1/reports")
                        .with(user(orgAdmin.getRefId()).roles("ORG_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.patientRefId").value(patient.getRefId()));
    }

    @Test
    @DisplayName("Test POST /api/v1/reports with SUPER_ADMIN fails with 403 Forbidden")
    void testPostReports_SuperAdmin_Forbidden() throws Exception {
        CreateReportRequest request = new CreateReportRequest(patient.getRefId());

        mockMvc.perform(post("/api/v1/reports")
                        .with(user("super-admin").roles("SUPER_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Test POST /api/v1/reports without authentication fails with 401 Unauthorized")
    void testPostReports_Unauthenticated_Unauthorized() throws Exception {
        CreateReportRequest request = new CreateReportRequest(patient.getRefId());

        mockMvc.perform(post("/api/v1/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Test POST /api/v1/reports with patient of another organization fails with 400 Bad Request")
    void testPostReports_CrossTenantPatient_BadRequest() throws Exception {
        Organization otherOrg = organizationRepository.save(Organization.builder()
                .name("Other Org")
                .code("OO-12345")
                .status(OrganizationStatus.ACTIVE)
                .build());

        Patient otherPatient = patientRepository.save(Patient.builder()
                .organization(otherOrg)
                .salutation(Salutation.MRS)
                .name("Other Patient")
                .patientCode("P-OTHER")
                .dateOfBirthKnown(true)
                .dateOfBirth(LocalDate.of(1985, 5, 5))
                .gender(Gender.FEMALE)
                .build());

        CreateReportRequest request = new CreateReportRequest(otherPatient.getRefId());

        mockMvc.perform(post("/api/v1/reports")
                        .with(user(orgAdmin.getRefId()).roles("ORG_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
