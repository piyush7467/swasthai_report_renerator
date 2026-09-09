package com.swasthai.report_generator.report.dto.request;

import com.swasthai.report_generator.test.dto.request.TestParameterResultInput;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request payload for autosaving/updating parameters of a test within a report.
 */
public record UpdateReportParametersRequest(
        Long lockVersion,

        @NotEmpty(message = "Parameter inputs list cannot be empty")
        @Size(max = 100, message = "Cannot submit more than 100 parameters at once")
        List<@Valid TestParameterResultInput> parameters
) {
}