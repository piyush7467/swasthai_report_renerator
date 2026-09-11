package com.swasthai.report_generator.license.repository;

import com.swasthai.report_generator.license.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlanRepository
        extends JpaRepository<Plan, UUID> {

    Optional<Plan> findByRefId(String refId);

    Optional<Plan> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    List<Plan> findAllByOrderByAnnualPriceAsc();
}