package com.swasthai.report_generator.test.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CalculateTestRequest(
        @NotEmpty(message = "Parameter values list cannot be empty")
        @Size(max = 100, message = "Cannot submit more than 100 parameters")
        List<@Valid ParameterValueInput> parameters
) {
}
