package com.swasthai.report_generator.test.calculation;

import lombok.Getter;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/**
 * Clinical validation exception thrown when CBC differential leukocyte count
 * violates clinical rules (e.g. incomplete differential, total deviating beyond approved tolerance).
 */
@Getter
public class CbcDifferentialValidationException extends IllegalArgumentException {

    private final BigDecimal totalSum;
    private final BigDecimal tolerance;
    private final List<String> missingParameters;

    public CbcDifferentialValidationException(String message) {
        super(message);
        this.totalSum = null;
        this.tolerance = null;
        this.missingParameters = Collections.emptyList();
    }

    public CbcDifferentialValidationException(String message, BigDecimal totalSum, BigDecimal tolerance) {
        super(message);
        this.totalSum = totalSum;
        this.tolerance = tolerance;
        this.missingParameters = Collections.emptyList();
    }

    public CbcDifferentialValidationException(String message, List<String> missingParameters) {
        super(message);
        this.totalSum = null;
        this.tolerance = null;
        this.missingParameters = missingParameters != null ? List.copyOf(missingParameters) : Collections.emptyList();
    }
}
