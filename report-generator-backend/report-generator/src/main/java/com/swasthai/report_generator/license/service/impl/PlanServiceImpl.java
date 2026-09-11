package com.swasthai.report_generator.license.service.impl;

import com.swasthai.report_generator.common.exception.ResourceAlreadyExistsException;
import com.swasthai.report_generator.common.exception.ResourceNotFoundException;
import com.swasthai.report_generator.license.dto.request.CreatePlanRequest;
import com.swasthai.report_generator.license.dto.request.UpdatePlanRequest;
import com.swasthai.report_generator.license.dto.response.PlanResponse;
import com.swasthai.report_generator.license.entity.Plan;
import com.swasthai.report_generator.license.repository.PlanRepository;
import com.swasthai.report_generator.license.service.PlanService;
import com.swasthai.report_generator.user.entity.Role;
import com.swasthai.report_generator.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class PlanServiceImpl implements PlanService {

    private final PlanRepository planRepository;

    @Override
    public PlanResponse createPlan(
            CreatePlanRequest request
    ) {

        requireSuperAdmin();

        if (request == null) {
            throw new IllegalArgumentException(
                    "Plan request is required"
            );
        }

        String code = normalizeCode(request.code());

        if (planRepository.existsByCodeIgnoreCase(code)) {
            throw new ResourceAlreadyExistsException(
                    "Plan code already exists."
            );
        }

        Plan plan = Plan.builder()
                .code(code)
                .name(normalizeRequired(request.name(), "Plan name"))
                .description(normalizeOptional(request.description()))
                .annualPrice(request.annualPrice())
                .currency(
                        request.currency()
                                .trim()
                                .toUpperCase(Locale.ROOT)
                )
                .active(
                        request.active() == null
                                || request.active()
                )
                .build();

        try {

            Plan saved = planRepository.save(plan);

            return mapToResponse(saved);

        } catch (DataIntegrityViolationException exception) {

            throw new ResourceAlreadyExistsException(
                    "Plan could not be created because its code already exists."
            );
        }
    }

    @Override
    public PlanResponse updatePlan(
            String planRefId,
            UpdatePlanRequest request
    ) {

        requireSuperAdmin();

        String normalizedRefId =
                normalizeRefId(planRefId);

        if (request == null) {
            throw new IllegalArgumentException(
                    "Plan request is required"
            );
        }

        Plan plan =
                planRepository.findByRefId(normalizedRefId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Plan not found."
                                )
                        );

        plan.setName(
                normalizeRequired(
                        request.name(),
                        "Plan name"
                )
        );

        plan.setDescription(
                normalizeOptional(
                        request.description()
                )
        );

        plan.setAnnualPrice(
                request.annualPrice()
        );

        plan.setCurrency(
                request.currency()
                        .trim()
                        .toUpperCase(Locale.ROOT)
        );

        plan.setActive(
                request.active()
        );

        return mapToResponse(
                planRepository.save(plan)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PlanResponse getPlan(
            String planRefId
    ) {

        requireSuperAdmin();

        Plan plan =
                planRepository.findByRefId(
                                normalizeRefId(planRefId)
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Plan not found."
                                )
                        );

        return mapToResponse(plan);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlanResponse> getAllPlans() {

        requireSuperAdmin();

        return planRepository
                .findAllByOrderByAnnualPriceAsc()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlanResponse> getActivePlans() {

        /*
         * Public/organization-facing plan selection can be
         * added later. For now this method remains SUPER_ADMIN-only
         * to avoid accidentally exposing commercial configuration.
         */
        requireSuperAdmin();

        return planRepository
                .findAllByOrderByAnnualPriceAsc()
                .stream()
                .filter(Plan::isActive)
                .map(this::mapToResponse)
                .toList();
    }

    private void requireSuperAdmin() {

        User currentUser =
                getCurrentUser();

        if (currentUser.getRole() != Role.SUPER_ADMIN) {
            throw new AccessDeniedException(
                    "Only SUPER_ADMIN can manage plans."
            );
        }
    }

    private User getCurrentUser() {

        var authentication =
                org.springframework.security.core.context
                        .SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getDetails() instanceof User user)) {

            throw new AccessDeniedException(
                    "Authenticated user is required."
            );
        }

        return user;
    }

    private String normalizeRefId(String value) {

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Plan reference ID is required."
            );
        }

        return value.trim();
    }

    private String normalizeCode(String value) {

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Plan code is required."
            );
        }

        return value.trim()
                .toUpperCase(Locale.ROOT);
    }

    private String normalizeRequired(
            String value,
            String field
    ) {

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    field + " is required."
            );
        }

        return value.trim();
    }

    private String normalizeOptional(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private PlanResponse mapToResponse(
            Plan plan
    ) {

        return new PlanResponse(
                plan.getRefId(),
                plan.getCode(),
                plan.getName(),
                plan.getDescription(),
                plan.getAnnualPrice(),
                plan.getCurrency(),
                plan.isActive(),
                plan.getCreatedAt(),
                plan.getUpdatedAt()
        );
    }
}