package com.swasthai.report_generator.test.service.impl;

import com.swasthai.report_generator.common.exception.ResourceAlreadyExistsException;
import com.swasthai.report_generator.common.exception.ResourceNotFoundException;
import com.swasthai.report_generator.common.util.RefIdGenerator;
import com.swasthai.report_generator.test.dto.request.CreateTestRequest;
import com.swasthai.report_generator.test.dto.request.UpdateTestRequest;
import com.swasthai.report_generator.test.dto.response.TestResponse;
import com.swasthai.report_generator.test.entity.SampleType;
import com.swasthai.report_generator.test.entity.Test;
import com.swasthai.report_generator.test.entity.TestCategory;
import com.swasthai.report_generator.test.entity.TestCategoryStatus;
import com.swasthai.report_generator.test.entity.TestStatus;
import com.swasthai.report_generator.test.repository.TestCategoryRepository;
import com.swasthai.report_generator.test.repository.TestRepository;
import com.swasthai.report_generator.test.specification.TestSpecification;
import com.swasthai.report_generator.test.service.TestService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class TestServiceImpl implements TestService {

    private final TestRepository testRepository;
    private final TestCategoryRepository testCategoryRepository;

    // ============================================================
    // CREATE SINGLE TEST
    // ============================================================

    @Override
    public TestResponse createTest(CreateTestRequest request) {

        validateSampleType(
                request.sampleType(),
                request.customSampleType()
        );

        validateEffectiveDates(
                request.effectiveFrom(),
                request.effectiveUntil()
        );

        String code = normalizeCode(request.code());
        String name = normalize(request.name());

        validateDuplicateForCreate(code, name);

        TestCategory category =
                getActiveCategory(request.categoryRefId());

        Test test = Test.builder()
                .refId(RefIdGenerator.generate("TEST"))

                // Identity
                .category(category)
                .code(code)
                .name(name)
                .shortName(normalize(request.shortName()))
                .testType(request.testType())
                .description(normalize(request.description()))

                // Sample
                .sampleType(request.sampleType())
                .customSampleType(
                        normalize(request.customSampleType())
                )
                .specimenContainer(
                        normalize(request.specimenContainer())
                )
                .sampleVolume(request.sampleVolume())
                .sampleVolumeUnit(
                        normalize(request.sampleVolumeUnit())
                )
                .fastingRequired(
                        Boolean.TRUE.equals(
                                request.fastingRequired()
                        )
                )
                .patientPreparation(
                        normalize(request.patientPreparation())
                )
                .collectionInstructions(
                        normalize(request.collectionInstructions())
                )

                // Processing
                .turnaroundTimeHours(
                        request.turnaroundTimeHours()
                )
                .prioritySupported(
                        Boolean.TRUE.equals(
                                request.prioritySupported()
                        )
                )
                .outsourced(
                        Boolean.TRUE.equals(
                                request.outsourced()
                        )
                )
                .laboratoryInstructions(
                        normalize(request.laboratoryInstructions())
                )

                // Reporting
                .reportSection(
                        normalize(request.reportSection())
                )
                .displayOrder(
                        request.displayOrder() != null
                                ? request.displayOrder()
                                : 0
                )
                .reportDescription(
                        normalize(request.reportDescription())
                )
                .interpretationGuidance(
                        normalize(request.interpretationGuidance())
                )

                // Commercial
                .basePrice(request.basePrice())
                .currency(
                        request.currency() != null
                                ? request.currency()
                                        .trim()
                                        .toUpperCase()
                                : "INR"
                )
                .billingCode(
                        request.billingCode() != null
                                ? request.billingCode()
                                        .trim()
                                        .toUpperCase()
                                : null
                )

                // Lifecycle
                .status(TestStatus.ACTIVE)
                .version(1)
                .effectiveFrom(request.effectiveFrom())
                .effectiveUntil(request.effectiveUntil())

                .build();

        Test savedTest = testRepository.save(test);

        return mapToResponse(savedTest);
    }

    // ============================================================
    // BULK CREATE
    // ============================================================

    @Override
    public List<TestResponse> createTestsBulk(
            List<CreateTestRequest> requests
    ) {

        if (requests == null || requests.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one test is required"
            );
        }

        if (requests.size() > 100) {
            throw new IllegalArgumentException(
                    "A maximum of 100 tests can be created at once"
            );
        }

        /*
         * Validate duplicates inside the same request first.
         *
         * This prevents:
         *
         * CBC
         * CBC
         *
         * from reaching the database.
         */
        validateBulkDuplicates(requests);

        /*
         * createTest() performs all normal validations.
         *
         * The class is transactional, therefore if any test
         * fails, the complete bulk operation is rolled back.
         */
        return requests.stream()
                .map(this::createTest)
                .toList();
    }

    // ============================================================
    // GET BY REF ID
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public TestResponse getTest(String testRefId) {

        Test test = testRepository.findByRefId(testRefId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Test not found"
                        )
                );

        return mapToResponse(test);
    }

    // ============================================================
    // GET / SEARCH / FILTER
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public Page<TestResponse> getTests(
            String search,
            String categoryRefId,
            String status,
            Pageable pageable
    ) {

        Specification<Test> specification =
                Specification.allOf();

        // --------------------------------------------------------
        // Search
        // --------------------------------------------------------

        if (search != null && !search.isBlank()) {

            specification = specification.and(
                    TestSpecification.search(search)
            );
        }

        // --------------------------------------------------------
        // Category filter
        // --------------------------------------------------------

        if (categoryRefId != null
                && !categoryRefId.isBlank()) {

            /*
             * Validate category refId.
             *
             * We intentionally only validate existence here.
             * An inactive category may still need to be visible
             * to SUPER_ADMIN while managing the master catalog.
             */
            getCategory(categoryRefId);

            specification = specification.and(
                    TestSpecification.belongsToCategory(
                            categoryRefId
                    )
            );
        }

        // --------------------------------------------------------
        // Status filter
        // --------------------------------------------------------

        if (status != null && !status.isBlank()) {

            TestStatus testStatus =
                    parseStatus(status);

            specification = specification.and(
                    TestSpecification.hasStatus(
                            testStatus
                    )
            );
        }

        return testRepository
                .findAll(specification, pageable)
                .map(this::mapToResponse);
    }

    // ============================================================
    // UPDATE
    // ============================================================

    @Override
    public TestResponse updateTest(
            String testRefId,
            UpdateTestRequest request
    ) {

        Test test = testRepository.findByRefId(testRefId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Test not found"
                        )
                );

        // --------------------------------------------------------
        // CODE
        // --------------------------------------------------------

        if (request.code() != null) {

            String newCode =
                    normalizeCode(request.code());

            if (!newCode.equals(test.getCode())
                    && testRepository
                    .existsByCodeAndIdNot(
                            newCode,
                            test.getId()
                    )) {

                throw new ResourceAlreadyExistsException(
                        "Test with code '"
                                + newCode
                                + "' already exists"
                );
            }

            test.setCode(newCode);
        }

        // --------------------------------------------------------
        // NAME
        // --------------------------------------------------------

        if (request.name() != null) {

            String newName =
                    normalize(request.name());

            if (!newName.equalsIgnoreCase(
                    test.getName()
            )
                    && testRepository
                    .existsByNameIgnoreCaseAndIdNot(
                            newName,
                            test.getId()
                    )) {

                throw new ResourceAlreadyExistsException(
                        "Test with name '"
                                + newName
                                + "' already exists"
                );
            }

            test.setName(newName);
        }

        // --------------------------------------------------------
        // SHORT NAME
        // --------------------------------------------------------

        if (request.shortName() != null) {

            test.setShortName(
                    normalize(request.shortName())
            );
        }

        // --------------------------------------------------------
        // CATEGORY
        // --------------------------------------------------------

        if (request.categoryRefId() != null) {

            test.setCategory(
                    getActiveCategory(
                            request.categoryRefId()
                    )
            );
        }

        // --------------------------------------------------------
        // TEST TYPE
        // --------------------------------------------------------

        if (request.testType() != null) {

            test.setTestType(
                    request.testType()
            );
        }

        // --------------------------------------------------------
        // DESCRIPTION
        // --------------------------------------------------------

        if (request.description() != null) {

            test.setDescription(
                    normalize(request.description())
            );
        }

        // --------------------------------------------------------
        // SAMPLE TYPE
        // --------------------------------------------------------

        /*
         * Calculate the final sample type first.
         *
         * This is important for PATCH requests.
         */
        SampleType finalSampleType =
                request.sampleType() != null
                        ? request.sampleType()
                        : test.getSampleType();

        /*
         * Calculate the final custom sample type.
         *
         * If the request supplies it, use the request value.
         * Otherwise retain the existing value temporarily.
         */
        String finalCustomSampleType =
                request.customSampleType() != null
                        ? normalize(
                                request.customSampleType()
                        )
                        : test.getCustomSampleType();

        /*
         * If sample type is anything other than OTHER,
         * customSampleType MUST be null.
         */
        if (finalSampleType != SampleType.OTHER) {
            finalCustomSampleType = null;
        }

        /*
         * If sample type is OTHER, customSampleType is mandatory.
         */
        if (finalSampleType == SampleType.OTHER
                && (finalCustomSampleType == null
                || finalCustomSampleType.isBlank())) {

            throw new IllegalArgumentException(
                    "Custom sample type is required when sample type is OTHER"
            );
        }

        test.setSampleType(finalSampleType);
        test.setCustomSampleType(finalCustomSampleType);

        // --------------------------------------------------------
        // SPECIMEN
        // --------------------------------------------------------

        if (request.specimenContainer() != null) {

            test.setSpecimenContainer(
                    normalize(
                            request.specimenContainer()
                    )
            );
        }

        if (request.sampleVolume() != null) {

            test.setSampleVolume(
                    request.sampleVolume()
            );
        }

        if (request.sampleVolumeUnit() != null) {

            test.setSampleVolumeUnit(
                    normalize(
                            request.sampleVolumeUnit()
                    )
            );
        }

        if (request.fastingRequired() != null) {

            test.setFastingRequired(
                    request.fastingRequired()
            );
        }

        if (request.patientPreparation() != null) {

            test.setPatientPreparation(
                    normalize(
                            request.patientPreparation()
                    )
            );
        }

        if (request.collectionInstructions() != null) {

            test.setCollectionInstructions(
                    normalize(
                            request.collectionInstructions()
                    )
            );
        }

        // --------------------------------------------------------
        // PROCESSING
        // --------------------------------------------------------

        if (request.turnaroundTimeHours() != null) {

            test.setTurnaroundTimeHours(
                    request.turnaroundTimeHours()
            );
        }

        if (request.prioritySupported() != null) {

            test.setPrioritySupported(
                    request.prioritySupported()
            );
        }

        if (request.outsourced() != null) {

            test.setOutsourced(
                    request.outsourced()
            );
        }

        if (request.laboratoryInstructions() != null) {

            test.setLaboratoryInstructions(
                    normalize(
                            request.laboratoryInstructions()
                    )
            );
        }

        // --------------------------------------------------------
        // REPORTING
        // --------------------------------------------------------

        if (request.reportSection() != null) {

            test.setReportSection(
                    normalize(
                            request.reportSection()
                    )
            );
        }

        if (request.displayOrder() != null) {

            test.setDisplayOrder(
                    request.displayOrder()
            );
        }

        if (request.reportDescription() != null) {

            test.setReportDescription(
                    normalize(
                            request.reportDescription()
                    )
            );
        }

        if (request.interpretationGuidance() != null) {

            test.setInterpretationGuidance(
                    normalize(
                            request.interpretationGuidance()
                    )
            );
        }

        // --------------------------------------------------------
        // COMMERCIAL
        // --------------------------------------------------------

        if (request.basePrice() != null) {

            test.setBasePrice(
                    request.basePrice()
            );
        }

        if (request.currency() != null) {

            test.setCurrency(
                    request.currency()
                            .trim()
                            .toUpperCase()
            );
        }

        if (request.billingCode() != null) {

            test.setBillingCode(
                    request.billingCode()
                            .trim()
                            .toUpperCase()
            );
        }

        // --------------------------------------------------------
        // STATUS
        // --------------------------------------------------------

        if (request.status() != null) {

            test.setStatus(
                    request.status()
            );
        }

        // --------------------------------------------------------
        // EFFECTIVE DATES
        // --------------------------------------------------------

        if (request.effectiveFrom() != null) {

            test.setEffectiveFrom(
                    request.effectiveFrom()
            );
        }

        if (request.effectiveUntil() != null) {

            test.setEffectiveUntil(
                    request.effectiveUntil()
            );
        }

        // --------------------------------------------------------
        // FINAL VALIDATION
        // --------------------------------------------------------

        validateSampleType(
                test.getSampleType(),
                test.getCustomSampleType()
        );

        validateEffectiveDates(
                test.getEffectiveFrom(),
                test.getEffectiveUntil()
        );

        /*
         * Increment catalog version whenever the test is modified.
         */
        test.setVersion(
                test.getVersion() + 1
        );

        Test updatedTest =
                testRepository.save(test);

        return mapToResponse(updatedTest);
    }

    // ============================================================
    // SOFT DELETE
    // ============================================================

    @Override
    public void deleteTest(String testRefId) {

        Test test = testRepository.findByRefId(testRefId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Test not found"
                        )
                );

        if (test.getStatus()
                == TestStatus.INACTIVE) {

            throw new IllegalStateException(
                    "Test is already inactive"
            );
        }

        test.setStatus(
                TestStatus.INACTIVE
        );

        test.setVersion(
                test.getVersion() + 1
        );

        testRepository.save(test);
    }

    // ============================================================
    // CATEGORY HELPERS
    // ============================================================

    /**
     * Gets a category regardless of status.
     *
     * Used when filtering/viewing the master catalog.
     */
    private TestCategory getCategory(
            String categoryRefId
    ) {

        if (categoryRefId == null
                || categoryRefId.isBlank()) {

            throw new IllegalArgumentException(
                    "Test category refId is required"
            );
        }

        return testCategoryRepository
                .findByRefId(categoryRefId.trim())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Test category not found"
                        )
                );
    }

    /**
     * Gets only an ACTIVE category.
     *
     * Tests must never be newly assigned to an
     * inactive master category.
     */
    private TestCategory getActiveCategory(
            String categoryRefId
    ) {

        TestCategory category =
                getCategory(categoryRefId);

        if (category.getStatus()
                != TestCategoryStatus.ACTIVE) {

            throw new IllegalStateException(
                    "Cannot assign an inactive test category"
            );
        }

        return category;
    }

    // ============================================================
    // DUPLICATE VALIDATION
    // ============================================================

    private void validateDuplicateForCreate(
            String code,
            String name
    ) {

        if (testRepository.existsByCode(code)) {

            throw new ResourceAlreadyExistsException(
                    "Test with code '"
                            + code
                            + "' already exists"
            );
        }

        if (testRepository
                .existsByNameIgnoreCase(name)) {

            throw new ResourceAlreadyExistsException(
                    "Test with name '"
                            + name
                            + "' already exists"
            );
        }
    }

    /**
     * Detect duplicate codes/names within the same
     * bulk request before database operations begin.
     */
    private void validateBulkDuplicates(
            List<CreateTestRequest> requests
    ) {

        Set<String> codes =
                new HashSet<>();

        Set<String> names =
                new HashSet<>();

        for (CreateTestRequest request : requests) {

            if (request == null) {

                throw new IllegalArgumentException(
                        "Bulk request cannot contain null test"
                );
            }

            String code =
                    normalizeCode(request.code());

            String name =
                    normalize(request.name());

            // ----------------------------------------------
            // Duplicate code in same request
            // ----------------------------------------------

            if (!codes.add(code)) {

                throw new ResourceAlreadyExistsException(
                        "Duplicate test code in bulk request: "
                                + code
                );
            }

            // ----------------------------------------------
            // Duplicate name in same request
            // ----------------------------------------------

            String normalizedName =
                    name == null
                            ? null
                            : name.toLowerCase();

            if (!names.add(normalizedName)) {

                throw new ResourceAlreadyExistsException(
                        "Duplicate test name in bulk request: "
                                + name
                );
            }
        }
    }

    // ============================================================
    // SAMPLE VALIDATION
    // ============================================================

    private void validateSampleType(
            SampleType sampleType,
            String customSampleType
    ) {

        if (sampleType == null) {

            throw new IllegalArgumentException(
                    "Sample type is required"
            );
        }

        boolean hasCustomValue =
                customSampleType != null
                        && !customSampleType.isBlank();

        /*
         * OTHER requires a custom value.
         */
        if (sampleType == SampleType.OTHER
                && !hasCustomValue) {

            throw new IllegalArgumentException(
                    "Custom sample type is required when sample type is OTHER"
            );
        }

        /*
         * Standard sample types must not have
         * a custom sample type.
         */
        if (sampleType != SampleType.OTHER
                && hasCustomValue) {

            throw new IllegalArgumentException(
                    "Custom sample type can only be provided when sample type is OTHER"
            );
        }
    }

    // ============================================================
    // EFFECTIVE DATE VALIDATION
    // ============================================================

    private void validateEffectiveDates(
            LocalDate effectiveFrom,
            LocalDate effectiveUntil
    ) {

        if (effectiveFrom != null
                && effectiveUntil != null
                && effectiveUntil.isBefore(
                        effectiveFrom
                )) {

            throw new IllegalArgumentException(
                    "Effective until date cannot be before effective from date"
            );
        }
    }

    // ============================================================
    // STATUS PARSING
    // ============================================================

    private TestStatus parseStatus(
            String status
    ) {

        try {

            return TestStatus.valueOf(
                    status.trim().toUpperCase()
            );

        } catch (IllegalArgumentException exception) {

            throw new IllegalArgumentException(
                    "Invalid test status"
            );
        }
    }

    // ============================================================
    // NORMALIZATION
    // ============================================================

    private String normalize(
            String value
    ) {

        if (value == null) {
            return null;
        }

        String normalized =
                value.trim();

        return normalized.isBlank()
                ? null
                : normalized;
    }

    private String normalizeCode(
            String code
    ) {

        if (code == null) {
            return null;
        }

        return code
                .trim()
                .toUpperCase();
    }

    // ============================================================
    // ENTITY → RESPONSE
    // ============================================================

    private TestResponse mapToResponse(
            Test test
    ) {

        return TestResponse.builder()

                // ------------------------------------------------
                // Identity
                // ------------------------------------------------

                .refId(test.getRefId())

                .categoryRefId(
                        test.getCategory()
                                .getRefId()
                )

                .categoryName(
                        test.getCategory()
                                .getName()
                )

                .code(test.getCode())
                .name(test.getName())
                .shortName(test.getShortName())
                .testType(test.getTestType())
                .description(test.getDescription())

                // ------------------------------------------------
                // Sample
                // ------------------------------------------------

                .sampleType(test.getSampleType())
                .customSampleType(
                        test.getCustomSampleType()
                )
                .specimenContainer(
                        test.getSpecimenContainer()
                )
                .sampleVolume(
                        test.getSampleVolume()
                )
                .sampleVolumeUnit(
                        test.getSampleVolumeUnit()
                )
                .fastingRequired(
                        test.isFastingRequired()
                )
                .patientPreparation(
                        test.getPatientPreparation()
                )
                .collectionInstructions(
                        test.getCollectionInstructions()
                )

                // ------------------------------------------------
                // Processing
                // ------------------------------------------------

                .turnaroundTimeHours(
                        test.getTurnaroundTimeHours()
                )
                .prioritySupported(
                        test.isPrioritySupported()
                )
                .outsourced(
                        test.isOutsourced()
                )
                .laboratoryInstructions(
                        test.getLaboratoryInstructions()
                )

                // ------------------------------------------------
                // Reporting
                // ------------------------------------------------

                .reportSection(
                        test.getReportSection()
                )
                .displayOrder(
                        test.getDisplayOrder()
                )
                .reportDescription(
                        test.getReportDescription()
                )
                .interpretationGuidance(
                        test.getInterpretationGuidance()
                )

                // ------------------------------------------------
                // Commercial
                // ------------------------------------------------

                .basePrice(
                        test.getBasePrice()
                )
                .currency(
                        test.getCurrency()
                )
                .billingCode(
                        test.getBillingCode()
                )

                // ------------------------------------------------
                // Lifecycle
                // ------------------------------------------------

                .status(test.getStatus())
                .version(test.getVersion())
                .effectiveFrom(
                        test.getEffectiveFrom()
                )
                .effectiveUntil(
                        test.getEffectiveUntil()
                )
                .createdAt(
                        test.getCreatedAt()
                )
                .updatedAt(
                        test.getUpdatedAt()
                )

                .build();
    }
}