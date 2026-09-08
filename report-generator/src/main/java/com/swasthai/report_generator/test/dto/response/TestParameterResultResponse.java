package com.swasthai.report_generator.test.dto.response;

import com.swasthai.report_generator.test.entity.CalculationType;
import com.swasthai.report_generator.test.entity.ParameterInputType;
import com.swasthai.report_generator.test.entity.ResultFlag;
import com.swasthai.report_generator.test.entity.TestParameterDataType;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Public response DTO for a single parameter result snapshot.
 *
 * Security & Design:
 * - Read-only representation.
 * - Uses immutable public refIds (refId, parameterRefId) instead of internal UUIDs.
 * - Snapshot captures configuration at the time the result was generated.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestParameterResultResponse {

    private String refId;

    // Associated parameter identity
    private String parameterRefId;
    private String parameterCode;
    private String parameterName;

    // Parameter configuration snapshot
    private TestParameterDataType dataType;
    private ParameterInputType inputType;
    private CalculationType calculationType;
    private String calculationVersion;
    private String unit;

    // Result values
    private String value;
    private BigDecimal numericValue;
    private ResultFlag flag;

    // Range snapshots
    private BigDecimal referenceMin;
    private BigDecimal referenceMax;
    private BigDecimal criticalLow;
    private BigDecimal criticalHigh;

    private Integer displayOrder;

    // Timestamps
    private Instant createdAt;
    private Instant updatedAt;
}