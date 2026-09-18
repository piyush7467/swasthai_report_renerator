package com.swasthai.report_generator.report.pdf;

import com.swasthai.report_generator.report.entity.Report;
import com.swasthai.report_generator.report.entity.ReportStatus;
import com.swasthai.report_generator.report.entity.ReportTestResult;
import com.swasthai.report_generator.test.entity.TestParameterResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ReportPdfDataBuilder {

    /*
     * IMPORTANT:
     *
     * This class must NEVER query:
     *
     * - current Organization
     * - current OrganizationProfile
     * - current Patient
     * - current User
     * - current Test
     * - current TestParameter
     *
     * A finalized report is a historical document.
     *
     * Therefore the PDF must be generated exclusively
     * from values already snapshotted into Report,
     * ReportTestResult and TestParameterResult.
     */

    private static final String VERIFICATION_BASE_URL =
            "https://verify.swasthai.com/reports/";

    public ReportPdfData build(Report report) {

        if (report == null) {
            throw new IllegalArgumentException(
                    "Report cannot be null"
            );
        }

        if (report.getStatus() != ReportStatus.FINALIZED) {
            throw new IllegalStateException(
                    "PDF can only be generated for finalized reports"
            );
        }

        validateReportSnapshot(report);

        OrganizationPdfSnapshot organizationSnapshot =
                OrganizationPdfSnapshot.builder()
                        .name(report.getOrganizationName())

                        .addressLine1(
                                report.getOrganizationAddressLine1()
                        )
                        .addressLine2(
                                report.getOrganizationAddressLine2()
                        )
                        .city(
                                report.getOrganizationCity()
                        )
                        .state(
                                report.getOrganizationState()
                        )
                        .postalCode(
                                report.getOrganizationPostalCode()
                        )
                        .country(
                                report.getOrganizationCountry()
                        )

                        .phone(
                                report.getOrganizationPhone()
                        )
                        .alternatePhone(
                                report.getOrganizationAlternatePhone()
                        )
                        .email(
                                report.getOrganizationEmail()
                        )
                        .website(
                                report.getOrganizationWebsite()
                        )

                        .logoStorageKey(
                                report.getOrganizationLogoStorageKey()
                        )

                        .signatureStorageKey(
                                report.getOrganizationSignatureStorageKey()
                        )
                        .signatureOwnerName(
                                report.getOrganizationSignatureOwnerName()
                        )
                        .signatureOwnerEmail(
                                report.getOrganizationSignatureOwnerEmail()
                        )

                        .reportFooterText(
                                report.getOrganizationReportFooterText()
                        )
                        .reportDisclaimer(
                                report.getOrganizationReportDisclaimer()
                        )

                        .build();

        PatientPdfSnapshot patientSnapshot =
                PatientPdfSnapshot.builder()
                        .patientCode(
                                report.getPatientCode()
                        )
                        .name(
                                report.getPatientName()
                        )
                        .salutation(
                                report.getPatientSalutation()
                        )
                        .gender(
                                report.getPatientGender()
                        )
                        .dateOfBirthKnown(
                                report.getPatientDateOfBirthKnown()
                        )
                        .dateOfBirth(
                                report.getPatientDateOfBirth()
                        )
                        .ageValue(
                                report.getPatientAgeAtReportingValue()
                        )
                        .ageUnit(
                                report.getPatientAgeAtReportingUnit()
                        )
                        .phone(
                                report.getPatientPhone()
                        )
                        .email(
                                report.getPatientEmail()
                        )
                        .address(
                                report.getPatientAddress()
                        )
                        .weightKg(
                                report.getPatientWeightKg()
                        )
                        .build();

        UserPdfSnapshot createdBySnapshot =
                UserPdfSnapshot.builder()
                        .name(
                                report.getCreatedByName()
                        )
                        .email(
                                report.getCreatedByEmail()
                        )
                        .build();

        UserPdfSnapshot finalizedBySnapshot =
                UserPdfSnapshot.builder()
                        .name(
                                report.getFinalizedByName()
                        )
                        .email(
                                report.getFinalizedByEmail()
                        )
                        .build();

        List<TestPdfItem> testItems =
                report.getTests() == null
                        ? Collections.emptyList()
                        : report.getTests()
                        .stream()
                        .sorted(
                                Comparator.comparing(
                                        ReportTestResult::getDisplayOrder,
                                        Comparator.nullsLast(
                                                Integer::compareTo
                                        )
                                )
                        )
                        .map(this::buildTestPdfItem)
                        .toList();

        String verificationUrl = VERIFICATION_BASE_URL + report.getRefId().trim();
        if (isBlank(verificationUrl)) {
            throw new IllegalStateException(
                    "Finalized report verification URL cannot be blank"
            );
        }

        return ReportPdfData.builder()
                .reportRefId(
                        report.getRefId()
                )
                .status(
                        report.getStatus()
                )
                .reportVersion(
                        report.getReportVersion()
                )
                .createdAt(
                        report.getCreatedAt()
                )
                .finalizedAt(
                        report.getFinalizedAt()
                )
                .organization(
                        organizationSnapshot
                )
                .patient(
                        patientSnapshot
                )
                .createdBy(
                        createdBySnapshot
                )
                .finalizedBy(
                        finalizedBySnapshot
                )
                .tests(
                        testItems
                )
                .verificationUrl(
                        verificationUrl
                )
                .includeOrganizationHeader(
                        Boolean.TRUE.equals(
                                report.getIncludeOrganizationHeader()
                        )
                )
                .build();
    }

    private TestPdfItem buildTestPdfItem(
            ReportTestResult reportTest
    ) {

        if (reportTest == null) {
            throw new IllegalStateException(
                    "Finalized report contains a null test result"
            );
        }

        validateTestSnapshot(reportTest);

        List<ParameterPdfItem> parameterItems =
                reportTest.getParameterResults() == null
                        ? Collections.emptyList()
                        : reportTest.getParameterResults()
                        .stream()
                        .sorted(
                                Comparator.comparing(
                                        TestParameterResult::getDisplayOrder,
                                        Comparator.nullsLast(
                                                Integer::compareTo
                                        )
                                )
                        )
                        .map(this::buildParameterPdfItem)
                        .toList();

        return TestPdfItem.builder()
                .testCode(
                        reportTest.getTestCode()
                )
                .testName(
                        reportTest.getTestName()
                )
                .testShortName(
                        reportTest.getTestShortName()
                )
                .sampleType(
                        reportTest.getSampleType()
                )
                .customSampleType(
                        reportTest.getCustomSampleType()
                )
                .specimenContainer(
                        reportTest.getSpecimenContainer()
                )
                .reportSection(
                        reportTest.getReportSection()
                )
                .displayOrder(
                        reportTest.getDisplayOrder()
                )
                .testVersion(
                        reportTest.getTestVersion()
                )
                .parameters(
                        parameterItems
                )
                .build();
    }

    private ParameterPdfItem buildParameterPdfItem(
            TestParameterResult paramResult
    ) {

        if (paramResult == null) {
            throw new IllegalStateException(
                    "Finalized report contains a null parameter result"
            );
        }

        /*
         * ResultFlag stored in the database is authoritative.
         *
         * NEVER calculate a flag while generating the PDF.
         */
        return ParameterPdfItem.builder()
                .parameterCode(
                        paramResult.getParameterCode()
                )
                .parameterName(
                        paramResult.getParameterName()
                )
                .dataType(
                        paramResult.getDataType()
                )
                .inputType(
                        paramResult.getInputType()
                )
                .calculationType(
                        paramResult.getCalculationType()
                )
                .calculationVersion(
                        paramResult.getCalculationVersion()
                )
                .unit(
                        paramResult.getUnit()
                )
                .value(
                        paramResult.getValue()
                )
                
                .numericValue(
                        paramResult.getNumericValue()
                )
                .flag(
                        paramResult.getFlag()
                )
                .referenceMin(
                        paramResult.getReferenceMin()
                )
                .referenceMax(
                        paramResult.getReferenceMax()
                )
                .criticalLow(
                        paramResult.getCriticalLow()
                )
                .criticalHigh(
                        paramResult.getCriticalHigh()
                )
                .displayOrder(
                        paramResult.getDisplayOrder()
                )
                .build();
    }

    private void validateReportSnapshot(
            Report report
    ) {

        if (isBlank(report.getRefId())) {
            throw new IllegalStateException(
                    "Finalized report is missing report reference ID"
            );
        }

        if (isBlank(report.getOrganizationName())) {
            throw new IllegalStateException(
                    "Finalized report is missing organization snapshot"
            );
        }

        if (isBlank(report.getPatientName())) {
            throw new IllegalStateException(
                    "Finalized report is missing patient name snapshot"
            );
        }

        if (isBlank(report.getPatientCode())) {
            throw new IllegalStateException(
                    "Finalized report is missing patient code snapshot"
            );
        }

        if (isBlank(report.getPatientGender())) {
            throw new IllegalStateException(
                    "Finalized report is missing patient gender snapshot"
            );
        }

        if (isBlank(report.getCreatedByName())) {
            throw new IllegalStateException(
                    "Finalized report is missing creator snapshot"
            );
        }

        if (isBlank(report.getFinalizedByName())) {
            throw new IllegalStateException(
                    "Finalized report is missing finalizer snapshot"
            );
        }

        if (report.getFinalizedAt() == null) {
            throw new IllegalStateException(
                    "Finalized report is missing finalized timestamp"
            );
        }

        if (report.getTests() == null
                || report.getTests().isEmpty()) {

            throw new IllegalStateException(
                    "Finalized report contains no test results"
            );
        }

        /*
         * Signature is optional.
         *
         * But if a signature asset exists, its historical
         * authorized owner MUST also exist.
         */
        if (!isBlank(
                report.getOrganizationSignatureStorageKey()
        )
                && isBlank(
                report.getOrganizationSignatureOwnerName()
        )) {

            throw new IllegalStateException(
                    "Finalized report signature is missing authorized owner snapshot"
            );
        }
    }

    private void validateTestSnapshot(
            ReportTestResult reportTest
    ) {

        if (isBlank(reportTest.getTestCode())) {
            throw new IllegalStateException(
                    "Finalized report test is missing test code snapshot"
            );
        }

        if (isBlank(reportTest.getTestName())) {
            throw new IllegalStateException(
                    "Finalized report test is missing test name snapshot"
            );
        }

        if (reportTest.getParameterResults() == null) {
            throw new IllegalStateException(
                    "Finalized report test is missing parameter results"
            );
        }
    }

    private boolean isBlank(String value) {

        return value == null || value.isBlank();
    }
}