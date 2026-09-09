package com.swasthai.report_generator.report.dto.response;

import com.swasthai.report_generator.test.entity.CalculationType;
import com.swasthai.report_generator.test.entity.ParameterInputType;
import com.swasthai.report_generator.test.entity.ResultFlag;
import com.swasthai.report_generator.test.entity.TestParameterDataType;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record ReportParameterItemResponse(
        String refId,
        String parameterRefId,
        String parameterCode,
        String parameterName,
        TestParameterDataType dataType,
        ParameterInputType inputType,
        CalculationType calculationType,
        String calculationVersion,
        String unit,
        String value,
        BigDecimal numericValue,
        ResultFlag flag,
        BigDecimal referenceMin,
        BigDecimal referenceMax,
        BigDecimal criticalLow,
        BigDecimal criticalHigh,
        Integer displayOrder
) {
}