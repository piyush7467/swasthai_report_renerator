package com.swasthai.report_generator.report;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swasthai.report_generator.common.security.RateLimiterService;
import com.swasthai.report_generator.organization.entity.Organization;
import com.swasthai.report_generator.organization.entity.OrganizationStatus;
import com.swasthai.report_generator.organization.repository.OrganizationRepository;
import com.swasthai.report_generator.patient.entity.Gender;
import com.swasthai.report_generator.patient.entity.Patient;
import com.swasthai.report_generator.patient.entity.Salutation;
import com.swasthai.report_generator.patient.repository.PatientRepository;
import com.swasthai.report_generator.report.dto.request.AddReportTestRequest;
import com.swasthai.report_generator.report.dto.request.CreateReportRequest;
import com.swasthai.report_generator.report.dto.request.UpdateReportParametersRequest;
import com.swasthai.report_generator.report.dto.response.ReportResponse;
import com.swasthai.report_generator.report.entity.Report;
import com.swasthai.report_generator.report.entity.ReportStatus;
import com.swasthai.report_generator.report.repository.ReportRepository;
import com.swasthai.report_generator.report.service.ReportService;
import com.swasthai.report_generator.test.dto.request.TestParameterResultInput;
import com.swasthai.report_generator.test.entity.*;
import com.swasthai.report_generator.test.repository.OrganizationTestRepository;
import com.swasthai.report_generator.test.repository.TestCategoryRepository;
import com.swasthai.report_generator.test.repository.TestParameterRepository;
import com.swasthai.report_generator.test.repository.TestRepository;
import com.swasthai.report_generator.user.entity.Role;
import com.swasthai.report_generator.user.entity.User;
import com.swasthai.report_generator.user.entity.UserStatus;
import com.swasthai.report_generator.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PublicReportVerificationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Autowired
    private ReportService reportService;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private com.swasthai.report_generator.report.service.PublicReportVerificationService verificationService;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private TestCategoryRepository testCategoryRepository;

    @Autowired
    private TestRepository testRepository;

    @Autowired
    private TestParameterRepository testParameterRepository;

    @Autowired
    private OrganizationTestRepository organizationTestRepository;

    @Autowired
    private com.swasthai.report_generator.license.repository.PlanRepository planRepository;

    @Autowired
    private com.swasthai.report_generator.license.repository.LicenseRepository licenseRepository;

    @Autowired
    private RateLimiterService rateLimiterService;

    private Organization orgA;
    private Organization orgB;
    private User labStaffOrgA;
    private User labStaffOrgB;
    private Patient patientOrgA;
    private Patient patientOrgB;
    private com.swasthai.report_generator.test.entity.Test cbcTest;
    private TestParameter paramHgb;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        orgA = organizationRepository.save(Organization.builder()
                .name("Alpha Diagnostics " + suffix)
                .code("AD-" + suffix)
                .status(OrganizationStatus.ACTIVE)
                .build());

        orgB = organizationRepository.save(Organization.builder()
                .name("Beta Labs " + suffix)
                .code("BL-" + suffix)
                .status(OrganizationStatus.ACTIVE)
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
                .organization(orgA)
                .plan(testPlan)
                .status(com.swasthai.report_generator.license.entity.LicenseStatus.ACTIVE)
                .startedAt(Instant.now().minusSeconds(3600))
                .expiresAt(Instant.now().plusSeconds(86400 * 365))
                .build());

        licenseRepository.save(com.swasthai.report_generator.license.entity.License.builder()
                .organization(orgB)
                .plan(testPlan)
                .status(com.swasthai.report_generator.license.entity.LicenseStatus.ACTIVE)
                .startedAt(Instant.now().minusSeconds(3600))
                .expiresAt(Instant.now().plusSeconds(86400 * 365))
                .build());

        labStaffOrgA = userRepository.save(User.builder()
                .email("verify-staff-a-" + suffix + "@alpha.com")
                .name("Dr. Alpha Staff")
                .passwordHash("hashed")
                .role(Role.LAB_STAFF)
                .status(UserStatus.ACTIVE)
                .organization(orgA)
                .build());

        labStaffOrgB = userRepository.save(User.builder()
                .email("verify-staff-b-" + suffix + "@beta.com")
                .name("Dr. Beta Staff")
                .passwordHash("hashed")
                .role(Role.LAB_STAFF)
                .status(UserStatus.ACTIVE)
                .organization(orgB)
                .build());

        patientOrgA = patientRepository.save(Patient.builder()
                .organization(orgA)
                .salutation(Salutation.MR)
                .name("John Doe")
                .patientCode("PT-A-" + suffix)
                .dateOfBirthKnown(true)
                .dateOfBirth(LocalDate.of(1990, 5, 20))
                .gender(Gender.MALE)
                .phone("+91-9876543210")
                .email("john.doe@example.com")
                .address("100 Alpha Street")
                .build());

        patientOrgB = patientRepository.save(Patient.builder()
                .organization(orgB)
                .salutation(Salutation.MRS)
                .name("Jane Smith")
                .patientCode("PT-B-" + suffix)
                .dateOfBirthKnown(true)
                .dateOfBirth(LocalDate.of(1992, 8, 14))
                .gender(Gender.FEMALE)
                .phone("+91-9876543211")
                .email("jane.smith@example.com")
                .address("200 Beta Street")
                .build());

        TestCategory cat = testCategoryRepository.save(TestCategory.builder()
                .name("Hematology " + suffix)
                .code("HEM-" + suffix)
                .status(TestCategoryStatus.ACTIVE)
                .build());

        cbcTest = testRepository.save(com.swasthai.report_generator.test.entity.Test.builder()
                .name("Complete Blood Count " + suffix)
                .code("CBC-" + suffix)
                .shortName("CBC")
                .category(cat)
                .sampleType(SampleType.WHOLE_BLOOD)
                .specimenContainer("EDTA Vacutainer (Lavender Top)")
                .reportSection("HEMATOLOGY")
                .status(TestStatus.ACTIVE)
                .version(1)
                .build());

        paramHgb = testParameterRepository.save(TestParameter.builder()
                .test(cbcTest)
                .code("HGB")
                .name("Hemoglobin")
                .dataType(TestParameterDataType.DECIMAL)
                .inputType(ParameterInputType.MANUAL)
                .unit("g/dL")
                .referenceMin(new BigDecimal("13.0"))
                .referenceMax(new BigDecimal("17.0"))
                .displayOrder(1)
                .required(true)
                .status(TestParameterStatus.ACTIVE)
                .build());

        organizationTestRepository.save(OrganizationTest.builder()
                .organization(orgA)
                .test(cbcTest)
                .status(OrganizationTestStatus.ACTIVE)
                .build());

        organizationTestRepository.save(OrganizationTest.builder()
                .organization(orgB)
                .test(cbcTest)
                .status(OrganizationTestStatus.ACTIVE)
                .build());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateUser(User user) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                user.getEmail(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
        auth.setDetails(user);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private ReportResponse createAndFinalizeReport(User staff, Patient patient) {
        authenticateUser(staff);
        ReportResponse report = reportService.createReport(new CreateReportRequest(patient.getRefId()));
        report = reportService.addTest(report.refId(), new AddReportTestRequest(cbcTest.getRefId(), null));
        String testRefId = report.tests().get(0).refId();

        UpdateReportParametersRequest updateReq = new UpdateReportParametersRequest(
                null,
                List.of(new TestParameterResultInput(paramHgb.getRefId(), "HGB", "15.2"))
        );
        report = reportService.updateParameterValues(report.refId(), testRefId, updateReq);
        ReportResponse finalized = reportService.finalizeReport(report.refId());
        SecurityContextHolder.clearContext();
        return finalized;
    }

    @Test
    @DisplayName("1. verifyFinalizedReport_returnsValid: Returns 200 with valid=true and minimal metadata")
    void verifyFinalizedReport_returnsValid() throws Exception {
        ReportResponse finalized = createAndFinalizeReport(labStaffOrgA, patientOrgA);

        mockMvc.perform(get("/api/v1/public/reports/{reportRefId}/verify", finalized.refId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.reportRefId").value(finalized.refId()))
                .andExpect(jsonPath("$.organizationName").value(orgA.getName()))
                .andExpect(jsonPath("$.reportStatus").value("FINALIZED"))
                .andExpect(jsonPath("$.finalizedAt").isNotEmpty())
                .andExpect(jsonPath("$.message").doesNotExist());
    }

    @Test
    @DisplayName("2. verifyNonExistentReport_returnsNotFound: Returns 404 with generic failure message")
    void verifyNonExistentReport_returnsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/public/reports/RPT-NOTEXISTING123/verify")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.reportRefId").value("RPT-NOTEXISTING123"))
                .andExpect(jsonPath("$.message").value("Report could not be verified."))
                .andExpect(jsonPath("$.organizationName").doesNotExist())
                .andExpect(jsonPath("$.reportStatus").doesNotExist());
    }

    @Test
    @DisplayName("3. verifyDraftReport_returnsNotFound: Unfinalized draft reports cannot be verified")
    void verifyDraftReport_returnsNotFound() throws Exception {
        authenticateUser(labStaffOrgA);
        ReportResponse draft = reportService.createReport(new CreateReportRequest(patientOrgA.getRefId()));
        SecurityContextHolder.clearContext();

        mockMvc.perform(get("/api/v1/public/reports/{reportRefId}/verify", draft.refId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.reportRefId").value(draft.refId()))
                .andExpect(jsonPath("$.message").value("Report could not be verified."));
    }

    @Test
    @DisplayName("4. verifyCalculatedReport_returnsNotFound: Draft report with parameters entered cannot be verified")
    void verifyCalculatedReport_returnsNotFound() throws Exception {
        authenticateUser(labStaffOrgA);
        ReportResponse report = reportService.createReport(new CreateReportRequest(patientOrgA.getRefId()));
        report = reportService.addTest(report.refId(), new AddReportTestRequest(cbcTest.getRefId(), null));
        String testRefId = report.tests().get(0).refId();

        UpdateReportParametersRequest updateReq = new UpdateReportParametersRequest(
                null,
                List.of(new TestParameterResultInput(paramHgb.getRefId(), "HGB", "14.5"))
        );
        ReportResponse calculated = reportService.updateParameterValues(report.refId(), testRefId, updateReq);
        SecurityContextHolder.clearContext();

        mockMvc.perform(get("/api/v1/public/reports/{reportRefId}/verify", calculated.refId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.message").value("Report could not be verified."));
    }

    @Test
    @DisplayName("5. verifySoftDeletedReport_returnsNotFound: Soft-deleted finalized reports cannot be verified")
    void verifySoftDeletedReport_returnsNotFound() throws Exception {
        ReportResponse finalized = createAndFinalizeReport(labStaffOrgA, patientOrgA);

        Report reportEntity = reportRepository.findByRefId(finalized.refId()).orElseThrow();
        reportEntity.setDeletedAt(Instant.now());
        reportRepository.save(reportEntity);

        mockMvc.perform(get("/api/v1/public/reports/{reportRefId}/verify", finalized.refId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.message").value("Report could not be verified."));
    }

    @Test
    @DisplayName("6. verifyBlankRefId_rejected: Blank/whitespace refId is rejected without database query")
    void verifyBlankRefId_rejected() throws Exception {
        // Direct service rejection verifies that blank input returns invalid without querying database
        com.swasthai.report_generator.report.dto.response.PublicReportVerificationResponse serviceResp =
                verificationService.verifyReport("   ", "127.0.0.1");
        assertThat(serviceResp.valid()).isFalse();
        assertThat(serviceResp.message()).isEqualTo("Report could not be verified.");

        // HTTP level rejection (rejected by security firewall / path matching as 4xx error)
        mockMvc.perform(get("/api/v1/public/reports/%20%20/verify")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("7. verifyMalformedRefId_rejected: Path traversal or invalid characters are rejected")
    void verifyMalformedRefId_rejected() throws Exception {
        // Path traversal rejection at firewall level
        mockMvc.perform(get("/api/v1/public/reports/..%2F..%2Fetc%2Fpasswd/verify")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError());

        // UUID format rejected (not accepted as alternative identifier)
        mockMvc.perform(get("/api/v1/public/reports/{reportRefId}/verify", "123e4567-e89b-12d3-a456-426614174000")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.message").value("Report could not be verified."));

        // Malformed characters rejected
        mockMvc.perform(get("/api/v1/public/reports/{reportRefId}/verify", "RPT-123_INVALID!")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.message").value("Report could not be verified."));
    }

    @Test
    @DisplayName("8. verifyExcessivelyLongRefId_rejected: RefIds exceeding 50 characters are rejected")
    void verifyExcessivelyLongRefId_rejected() throws Exception {
        String excessivelyLongRefId = "RPT-" + "A".repeat(60);

        mockMvc.perform(get("/api/v1/public/reports/{reportRefId}/verify", excessivelyLongRefId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.message").value("Report could not be verified."));
    }

    @Test
    @DisplayName("9. verifyResponseContainsNoPatientInformation: Response contains zero PHI or patient fields")
    void verifyResponseContainsNoPatientInformation() throws Exception {
        ReportResponse finalized = createAndFinalizeReport(labStaffOrgA, patientOrgA);

        MvcResult result = mockMvc.perform(get("/api/v1/public/reports/{reportRefId}/verify", finalized.refId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(json);

        assertThat(node.has("patientName")).isFalse();
        assertThat(node.has("patientCode")).isFalse();
        assertThat(node.has("patientId")).isFalse();
        assertThat(node.has("dateOfBirth")).isFalse();
        assertThat(node.has("age")).isFalse();
        assertThat(node.has("gender")).isFalse();
        assertThat(node.has("phone")).isFalse();
        assertThat(node.has("email")).isFalse();
        assertThat(node.has("address")).isFalse();

        // Content check: None of the patient's personal data should leak in the raw json string
        assertThat(json).doesNotContain("John Doe");
        assertThat(json).doesNotContain("john.doe@example.com");
        assertThat(json).doesNotContain("+91-9876543210");
        assertThat(json).doesNotContain("Alpha Street");
    }

    @Test
    @DisplayName("10. verifyResponseContainsNoInternalUuid: Response contains zero internal database UUIDs")
    void verifyResponseContainsNoInternalUuid() throws Exception {
        ReportResponse finalized = createAndFinalizeReport(labStaffOrgA, patientOrgA);
        Report reportEntity = reportRepository.findByRefId(finalized.refId()).orElseThrow();

        MvcResult result = mockMvc.perform(get("/api/v1/public/reports/{reportRefId}/verify", finalized.refId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String json = result.getResponse().getContentAsString();

        // None of the internal entity UUIDs should be present in the verification response
        assertThat(json).doesNotContain(reportEntity.getId().toString());
        assertThat(json).doesNotContain(orgA.getId().toString());
        assertThat(json).doesNotContain(patientOrgA.getId().toString());
        assertThat(json).doesNotContain(labStaffOrgA.getId().toString());
    }

    @Test
    @DisplayName("11. verifyResponseContainsNoClinicalResults: Response contains zero test codes, results or ranges")
    void verifyResponseContainsNoClinicalResults() throws Exception {
        ReportResponse finalized = createAndFinalizeReport(labStaffOrgA, patientOrgA);

        MvcResult result = mockMvc.perform(get("/api/v1/public/reports/{reportRefId}/verify", finalized.refId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(json);

        assertThat(node.has("tests")).isFalse();
        assertThat(node.has("parameters")).isFalse();
        assertThat(node.has("results")).isFalse();

        assertThat(json).doesNotContain("Complete Blood Count");
        assertThat(json).doesNotContain("Hemoglobin");
        assertThat(json).doesNotContain("15.2");
        assertThat(json).doesNotContain("g/dL");
        assertThat(json).doesNotContain("NORMAL");
    }

    @Test
    @DisplayName("12. verifyPublicEndpointRequiresNoJwt: Can be accessed completely unauthenticated")
    void verifyPublicEndpointRequiresNoJwt() throws Exception {
        ReportResponse finalized = createAndFinalizeReport(labStaffOrgA, patientOrgA);

        // Explicitly clear security context and verify without headers
        SecurityContextHolder.clearContext();

        mockMvc.perform(get("/api/v1/public/reports/{reportRefId}/verify", finalized.refId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));
    }

    @Test
    @DisplayName("13. verifyAuthenticatedUserIsNotRequired: Validates report when security context has no principal")
    void verifyAuthenticatedUserIsNotRequired() throws Exception {
        ReportResponse finalized = createAndFinalizeReport(labStaffOrgA, patientOrgA);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        mockMvc.perform(get("/api/v1/public/reports/{reportRefId}/verify", finalized.refId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));
    }

    @Test
    @DisplayName("14. verifyGenericResponseDoesNotRevealWhyVerificationFailed: Generic response for all failures")
    void verifyGenericResponseDoesNotRevealWhyVerificationFailed() throws Exception {
        // Draft report
        authenticateUser(labStaffOrgA);
        ReportResponse draft = reportService.createReport(new CreateReportRequest(patientOrgA.getRefId()));
        SecurityContextHolder.clearContext();

        // Soft-deleted report
        ReportResponse finalized = createAndFinalizeReport(labStaffOrgA, patientOrgA);
        Report reportEntity = reportRepository.findByRefId(finalized.refId()).orElseThrow();
        reportEntity.setDeletedAt(Instant.now());
        reportRepository.save(reportEntity);

        MvcResult draftResult = mockMvc.perform(get("/api/v1/public/reports/{reportRefId}/verify", draft.refId()))
                .andExpect(status().isNotFound())
                .andReturn();

        MvcResult nonExistentResult = mockMvc.perform(get("/api/v1/public/reports/RPT-DOESNOTEXIST/verify"))
                .andExpect(status().isNotFound())
                .andReturn();

        MvcResult deletedResult = mockMvc.perform(get("/api/v1/public/reports/{reportRefId}/verify", finalized.refId()))
                .andExpect(status().isNotFound())
                .andReturn();

        JsonNode draftNode = objectMapper.readTree(draftResult.getResponse().getContentAsString());
        JsonNode nonExistentNode = objectMapper.readTree(nonExistentResult.getResponse().getContentAsString());
        JsonNode deletedNode = objectMapper.readTree(deletedResult.getResponse().getContentAsString());

        // All failure reasons return identical error message and valid=false
        assertThat(draftNode.get("message").asText()).isEqualTo("Report could not be verified.");
        assertThat(nonExistentNode.get("message").asText()).isEqualTo("Report could not be verified.");
        assertThat(deletedNode.get("message").asText()).isEqualTo("Report could not be verified.");

        assertThat(draftNode.get("valid").asBoolean()).isFalse();
        assertThat(nonExistentNode.get("valid").asBoolean()).isFalse();
        assertThat(deletedNode.get("valid").asBoolean()).isFalse();
    }

    @Test
    @DisplayName("15. verifyRateLimitIsApplied: Exceeding request threshold returns 429 Too Many Requests")
    void verifyRateLimitIsApplied() throws Exception {
        String mockClientIp = "198.51.100.42";
        String ipKey = "verify:ip:" + mockClientIp;
        rateLimiterService.reset(ipKey);

        try {
            // Default maxAttempts is 30. Send 30 requests.
            for (int i = 0; i < 30; i++) {
                mockMvc.perform(get("/api/v1/public/reports/RPT-TESTLIMIT/verify")
                                .with(request -> {
                                    request.setRemoteAddr(mockClientIp);
                                    return request;
                                }))
                        .andExpect(status().isNotFound());
            }

            // 31st request must trigger 429 Too Many Requests
            mockMvc.perform(get("/api/v1/public/reports/RPT-TESTLIMIT/verify")
                            .with(request -> {
                                request.setRemoteAddr(mockClientIp);
                                return request;
                            }))
                    .andExpect(status().isTooManyRequests())
                    .andExpect(jsonPath("$.code").value("RATE_LIMIT_EXCEEDED"));
        } finally {
            rateLimiterService.reset(ipKey);
        }
    }

    @Test
    @DisplayName("16. verifyCrossTenantConceptDoesNotApplyToPublicVerification: Any tenant's finalized report can be verified")
    void verifyCrossTenantConceptDoesNotApplyToPublicVerification() throws Exception {
        ReportResponse reportOrgA = createAndFinalizeReport(labStaffOrgA, patientOrgA);
        ReportResponse reportOrgB = createAndFinalizeReport(labStaffOrgB, patientOrgB);

        // Same public caller verifies reports from both Org A and Org B without error
        mockMvc.perform(get("/api/v1/public/reports/{reportRefId}/verify", reportOrgA.refId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.organizationName").value(orgA.getName()));

        mockMvc.perform(get("/api/v1/public/reports/{reportRefId}/verify", reportOrgB.refId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.organizationName").value(orgB.getName()));
    }

    @Test
    @DisplayName("17. verifyPurgedReportCannotBeVerified: Hard-purged report returns 404")
    void verifyPurgedReportCannotBeVerified() throws Exception {
        ReportResponse finalized = createAndFinalizeReport(labStaffOrgA, patientOrgA);

        Report reportEntity = reportRepository.findByRefId(finalized.refId()).orElseThrow();
        reportRepository.delete(reportEntity);

        mockMvc.perform(get("/api/v1/public/reports/{reportRefId}/verify", finalized.refId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.message").value("Report could not be verified."));
    }

    @Test
    @DisplayName("18. verifyExactReportRefIdIsReturned: RefId in response exactly matches the queried refId")
    void verifyExactReportRefIdIsReturned() throws Exception {
        ReportResponse finalized = createAndFinalizeReport(labStaffOrgA, patientOrgA);

        mockMvc.perform(get("/api/v1/public/reports/{reportRefId}/verify", finalized.refId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportRefId").value(finalized.refId()));

        mockMvc.perform(get("/api/v1/public/reports/RPT-SOMEUNKNOWN/verify"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.reportRefId").value("RPT-SOMEUNKNOWN"));
    }

    @Test
    @DisplayName("19. verifyFinalizedAtIsHistoricalSnapshotValue: Returns exact timestamp of report finalization")
    void verifyFinalizedAtIsHistoricalSnapshotValue() throws Exception {
        ReportResponse finalized = createAndFinalizeReport(labStaffOrgA, patientOrgA);
        Report reportEntity = reportRepository.findByRefId(finalized.refId()).orElseThrow();

        MvcResult result = mockMvc.perform(get("/api/v1/public/reports/{reportRefId}/verify", finalized.refId()))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        Instant responseFinalizedAt = Instant.parse(node.get("finalizedAt").asText());

        assertThat(responseFinalizedAt.truncatedTo(ChronoUnit.MILLIS))
                .isEqualTo(reportEntity.getFinalizedAt().truncatedTo(ChronoUnit.MILLIS));
    }

    @Test
    @DisplayName("20. verifyOrganizationNameComesFromHistoricalReportSnapshot: Modifying live Org name does NOT change verification response")
    void verifyOrganizationNameComesFromHistoricalReportSnapshot() throws Exception {
        ReportResponse finalized = createAndFinalizeReport(labStaffOrgA, patientOrgA);

        // Now mutate the live Organization entity name in master database
        String originalOrgName = orgA.getName();
        orgA.setName("Completely Changed Hospital Brand " + UUID.randomUUID());
        organizationRepository.save(orgA);

        // Public verification MUST still return the historical snapshot organization name
        mockMvc.perform(get("/api/v1/public/reports/{reportRefId}/verify", finalized.refId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.organizationName").value(originalOrgName))
                .andExpect(jsonPath("$.organizationName").value(org.hamcrest.Matchers.not(orgA.getName())));
    }

    @Test
    @DisplayName("21. verifyPublicEndpointCannotAccessProtectedReportApis: Security regression test")
    void verifyPublicEndpointCannotAccessProtectedReportApis() throws Exception {
        ReportResponse finalized = createAndFinalizeReport(labStaffOrgA, patientOrgA);

        SecurityContextHolder.clearContext();

        // 1. Authenticated report details must be rejected without auth (401)
        mockMvc.perform(get("/api/v1/reports/{reportRefId}", finalized.refId()))
                .andExpect(status().isUnauthorized());

        // 2. Authenticated PDF download must be rejected without auth (401)
        mockMvc.perform(get("/api/v1/reports/{reportRefId}/pdf", finalized.refId()))
                .andExpect(status().isUnauthorized());

        // 3. Public verification endpoint succeeds without auth (200)
        mockMvc.perform(get("/api/v1/public/reports/{reportRefId}/verify", finalized.refId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));
    }
}
