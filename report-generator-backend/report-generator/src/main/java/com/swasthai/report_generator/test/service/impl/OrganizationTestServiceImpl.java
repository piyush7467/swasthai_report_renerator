package com.swasthai.report_generator.test.service.impl;

import com.swasthai.report_generator.common.exception.ResourceAlreadyExistsException;
import com.swasthai.report_generator.common.exception.ResourceNotFoundException;
import com.swasthai.report_generator.organization.entity.Organization;
import com.swasthai.report_generator.organization.entity.OrganizationStatus;
import com.swasthai.report_generator.organization.repository.OrganizationRepository;
import com.swasthai.report_generator.test.dto.request.AssignTestRequest;
import com.swasthai.report_generator.test.dto.request.UpdateOrganizationTestRequest;
import com.swasthai.report_generator.test.dto.response.OrganizationTestResponse;
import com.swasthai.report_generator.test.entity.OrganizationTest;
import com.swasthai.report_generator.test.entity.OrganizationTestStatus;
import com.swasthai.report_generator.test.entity.Test;
import com.swasthai.report_generator.test.entity.TestStatus;
import com.swasthai.report_generator.test.repository.OrganizationTestRepository;
import com.swasthai.report_generator.test.repository.TestRepository;
import com.swasthai.report_generator.test.service.OrganizationTestService;
import com.swasthai.report_generator.user.entity.Role;
import com.swasthai.report_generator.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional
public class OrganizationTestServiceImpl
        implements OrganizationTestService {

    private final OrganizationTestRepository organizationTestRepository;
    private final OrganizationRepository organizationRepository;
    private final TestRepository testRepository;

    // ============================================================
    // ASSIGN TEST
    // SUPER_ADMIN ONLY
    // ============================================================

    @Override
    public OrganizationTestResponse assignTest(
            AssignTestRequest request
    ) {

        requireSuperAdmin();

        validateEffectiveDates(
                request.effectiveFrom(),
                request.effectiveUntil()
        );

        Organization organization =
                getActiveOrganization(
                        request.organizationRefId()
                );

        Test test =
                getActiveTest(
                        request.testRefId()
                );

        // --------------------------------------------------------
        // Prevent duplicate assignment
        // --------------------------------------------------------

        if (organizationTestRepository
                .existsByOrganization_IdAndTest_Id(
                        organization.getId(),
                        test.getId()
                )) {

            throw new ResourceAlreadyExistsException(
                    "Test is already assigned to this organization"
            );
        }

        OrganizationTest assignment =
                OrganizationTest.builder()
                        .organization(organization)
                        .test(test)
                        .status(
                                OrganizationTestStatus.ACTIVE
                        )
                        .effectiveFrom(
                                request.effectiveFrom()
                        )
                        .effectiveUntil(
                                request.effectiveUntil()
                        )
                        .build();

        OrganizationTest saved =
                organizationTestRepository.save(
                        assignment
                );

        return mapToResponse(saved);
    }

    // ============================================================
    // GET ASSIGNMENT BY REF ID
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public OrganizationTestResponse getAssignment(
            String organizationTestRefId
    ) {

        OrganizationTest assignment =
                findAssignment(organizationTestRefId);

        User currentUser =
                getCurrentUser();

        if (currentUser.getRole() != Role.SUPER_ADMIN) {

            ensureSameOrganization(
                    currentUser,
                    assignment.getOrganization()
            );
        }

        return mapToResponse(assignment);
    }

    // ============================================================
    // GET ALL ASSIGNMENTS
    // SUPER_ADMIN
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public Page<OrganizationTestResponse> getAllAssignments(
            String organizationRefId,
            String status,
            Pageable pageable
    ) {

        requireSuperAdmin();

        Organization organization =
                getOrganization(
                        organizationRefId
                );

        OrganizationTestStatus assignmentStatus =
                parseStatus(status);

        Page<OrganizationTest> assignments;

        if (assignmentStatus == null) {

            assignments =
                    organizationTestRepository
                            .findAllByOrganization_RefId(
                                    organization.getRefId(),
                                    pageable
                            );

        } else {

            assignments =
                    organizationTestRepository
                            .findAllByOrganization_RefIdAndStatus(
                                    organization.getRefId(),
                                    assignmentStatus,
                                    pageable
                            );
        }

        return assignments.map(
                this::mapToResponse
        );
    }

    // ============================================================
    // GET MY ORGANIZATION TESTS
    // ORG_ADMIN / LAB_STAFF
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public Page<OrganizationTestResponse>
    getMyOrganizationTests(
            String status,
            Pageable pageable
    ) {

        User currentUser =
                getCurrentUser();

        requireOrganizationUser(currentUser);

        Organization organization =
                getCurrentUserOrganization(
                        currentUser
                );

        OrganizationTestStatus assignmentStatus =
                parseStatus(status);

        Page<OrganizationTest> assignments;

        if (assignmentStatus == null) {

            assignments =
                    organizationTestRepository
                            .findAllByOrganization_RefId(
                                    organization.getRefId(),
                                    pageable
                            );

        } else {

            assignments =
                    organizationTestRepository
                            .findAllByOrganization_RefIdAndStatus(
                                    organization.getRefId(),
                                    assignmentStatus,
                                    pageable
                            );
        }

        return assignments.map(
                this::mapToResponse
        );
    }

    // ============================================================
    // UPDATE ASSIGNMENT
    // SUPER_ADMIN ONLY
    // ============================================================

    @Override
    public OrganizationTestResponse updateAssignment(
            String organizationTestRefId,
            UpdateOrganizationTestRequest request
    ) {

        requireSuperAdmin();

        OrganizationTest assignment =
                findAssignment(
                        organizationTestRefId
                );

        LocalDate finalEffectiveFrom =
                request.effectiveFrom() != null
                        ? request.effectiveFrom()
                        : assignment.getEffectiveFrom();

        LocalDate finalEffectiveUntil =
                request.effectiveUntil() != null
                        ? request.effectiveUntil()
                        : assignment.getEffectiveUntil();

        validateEffectiveDates(
                finalEffectiveFrom,
                finalEffectiveUntil
        );

        if (request.status() != null) {

            assignment.setStatus(
                    request.status()
            );
        }

        if (request.effectiveFrom() != null) {

            assignment.setEffectiveFrom(
                    request.effectiveFrom()
            );
        }

        if (request.effectiveUntil() != null) {

            assignment.setEffectiveUntil(
                    request.effectiveUntil()
            );
        }

        OrganizationTest updated =
                organizationTestRepository.save(
                        assignment
                );

        return mapToResponse(updated);
    }

    // ============================================================
    // DEACTIVATE ASSIGNMENT
    // SUPER_ADMIN ONLY
    // ============================================================

    @Override
    public void deactivateAssignment(
            String organizationTestRefId
    ) {

        requireSuperAdmin();

        OrganizationTest assignment =
                findAssignment(
                        organizationTestRefId
                );

        if (assignment.getStatus()
                == OrganizationTestStatus.INACTIVE) {

            throw new IllegalStateException(
                    "Test assignment is already inactive"
            );
        }

        assignment.setStatus(
                OrganizationTestStatus.INACTIVE
        );

        organizationTestRepository.save(
                assignment
        );
    }

    // ============================================================
    // CHECK TEST ACCESS
    // CURRENT ORGANIZATION
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public boolean hasTestAccess(
            String testRefId
    ) {

        User currentUser =
                getCurrentUser();

        requireOrganizationUser(
                currentUser
        );

        Organization organization =
                getCurrentUserOrganization(
                        currentUser
                );

        Test test =
                testRepository.findByRefId(
                        testRefId
                ).orElse(null);

        if (test == null
                || test.getStatus()
                != TestStatus.ACTIVE) {

            return false;
        }

        OrganizationTest assignment =
                organizationTestRepository
                        .findByOrganization_IdAndTest_Id(
                                organization.getId(),
                                test.getId()
                        )
                        .orElse(null);

        if (assignment == null
                || assignment.getStatus()
                != OrganizationTestStatus.ACTIVE) {

            return false;
        }

        return isCurrentlyEffective(
                assignment.getEffectiveFrom(),
                assignment.getEffectiveUntil()
        );
    }

    // ============================================================
    // SECURITY
    // ============================================================

    private User getCurrentUser() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()) {

            throw new AccessDeniedException(
                    "Authentication is required"
            );
        }

        Object details =
                authentication.getDetails();

        if (!(details instanceof User user)) {

            throw new AccessDeniedException(
                    "Authenticated user information is unavailable"
            );
        }

        return user;
    }

    private void requireSuperAdmin() {

        User user = getCurrentUser();

        if (user.getRole() != Role.SUPER_ADMIN) {

            throw new AccessDeniedException(
                    "Only SUPER_ADMIN can manage test assignments"
            );
        }
    }

    private void requireOrganizationUser(
            User user
    ) {

        if (user.getRole() != Role.ORG_ADMIN
                && user.getRole() != Role.LAB_STAFF) {

            throw new AccessDeniedException(
                    "Only organization users can access organization tests"
            );
        }
    }

    private void ensureSameOrganization(
            User currentUser,
            Organization targetOrganization
    ) {

        Organization userOrganization =
                getCurrentUserOrganization(
                        currentUser
                );

        if (!userOrganization.getId()
                .equals(targetOrganization.getId())) {

            throw new AccessDeniedException(
                    "You are not allowed to access this organization"
            );
        }
    }

    private Organization getCurrentUserOrganization(
            User user
    ) {

        if (user.getOrganization() == null) {

            throw new AccessDeniedException(
                    "User is not associated with an organization"
            );
        }

        if (user.getOrganization().getStatus()
                != OrganizationStatus.ACTIVE) {

            throw new AccessDeniedException(
                    "Organization is not active"
            );
        }

        return user.getOrganization();
    }

    // ============================================================
    // ORGANIZATION / TEST LOOKUPS
    // ============================================================

    private Organization getOrganization(
            String organizationRefId
    ) {

        if (organizationRefId == null
                || organizationRefId.isBlank()) {

            throw new IllegalArgumentException(
                    "Organization refId is required"
            );
        }

        return organizationRepository
                .findByRefId(
                        organizationRefId.trim()
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Organization not found"
                        )
                );
    }

    private Organization getActiveOrganization(
            String organizationRefId
    ) {

        Organization organization =
                getOrganization(
                        organizationRefId
                );

        if (organization.getStatus()
                != OrganizationStatus.ACTIVE) {

            throw new IllegalStateException(
                    "Cannot assign tests to an inactive organization"
            );
        }

        return organization;
    }

    private Test getActiveTest(
            String testRefId
    ) {

        if (testRefId == null
                || testRefId.isBlank()) {

            throw new IllegalArgumentException(
                    "Test refId is required"
            );
        }

        Test test =
                testRepository.findByRefId(
                        testRefId.trim()
                ).orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Test not found"
                        )
                );

        if (test.getStatus()
                != TestStatus.ACTIVE) {

            throw new IllegalStateException(
                    "Cannot assign an inactive test"
            );
        }

        return test;
    }

    private OrganizationTest findAssignment(
            String organizationTestRefId
    ) {

        if (organizationTestRefId == null
                || organizationTestRefId.isBlank()) {

            throw new IllegalArgumentException(
                    "Organization test refId is required"
            );
        }

        return organizationTestRepository
                .findByRefId(
                        organizationTestRefId.trim()
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Test assignment not found"
                        )
                );
    }

    // ============================================================
    // VALIDATION
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

    private boolean isCurrentlyEffective(
            LocalDate effectiveFrom,
            LocalDate effectiveUntil
    ) {

        LocalDate today =
                LocalDate.now();

        if (effectiveFrom != null
                && today.isBefore(effectiveFrom)) {

            return false;
        }

        if (effectiveUntil != null
                && today.isAfter(effectiveUntil)) {

            return false;
        }

        return true;
    }

    private OrganizationTestStatus parseStatus(
            String status
    ) {

        if (status == null
                || status.isBlank()) {

            return null;
        }

        try {

            return OrganizationTestStatus.valueOf(
                    status.trim().toUpperCase()
            );

        } catch (IllegalArgumentException exception) {

            throw new IllegalArgumentException(
                    "Invalid organization test status"
            );
        }
    }

    // ============================================================
    // RESPONSE MAPPING
    // ============================================================

    private OrganizationTestResponse mapToResponse(
            OrganizationTest assignment
    ) {

        return OrganizationTestResponse.builder()

                .refId(
                        assignment.getRefId()
                )

                .organizationRefId(
                        assignment
                                .getOrganization()
                                .getRefId()
                )

                .organizationName(
                        assignment
                                .getOrganization()
                                .getName()
                )

                .testRefId(
                        assignment
                                .getTest()
                                .getRefId()
                )

                .testCode(
                        assignment
                                .getTest()
                                .getCode()
                )

                .testName(
                        assignment
                                .getTest()
                                .getName()
                )

                .testType(
                        assignment
                                .getTest()
                                .getTestType()
                                .name()
                )

                .status(
                        assignment.getStatus()
                )

                .effectiveFrom(
                        assignment
                                .getEffectiveFrom()
                )

                .effectiveUntil(
                        assignment
                                .getEffectiveUntil()
                )

                .createdAt(
                        assignment.getCreatedAt()
                )

                .updatedAt(
                        assignment.getUpdatedAt()
                )

                .build();
    }
}