package com.swasthai.report_generator.test.dto.response;

import com.swasthai.report_generator.test.entity.TestParameterDataType;
import com.swasthai.report_generator.test.entity.TestParameterStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestParameterResponse {

    private String refId;

    // Parent test information
    private String testRefId;
    private String testCode;
    private String testName;

    // Parameter identity
    private String code;
    private String name;
    private String description;

    // Data configuration
    private TestParameterDataType dataType;
    private String unit;
    private boolean required;
    private Integer displayOrder;

    // Reference range
    private BigDecimal referenceMin;
    private BigDecimal referenceMax;

    // Critical range
    private BigDecimal criticalLow;
    private BigDecimal criticalHigh;

    // Reporting
    private String reportDescription;
    private String interpretationGuidance;

    // Lifecycle
    private TestParameterStatus status;
    private Integer version;

    private Instant createdAt;
    private Instant updatedAt;
}