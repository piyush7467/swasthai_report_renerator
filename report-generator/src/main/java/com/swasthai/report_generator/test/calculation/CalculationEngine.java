package com.swasthai.report_generator.test.calculation;

import com.swasthai.report_generator.test.entity.CalculationType;
import com.swasthai.report_generator.test.entity.TestParameterDataType;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

public interface CalculationEngine {

    BigDecimal calculate(
            CalculationType calculationType,
            Map<String, BigDecimal> values
    );

    boolean isSupported(CalculationType calculationType);

    Set<String> getRequiredParameters(CalculationType calculationType);

    boolean isResultDataTypeSupported(
            CalculationType calculationType,
            TestParameterDataType dataType
    );
}