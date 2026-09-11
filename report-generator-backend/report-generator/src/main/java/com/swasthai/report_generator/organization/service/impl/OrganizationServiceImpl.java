package com.swasthai.report_generator.organization.service.impl;

import com.swasthai.report_generator.common.exception.ResourceAlreadyExistsException;
import com.swasthai.report_generator.common.exception.ResourceNotFoundException;
import com.swasthai.report_generator.organization.dto.request.CreateOrganizationRequest;
import com.swasthai.report_generator.organization.dto.request.UpdateOrganizationRequest;
import com.swasthai.report_generator.organization.dto.request.UpdateOrganizationStatusRequest;
import com.swasthai.report_generator.organization.dto.response.OrganizationPageResponse;
import com.swasthai.report_generator.organization.dto.response.OrganizationResponse;
import com.swasthai.report_generator.organization.entity.Organization;
import com.swasthai.report_generator.organization.entity.OrganizationStatus;
import com.swasthai.report_generator.organization.repository.OrganizationRepository;
import com.swasthai.report_generator.organization.repository.OrganizationSequenceRepository;
import com.swasthai.report_generator.organization.service.OrganizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class OrganizationServiceImpl implements OrganizationService {

    private static final int MAX_PAGE_SIZE = 100;

    private final OrganizationRepository organizationRepository;
    private final OrganizationSequenceRepository sequenceRepository;

    // ============================================================
    // CREATE
    // ============================================================

    @Override
    public OrganizationResponse createOrganization(
            CreateOrganizationRequest request
    ) {

        String name = normalizeName(request.getName());
        String code = normalizeCode(request.getCode());

        validateUniqueName(name);
        validateUniqueCode(code);

        Organization organization = Organization.builder()
                .name(name)
                .code(code)
                .status(OrganizationStatus.ACTIVE)
                .build();

        Organization savedOrganization =
                organizationRepository.save(organization);

        /*
         * Every organization receives its own patient sequence.
         *
         * This is intentionally kept as part of organization creation.
         */
        OrganizationSequenceRepository sequenceRepositoryRef =
                sequenceRepository;

        sequenceRepositoryRef.save(
                com.swasthai.report_generator.organization.entity.OrganizationSequence
                        .builder()
                        .organization(savedOrganization)
                        .patientSequence(0L)
                        .build()
        );

        return mapToResponse(savedOrganization);
    }

    // ============================================================
    // LIST
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public OrganizationPageResponse getOrganizations(
            int page,
            int size,
            String sortBy,
            String sortDirection
    ) {

        if (page < 0) {
            throw new IllegalArgumentException(
                    "Page number cannot be negative."
            );
        }

        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException(
                    "Page size must be between 1 and " + MAX_PAGE_SIZE + "."
            );
        }

        String safeSortBy = resolveSortField(sortBy);
        Sort.Direction direction =
                resolveSortDirection(sortDirection);

        Pageable pageable =
                PageRequest.of(
                        page,
                        size,
                        Sort.by(direction, safeSortBy)
                );

        Page<Organization> organizationPage =
                organizationRepository.findAll(pageable);

        List<OrganizationResponse> content =
                organizationPage
                        .getContent()
                        .stream()
                        .map(this::mapToResponse)
                        .toList();

        return OrganizationPageResponse.builder()
                .content(content)
                .page(organizationPage.getNumber())
                .size(organizationPage.getSize())
                .totalElements(organizationPage.getTotalElements())
                .totalPages(organizationPage.getTotalPages())
                .first(organizationPage.isFirst())
                .last(organizationPage.isLast())
                .build();
    }

    // ============================================================
    // GET BY REF ID
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public OrganizationResponse getOrganizationByRefId(
            String refId
    ) {

        String normalizedRefId = normalizeRefId(refId);

        Organization organization =
                organizationRepository
                        .findByRefId(normalizedRefId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Organization not found."
                                )
                        );

        return mapToResponse(organization);
    }

    // ============================================================
    // UPDATE
    // ============================================================

    @Override
    public OrganizationResponse updateOrganization(
            String refId,
            UpdateOrganizationRequest request
    ) {

        String normalizedRefId = normalizeRefId(refId);

        Organization organization =
                organizationRepository
                        .findByRefId(normalizedRefId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Organization not found."
                                )
                        );

        /*
         * DISABLED organizations are terminal.
         *
         * We don't allow modifying their business identity.
         * This prevents a disabled organization from effectively
         * being reused under a different identity.
         */
        if (organization.getStatus() == OrganizationStatus.DISABLED) {
            throw new IllegalStateException(
                    "A disabled organization cannot be modified."
            );
        }

        String name = normalizeName(request.getName());
        String code = normalizeCode(request.getCode());

        /*
         * Don't treat the organization itself as a duplicate.
         */
        if (organizationRepository.existsByNameIgnoreCaseAndIdNot(
                name,
                organization.getId()
        )) {
            throw new ResourceAlreadyExistsException(
                    "Organization name already exists."
            );
        }

        if (organizationRepository.existsByCodeAndIdNot(
                code,
                organization.getId()
        )) {
            throw new ResourceAlreadyExistsException(
                    "Organization code already exists."
            );
        }

        organization.setName(name);
        organization.setCode(code);

        Organization savedOrganization =
                organizationRepository.save(organization);

        return mapToResponse(savedOrganization);
    }

    // ============================================================
    // STATUS UPDATE
    // ============================================================

    @Override
    public OrganizationResponse updateOrganizationStatus(
            String refId,
            UpdateOrganizationStatusRequest request
    ) {

        String normalizedRefId = normalizeRefId(refId);

        Organization organization =
                organizationRepository
                        .findByRefId(normalizedRefId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Organization not found."
                                )
                        );

        OrganizationStatus currentStatus =
                organization.getStatus();

        OrganizationStatus requestedStatus =
                request.getStatus();

        validateStatusTransition(
                currentStatus,
                requestedStatus
        );

        organization.setStatus(requestedStatus);

        Organization savedOrganization =
                organizationRepository.save(organization);

        return mapToResponse(savedOrganization);
    }

    // ============================================================
    // VALIDATION
    // ============================================================

    private void validateUniqueName(String name) {

        if (organizationRepository.existsByNameIgnoreCase(name)) {
            throw new ResourceAlreadyExistsException(
                    "Organization name already exists."
            );
        }
    }

    private void validateUniqueCode(String code) {

        if (organizationRepository.existsByCode(code)) {
            throw new ResourceAlreadyExistsException(
                    "Organization code already exists."
            );
        }
    }

    private void validateStatusTransition(
            OrganizationStatus currentStatus,
            OrganizationStatus requestedStatus
    ) {

        if (currentStatus == requestedStatus) {
            throw new IllegalStateException(
                    "Organization is already in the requested status."
            );
        }

        /*
         * DISABLED is terminal.
         */
        if (currentStatus == OrganizationStatus.DISABLED) {
            throw new IllegalStateException(
                    "A disabled organization cannot change status."
            );
        }

        /*
         * Valid transitions:
         *
         * ACTIVE    -> SUSPENDED
         * ACTIVE    -> DISABLED
         *
         * SUSPENDED -> ACTIVE
         * SUSPENDED -> DISABLED
         */
        boolean validTransition =
                (currentStatus == OrganizationStatus.ACTIVE
                        && (requestedStatus == OrganizationStatus.SUSPENDED
                        || requestedStatus == OrganizationStatus.DISABLED))
                ||
                (currentStatus == OrganizationStatus.SUSPENDED
                        && (requestedStatus == OrganizationStatus.ACTIVE
                        || requestedStatus == OrganizationStatus.DISABLED));

        if (!validTransition) {
            throw new IllegalStateException(
                    "Invalid organization status transition from "
                            + currentStatus
                            + " to "
                            + requestedStatus
                            + "."
            );
        }
    }

    // ============================================================
    // NORMALIZATION
    // ============================================================

    private String normalizeName(String value) {

        if (value == null) {
            throw new IllegalArgumentException(
                    "Organization name is required."
            );
        }

        String normalized = value.trim();

        if (normalized.isBlank()) {
            throw new IllegalArgumentException(
                    "Organization name is required."
            );
        }

        return normalized;
    }

    private String normalizeCode(String value) {

        if (value == null) {
            throw new IllegalArgumentException(
                    "Organization code is required."
            );
        }

        String normalized =
                value.trim().toUpperCase(Locale.ROOT);

        if (normalized.isBlank()) {
            throw new IllegalArgumentException(
                    "Organization code is required."
            );
        }

        return normalized;
    }

    private String normalizeRefId(String value) {

        if (value == null) {
            throw new IllegalArgumentException(
                    "Organization reference ID is required."
            );
        }

        String normalized = value.trim();

        if (normalized.isBlank()) {
            throw new IllegalArgumentException(
                    "Organization reference ID is required."
            );
        }

        return normalized;
    }

    // ============================================================
    // SAFE SORTING
    // ============================================================

    private String resolveSortField(String sortBy) {

        if (sortBy == null || sortBy.isBlank()) {
            return "createdAt";
        }

        return switch (sortBy.trim()) {
            case "name" -> "name";
            case "code" -> "code";
            case "status" -> "status";
            case "createdAt" -> "createdAt";
            case "updatedAt" -> "updatedAt";
            default -> throw new IllegalArgumentException(
                    "Invalid organization sort field."
            );
        };
    }

    private Sort.Direction resolveSortDirection(
            String sortDirection
    ) {

        if (sortDirection == null
                || sortDirection.isBlank()) {
            return Sort.Direction.DESC;
        }

        try {
            return Sort.Direction.fromString(
                    sortDirection.trim()
            );
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Invalid sort direction. Use ASC or DESC."
            );
        }
    }

    // ============================================================
    // RESPONSE MAPPING
    // ============================================================

    private OrganizationResponse mapToResponse(
            Organization organization
    ) {

        return OrganizationResponse.builder()
                .refId(organization.getRefId())
                .name(organization.getName())
                .code(organization.getCode())
                .status(organization.getStatus())
                .createdAt(organization.getCreatedAt())
                .updatedAt(organization.getUpdatedAt())
                .build();
    }
}