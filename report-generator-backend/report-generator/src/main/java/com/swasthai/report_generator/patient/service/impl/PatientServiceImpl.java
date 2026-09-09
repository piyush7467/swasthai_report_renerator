package com.swasthai.report_generator.patient.service.impl;

import com.swasthai.report_generator.auth.service.CurrentOrganizationService;
import com.swasthai.report_generator.common.exception.ResourceAlreadyExistsException;
import com.swasthai.report_generator.common.exception.ResourceNotFoundException;
import com.swasthai.report_generator.organization.entity.Organization;
import com.swasthai.report_generator.organization.service.OrganizationSequenceService;
import com.swasthai.report_generator.patient.dto.request.CreatePatientRequest;
import com.swasthai.report_generator.patient.dto.response.PatientResponse;
import com.swasthai.report_generator.patient.entity.Patient;
import com.swasthai.report_generator.patient.repository.PatientRepository;
import com.swasthai.report_generator.patient.service.PatientService;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PatientServiceImpl implements PatientService {

    private final PatientRepository patientRepository;
    private final CurrentOrganizationService currentOrganizationService;
    private final OrganizationSequenceService organizationSequenceService;

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final java.util.Set<String> ALLOWED_SORT_FIELDS =
            java.util.Set.of("createdAt", "name", "patientCode", "dateOfBirth");

    @Override
    public PatientResponse createPatient(
            CreatePatientRequest request) {

        Organization organization = currentOrganizationService
                .getCurrentOrganization();

        String name = request.getName().trim();

        String phone = request.getPhone() != null
                ? request.getPhone().trim()
                : null;

        String email = request.getEmail() != null
                ? request.getEmail().trim().toLowerCase()
                : null;

        String address = request.getAddress() != null
                ? request.getAddress().trim()
                : null;

        String patientCode = organizationSequenceService
                .generatePatientCode(organization);

        if (patientRepository
                .existsByPatientCodeAndOrganization_Id(
                        patientCode,
                        organization.getId())) {

            throw new ResourceAlreadyExistsException(
                    "Patient code already exists.");
        }

        Patient patient = Patient.builder()
                .organization(organization)
                .patientCode(patientCode)
                .name(name)
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender())
                .phone(phone)
                .email(email)
                .address(address)
                .build();

        Patient savedPatient = patientRepository.save(patient);

        return mapToResponse(savedPatient);
    }

    private PatientResponse mapToResponse(
            Patient patient) {

        return PatientResponse.builder()
                .refId(patient.getRefId())
                .patientCode(patient.getPatientCode())
                .name(patient.getName())
                .dateOfBirth(patient.getDateOfBirth())
                .gender(patient.getGender())
                .phone(patient.getPhone())
                .email(patient.getEmail())
                .address(patient.getAddress())
                .organizationRefId(
                        patient.getOrganization().getRefId())
                .createdAt(patient.getCreatedAt())
                .updatedAt(patient.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PatientResponse getPatient(
            String patientRefId) {

        Organization organization = currentOrganizationService
                .getCurrentOrganization();

        Patient patient = patientRepository
                .findByRefIdAndOrganization_Id(
                        patientRefId,
                        organization.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Patient not found."));

        return mapToResponse(patient);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PatientResponse> getPatients() {

        Organization organization = currentOrganizationService
                .getCurrentOrganization();

        return patientRepository
                .findAllByOrganization_IdOrderByCreatedAtDesc(
                        organization.getId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public com.swasthai.report_generator.common.response.PagedResponse<PatientResponse> getPatients(
            int page,
            int size,
            String sortBy,
            String sortDirection
    ) {

        Organization organization = currentOrganizationService
                .getCurrentOrganization();

        int sanitizedPage = Math.max(0, page);
        int sanitizedSize = (size <= 0) ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);

        String sanitizedSortBy = (sortBy != null && ALLOWED_SORT_FIELDS.contains(sortBy))
                ? sortBy
                : "createdAt";

        org.springframework.data.domain.Sort.Direction direction =
                "asc".equalsIgnoreCase(sortDirection)
                        ? org.springframework.data.domain.Sort.Direction.ASC
                        : org.springframework.data.domain.Sort.Direction.DESC;

        org.springframework.data.domain.Pageable pageable =
                org.springframework.data.domain.PageRequest.of(
                        sanitizedPage,
                        sanitizedSize,
                        org.springframework.data.domain.Sort.by(direction, sanitizedSortBy)
                );

        org.springframework.data.domain.Page<Patient> patientPage =
                patientRepository.findAllByOrganization_Id(organization.getId(), pageable);

        List<PatientResponse> content = patientPage.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        return com.swasthai.report_generator.common.response.PagedResponse.<PatientResponse>builder()
                .content(content)
                .page(patientPage.getNumber())
                .size(patientPage.getSize())
                .totalElements(patientPage.getTotalElements())
                .totalPages(patientPage.getTotalPages())
                .last(patientPage.isLast())
                .build();
    }

}