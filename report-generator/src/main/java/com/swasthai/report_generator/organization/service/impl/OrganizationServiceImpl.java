package com.swasthai.report_generator.organization.service.impl;

import com.swasthai.report_generator.common.exception.ResourceAlreadyExistsException;
import com.swasthai.report_generator.organization.dto.request.CreateOrganizationRequest;
import com.swasthai.report_generator.organization.dto.response.OrganizationResponse;
import com.swasthai.report_generator.organization.entity.Organization;
import com.swasthai.report_generator.organization.repository.OrganizationRepository;
import com.swasthai.report_generator.organization.service.OrganizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class OrganizationServiceImpl implements OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final com.swasthai.report_generator.organization.repository.OrganizationSequenceRepository sequenceRepository;

    @Override
    public OrganizationResponse createOrganization(
            CreateOrganizationRequest request
    ) {

        String name = request.getName().trim();
        String code = request.getCode().trim().toUpperCase();

        // Check duplicate organization code
        if (organizationRepository.existsByCode(code)) {
            throw new ResourceAlreadyExistsException(
                    "Organization code already exists."
            );
        }

        // Check duplicate organization name
        if (organizationRepository.existsByNameIgnoreCase(name)) {
            throw new ResourceAlreadyExistsException(
                    "Organization name already exists."
            );
        }

        Organization organization = Organization.builder()
                .name(name)
                .code(code)
                .build();

        Organization savedOrganization =
                organizationRepository.save(organization);

        // Deterministically initialize sequence to prevent concurrent creation race conditions
        com.swasthai.report_generator.organization.entity.OrganizationSequence sequence =
                com.swasthai.report_generator.organization.entity.OrganizationSequence.builder()
                        .organization(savedOrganization)
                        .patientSequence(0L)
                        .build();
        sequenceRepository.save(sequence);

        return mapToResponse(savedOrganization);
    }

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