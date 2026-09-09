package com.swasthai.report_generator.test.calculation;

import com.swasthai.report_generator.test.entity.CalculationType;
import com.swasthai.report_generator.test.entity.TestParameterDataType;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

public interface ParameterCalculator {

    CalculationType supports();

    Set<String> requiredParameters();

    BigDecimal calculate(Map<String, BigDecimal> values);

    default boolean isResultDataTypeSupported(TestParameterDataType dataType) {
        return dataType == TestParameterDataType.DECIMAL;
    }
}