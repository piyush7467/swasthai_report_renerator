package com.swasthai.report_generator.test.controller;

import com.swasthai.report_generator.common.response.ApiResponse;
import com.swasthai.report_generator.test.dto.request.CalculateTestRequest;
import com.swasthai.report_generator.test.dto.response.CalculatedTestResponse;
import com.swasthai.report_generator.test.service.TestCalculationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
public class TestCalculationController {

    private final TestCalculationService testCalculationService;

    @PostMapping("/tests/{testRefId}/calculate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'LAB_STAFF')")
    public ApiResponse<CalculatedTestResponse> calculateTestParameters(
            @PathVariable String testRefId,
            @Valid @RequestBody CalculateTestRequest request
    ) {
        return ApiResponse.success(
                "Test parameters calculated successfully",
                testCalculationService.calculateParameters(
                        testRefId,
                        request
                )
        );
    }
}
