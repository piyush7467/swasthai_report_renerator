package com.swasthai.report_generator.test.dto.response;

import com.swasthai.report_generator.test.entity.CalculationType;
import com.swasthai.report_generator.test.entity.ParameterInputType;
import com.swasthai.report_generator.test.entity.ResultFlag;
import com.swasthai.report_generator.test.entity.TestParameterDataType;

import java.math.BigDecimal;

public record CalculatedParameterItemResponse(
        String parameterRefId,
        String parameterCode,
        String parameterName,
        ParameterInputType inputType,
        CalculationType calculationType,
        TestParameterDataType dataType,
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
