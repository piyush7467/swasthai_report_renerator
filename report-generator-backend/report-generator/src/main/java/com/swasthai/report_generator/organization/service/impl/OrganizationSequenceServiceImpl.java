package com.swasthai.report_generator.organization.service.impl;

import com.swasthai.report_generator.organization.entity.Organization;
import com.swasthai.report_generator.organization.entity.OrganizationSequence;
import com.swasthai.report_generator.organization.repository.OrganizationSequenceRepository;
import com.swasthai.report_generator.organization.service.OrganizationSequenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrganizationSequenceServiceImpl
        implements OrganizationSequenceService {

    private final OrganizationSequenceRepository sequenceRepository;

    @Override
    @Transactional
    public String generatePatientCode(
            Organization organization
    ) {

        OrganizationSequence sequence =
                sequenceRepository
                        .findByOrganizationIdForUpdate(
                                organization.getId()
                        )
                        .orElseGet(() -> {
                            try {
                                return createSequence(organization);
                            } catch (Exception e) {
                                return sequenceRepository
                                        .findByOrganizationIdForUpdate(organization.getId())
                                        .orElseThrow(() -> new IllegalStateException("Failed to lock organization sequence."));
                            }
                        });

        long nextNumber =
                sequence.getPatientSequence() + 1;

        sequence.setPatientSequence(nextNumber);

        sequenceRepository.save(sequence);

        return String.format(
                "PAT-%06d",
                nextNumber
        );
    }

    private OrganizationSequence createSequence(
            Organization organization
    ) {

        OrganizationSequence sequence =
                OrganizationSequence.builder()
                        .organization(organization)
                        .patientSequence(0L)
                        .build();

        return sequenceRepository.save(sequence);
    }
}