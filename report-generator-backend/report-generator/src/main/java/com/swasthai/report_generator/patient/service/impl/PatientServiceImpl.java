package com.swasthai.report_generator.patient.service.impl;

import com.swasthai.report_generator.auth.service.CurrentOrganizationService;
import com.swasthai.report_generator.auth.service.CurrentUserService;
import com.swasthai.report_generator.common.exception.ForbiddenException;
import com.swasthai.report_generator.common.exception.ResourceAlreadyExistsException;
import com.swasthai.report_generator.common.exception.ResourceNotFoundException;
import com.swasthai.report_generator.common.response.PagedResponse;
import com.swasthai.report_generator.organization.entity.Organization;
import com.swasthai.report_generator.organization.entity.OrganizationStatus;
import com.swasthai.report_generator.organization.service.OrganizationSequenceService;
import com.swasthai.report_generator.patient.dto.request.CreatePatientRequest;
import com.swasthai.report_generator.patient.dto.request.UpdatePatientRequest;
import com.swasthai.report_generator.patient.dto.response.PatientResponse;
import com.swasthai.report_generator.patient.entity.AgeUnit;
import com.swasthai.report_generator.patient.entity.Patient;
import com.swasthai.report_generator.patient.repository.PatientRepository;
import com.swasthai.report_generator.patient.service.PatientService;
import com.swasthai.report_generator.user.entity.Role;
import com.swasthai.report_generator.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.swasthai.report_generator.report.dto.response.PatientReportStats;
import com.swasthai.report_generator.report.repository.ReportRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PatientServiceImpl implements PatientService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "createdAt",
            "name",
            "patientCode",
            "dateOfBirth",
            "updatedAt"
    );

    private final PatientRepository patientRepository;
    private final ReportRepository reportRepository;
    private final CurrentOrganizationService currentOrganizationService;
    private final CurrentUserService currentUserService;
    private final OrganizationSequenceService organizationSequenceService;

    // ============================================================
    // CREATE
    // ============================================================

    @Override
    public PatientResponse createPatient(CreatePatientRequest request) {

        Organization organization =
                currentOrganizationService.getCurrentOrganization();

        validateOrganizationAvailable(organization);

        validateAgeData(
                request.getDateOfBirthKnown(),
                request.getDateOfBirth(),
                request.getAgeValue(),
                request.getAgeUnit()
        );

        String name = normalizeName(request.getName());
        String phone = normalizePhone(request.getPhone());
        String email = normalizeEmail(request.getEmail());
        String address = normalizeOptional(request.getAddress());

        /*
         * Phone numbers are intentionally NOT unique.
         *
         * Family members may share the same phone number.
         */

        String patientCode =
                organizationSequenceService.generatePatientCode(organization);

        if (patientRepository.existsByPatientCodeAndOrganization_Id(
                patientCode,
                organization.getId()
        )) {
            throw new ResourceAlreadyExistsException(
                    "Patient code already exists."
            );
        }

        Patient patient = Patient.builder()
                .organization(organization)
                .patientCode(patientCode)
                .salutation(request.getSalutation())
                .name(name)
                .dateOfBirthKnown(request.getDateOfBirthKnown())
                .dateOfBirth(request.getDateOfBirth())
                .ageValue(request.getAgeValue())
                .ageUnit(request.getAgeUnit())
                .gender(request.getGender())
                .phone(phone)
                .email(email)
                .address(address)
                .weightKg(normalizeWeight(request.getWeightKg()))
                .build();

        Patient savedPatient = patientRepository.save(patient);

        return mapToResponse(savedPatient);
    }

    // ============================================================
    // GET ONE
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public PatientResponse getPatient(String patientRefId) {

        Organization organization =
                currentOrganizationService.getCurrentOrganization();

        validateOrganizationAvailable(organization);

        Patient patient =
                patientRepository
                        .findByRefIdAndOrganization_IdAndDeletedAtIsNull(
                                normalizeRefId(patientRefId),
                                organization.getId()
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Patient not found."
                                )
                        );

        Optional<PatientReportStats> stats = reportRepository.getReportStatsForPatient(
                organization.getId(),
                patient.getRefId()
        );
        long totalReports = stats.map(PatientReportStats::totalReports).orElse(0L);
        Instant lastReportDate = stats.map(PatientReportStats::lastReportDate).orElse(null);

        return mapToResponse(patient, totalReports, lastReportDate);
    }

    // ============================================================
    // LIST / SEARCH
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<PatientResponse> getPatients(
            int page,
            int size,
            String sortBy,
            String sortDirection,
            String search
    ) {

        Organization organization =
                currentOrganizationService.getCurrentOrganization();

        validateOrganizationAvailable(organization);
        validatePagination(page, size);

        String normalizedSearch = normalizeOptional(search);

        String safeSortBy = resolveSortField(sortBy);
        Sort.Direction direction = resolveSortDirection(sortDirection);

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(direction, safeSortBy)
        );

        Page<Patient> patientPage;

        if (normalizedSearch == null) {

            patientPage =
                    patientRepository
                            .findAllByOrganization_IdAndDeletedAtIsNull(
                                    organization.getId(),
                                    pageable
                            );

        } else {

            patientPage =
                    patientRepository.searchActivePatients(
                            organization.getId(),
                            normalizedSearch,
                            pageable
                    );
        }

        List<Patient> patients = patientPage.getContent();
        List<String> refIds = patients.stream().map(Patient::getRefId).toList();
        Map<String, PatientReportStats> statsMap = Collections.emptyMap();
        if (!refIds.isEmpty()) {
            List<PatientReportStats> statsList = reportRepository.getReportStatsForPatients(
                    organization.getId(),
                    refIds
            );
            statsMap = statsList.stream().collect(Collectors.toMap(
                    PatientReportStats::patientRefId,
                    s -> s
            ));
        }

        Map<String, PatientReportStats> finalStatsMap = statsMap;
        return PagedResponse.<PatientResponse>builder()
                .content(
                        patients.stream()
                                .map(p -> {
                                    PatientReportStats s = finalStatsMap.get(p.getRefId());
                                    long count = s != null ? s.totalReports() : 0L;
                                    Instant lastDate = s != null ? s.lastReportDate() : null;
                                    return mapToResponse(p, count, lastDate);
                                })
                                .toList()
                )
                .page(patientPage.getNumber())
                .size(patientPage.getSize())
                .totalElements(patientPage.getTotalElements())
                .totalPages(patientPage.getTotalPages())
                .last(patientPage.isLast())
                .build();
    }

    // ============================================================
    // UPDATE
    // ============================================================

    @Override
    public PatientResponse updatePatient(
            String patientRefId,
            UpdatePatientRequest request
    ) {

        User currentUser = currentUserService.getCurrentUser();

        requireRole(
                currentUser,
                Role.ORG_ADMIN,
                "Only ORG_ADMIN can update patients."
        );

        Organization organization =
                currentOrganizationService.getCurrentOrganization();

        validateOrganizationAvailable(organization);

        Patient patient =
                patientRepository
                        .findByRefIdAndOrganization_IdAndDeletedAtIsNull(
                                normalizeRefId(patientRefId),
                                organization.getId()
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Patient not found."
                                )
                        );

        validateAgeData(
                request.getDateOfBirthKnown(),
                request.getDateOfBirth(),
                request.getAgeValue(),
                request.getAgeUnit()
        );

        patient.setSalutation(request.getSalutation());

        patient.setName(
                normalizeName(request.getName())
        );

        patient.setDateOfBirthKnown(
                request.getDateOfBirthKnown()
        );

        patient.setDateOfBirth(
                request.getDateOfBirth()
        );

        patient.setAgeValue(
                request.getAgeValue()
        );

        patient.setAgeUnit(
                request.getAgeUnit()
        );

        patient.setGender(
                request.getGender()
        );

        patient.setPhone(
                normalizePhone(request.getPhone())
        );

        patient.setEmail(
                normalizeEmail(request.getEmail())
        );

        patient.setAddress(
                normalizeOptional(request.getAddress())
        );

        patient.setWeightKg(
                normalizeWeight(request.getWeightKg())
        );

        /*
         * Intentionally do NOT modify:
         *
         * id
         * refId
         * patientCode
         * organization
         * createdAt
         */

        Patient updatedPatient =
                patientRepository.save(patient);

        return mapToResponse(updatedPatient);
    }

    // ============================================================
    // SOFT DELETE
    // ============================================================

    @Override
    public void deletePatient(String patientRefId) {

        User currentUser = currentUserService.getCurrentUser();

        requireRole(
                currentUser,
                Role.ORG_ADMIN,
                "Only ORG_ADMIN can delete patients."
        );

        Organization organization =
                currentOrganizationService.getCurrentOrganization();

        validateOrganizationAvailable(organization);

        Patient patient =
                patientRepository
                        .findByRefIdAndOrganization_IdAndDeletedAtIsNull(
                                normalizeRefId(patientRefId),
                                organization.getId()
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Patient not found."
                                )
                        );

        patient.setDeletedAt(Instant.now());
        patient.setDeletedBy(currentUser);

        patientRepository.saveAndFlush(patient);
    }

    // ============================================================
    // RESTORE
    // ============================================================

    @Override
    public PatientResponse restorePatient(String patientRefId) {

        User currentUser = currentUserService.getCurrentUser();

        requireRole(
                currentUser,
                Role.ORG_ADMIN,
                "Only ORG_ADMIN can restore patients."
        );

        Organization organization =
                currentOrganizationService.getCurrentOrganization();

        validateOrganizationAvailable(organization);

        /*
         * IMPORTANT:
         *
         * This lookup intentionally does NOT filter deletedAt.
         * Otherwise a deleted patient could never be restored.
         */
        Patient patient =
                patientRepository
                        .findByRefId(normalizeRefId(patientRefId))
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Patient not found."
                                )
                        );

        /*
         * Tenant isolation.
         *
         * A patient belonging to another organization must appear
         * as not found.
         */
        if (!patient.getOrganization()
                .getId()
                .equals(organization.getId())) {

            throw new ResourceNotFoundException(
                    "Patient not found."
            );
        }

        if (patient.getDeletedAt() == null) {
            throw new IllegalStateException(
                    "Patient is already active."
            );
        }

        patient.setDeletedAt(null);
        patient.setDeletedBy(null);

        Patient restoredPatient =
                patientRepository.save(patient);

        return mapToResponse(restoredPatient);
    }

    // ============================================================
    // AGE VALIDATION
    // ============================================================

    private void validateAgeData(
            Boolean dateOfBirthKnown,
            LocalDate dateOfBirth,
            Integer ageValue,
            AgeUnit ageUnit
    ) {

        if (dateOfBirthKnown == null) {
            throw new IllegalArgumentException(
                    "Date of birth known flag is required."
            );
        }

        if (dateOfBirthKnown) {

            if (dateOfBirth == null) {
                throw new IllegalArgumentException(
                        "Date of birth is required when DOB is known."
                );
            }

            if (!dateOfBirth.isBefore(LocalDate.now())) {
                throw new IllegalArgumentException(
                        "Date of birth must be in the past."
                );
            }

            if (ageValue != null || ageUnit != null) {
                throw new IllegalArgumentException(
                        "Age value and age unit must be empty when DOB is known."
                );
            }

            return;
        }

        /*
         * DOB is unknown.
         */

        if (dateOfBirth != null) {
            throw new IllegalArgumentException(
                    "Date of birth must be empty when DOB is unknown."
            );
        }

        if (ageValue == null || ageUnit == null) {
            throw new IllegalArgumentException(
                    "Age value and age unit are required when DOB is unknown."
            );
        }

        validateManualAge(ageValue, ageUnit);
    }

    private void validateManualAge(
            int ageValue,
            AgeUnit ageUnit
    ) {

        if (ageValue <= 0) {
            throw new IllegalArgumentException(
                    "Age must be greater than zero."
            );
        }

        switch (ageUnit) {

            case DAYS -> {
                if (ageValue > 3650) {
                    throw new IllegalArgumentException(
                            "Age in days is too large."
                    );
                }
            }

            case WEEKS -> {
                if (ageValue > 520) {
                    throw new IllegalArgumentException(
                            "Age in weeks is too large."
                    );
                }
            }

            case MONTHS -> {
                if (ageValue > 1800) {
                    throw new IllegalArgumentException(
                            "Age in months is too large."
                    );
                }
            }

            case YEARS -> {
                if (ageValue > 150) {
                    throw new IllegalArgumentException(
                            "Age in years is too large."
                    );
                }
            }
        }
    }

    // ============================================================
    // CURRENT AGE CALCULATION
    // ============================================================

    private AgeDisplay calculateCurrentAge(Patient patient) {

        /*
         * If DOB is unknown, return the manually supplied age.
         */
        if (!patient.isDateOfBirthKnown()) {

            return new AgeDisplay(
                    patient.getAgeValue(),
                    patient.getAgeUnit()
            );
        }

        LocalDate dob = patient.getDateOfBirth();
        LocalDate today = LocalDate.now();

        Period period = Period.between(dob, today);

        /*
         * Prefer years once the patient is at least one year old.
         */
        if (period.getYears() > 0) {

            return new AgeDisplay(
                    period.getYears(),
                    AgeUnit.YEARS
            );
        }

        /*
         * For infants below one year, prefer months.
         */
        if (period.getMonths() > 0) {

            long totalMonths =
                    ChronoUnit.MONTHS.between(
                            dob.withDayOfMonth(1),
                            today.withDayOfMonth(1)
                    );

            return new AgeDisplay(
                    (int) totalMonths,
                    AgeUnit.MONTHS
            );
        }

        long days =
                ChronoUnit.DAYS.between(
                        dob,
                        today
                );

        /*
         * Seven or more days -> weeks.
         */
        if (days >= 7) {

            return new AgeDisplay(
                    (int) (days / 7),
                    AgeUnit.WEEKS
            );
        }

        /*
         * Less than seven days -> days.
         */
        return new AgeDisplay(
                (int) days,
                AgeUnit.DAYS
        );
    }

    private record AgeDisplay(
            int value,
            AgeUnit unit
    ) {
    }

    // ============================================================
    // ORGANIZATION
    // ============================================================

    private void validateOrganizationAvailable(
            Organization organization
    ) {

        if (organization == null) {
            throw new ForbiddenException(
                    "User is not associated with an organization."
            );
        }

        if (organization.getStatus() != OrganizationStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Organization is not active."
            );
        }
    }

    // ============================================================
    // AUTHORIZATION
    // ============================================================

    private void requireRole(
            User currentUser,
            Role requiredRole,
            String message
    ) {

        if (currentUser == null) {
            throw new ForbiddenException(
                    "Authenticated user is required."
            );
        }

        if (currentUser.getRole() != requiredRole) {
            throw new ForbiddenException(message);
        }
    }

    // ============================================================
    // PAGINATION
    // ============================================================

    private void validatePagination(
            int page,
            int size
    ) {

        if (page < 0) {
            throw new IllegalArgumentException(
                    "Page number cannot be negative."
            );
        }

        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException(
                    "Page size must be between 1 and "
                            + MAX_PAGE_SIZE
                            + "."
            );
        }
    }

    private String resolveSortField(String sortBy) {

        if (sortBy == null || sortBy.isBlank()) {
            return "createdAt";
        }

        String normalizedSortBy = sortBy.trim();

        if (!ALLOWED_SORT_FIELDS.contains(normalizedSortBy)) {
            throw new IllegalArgumentException(
                    "Invalid patient sort field."
            );
        }

        return normalizedSortBy;
    }

    private Sort.Direction resolveSortDirection(
            String sortDirection
    ) {

        if (sortDirection == null || sortDirection.isBlank()) {
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
    // NORMALIZATION
    // ============================================================

    private String normalizeRefId(String value) {

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Patient reference ID is required."
            );
        }

        return value.trim();
    }

    private String normalizeName(String value) {

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Patient name is required."
            );
        }

        return value.trim();
    }

    private String normalizePhone(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private String normalizeEmail(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        return value
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private String normalizeOptional(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private BigDecimal normalizeWeight(BigDecimal value) {

        if (value == null) {
            return null;
        }

        return value.stripTrailingZeros();
    }

    // ============================================================
    // RESPONSE MAPPING
    // ============================================================

    private PatientResponse mapToResponse(Patient patient) {
        return mapToResponse(patient, 0L, null);
    }

    private PatientResponse mapToResponse(Patient patient, Long totalReports, Instant lastReportDate) {

        AgeDisplay age =
                calculateCurrentAge(patient);

        return PatientResponse.builder()
                .refId(patient.getRefId())
                .patientCode(patient.getPatientCode())
                .salutation(patient.getSalutation().name())
                .name(patient.getName())
                .dateOfBirthKnown(patient.isDateOfBirthKnown())
                .dateOfBirth(patient.getDateOfBirth())
                .ageValue(age.value())
                .ageUnit(age.unit())
                .gender(patient.getGender())
                .phone(patient.getPhone())
                .email(patient.getEmail())
                .address(patient.getAddress())
                .weightKg(patient.getWeightKg())
                .organizationRefId(
                        patient.getOrganization().getRefId()
                )
                .totalReports(totalReports != null ? totalReports : 0L)
                .lastReportDate(lastReportDate)
                .createdAt(patient.getCreatedAt())
                .updatedAt(patient.getUpdatedAt())
                .build();
    }
}