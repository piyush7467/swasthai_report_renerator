package com.swasthai.report_generator.license.service;

import com.swasthai.report_generator.license.dto.request.CreatePlanRequest;
import com.swasthai.report_generator.license.dto.request.UpdatePlanRequest;
import com.swasthai.report_generator.license.dto.response.PlanResponse;

import java.util.List;

public interface PlanService {

    PlanResponse createPlan(CreatePlanRequest request);

    PlanResponse updatePlan(
            String planRefId,
            UpdatePlanRequest request
    );

    PlanResponse getPlan(String planRefId);

    List<PlanResponse> getAllPlans();

    List<PlanResponse> getActivePlans();
}