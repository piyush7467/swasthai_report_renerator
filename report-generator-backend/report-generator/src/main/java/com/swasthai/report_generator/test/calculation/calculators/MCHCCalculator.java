package com.swasthai.report_generator.test.calculation.calculators;

import com.swasthai.report_generator.test.calculation.CalculationException;
import com.swasthai.report_generator.test.calculation.ParameterCalculator;
import com.swasthai.report_generator.test.entity.CalculationType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Set;

@Component
public class MCHCCalculator implements ParameterCalculator {

    private static final String HGB = "HGB";
    private static final String HCT = "HCT";

    @Override
    public CalculationType supports() {
        return CalculationType.MCHC;
    }

    @Override
    public Set<String> requiredParameters() {
        return Set.of(HGB, HCT);
    }

    @Override
    public BigDecimal calculate(Map<String, BigDecimal> values) {

        BigDecimal hgb = values.get(HGB);
        BigDecimal hct = values.get(HCT);

        if (hgb == null) {
            throw new CalculationException(
                    "Required parameter value is missing: HGB"
            );
        }

        if (hct == null) {
            throw new CalculationException(
                    "Required parameter value is missing: HCT"
            );
        }

        if (hgb.compareTo(BigDecimal.ZERO) < 0) {
            throw new CalculationException(
                    "Parameter value cannot be negative: HGB"
            );
        }

        if (hct.compareTo(BigDecimal.ZERO) < 0) {
            throw new CalculationException(
                    "Parameter value cannot be negative: HCT"
            );
        }

        if (hct.compareTo(BigDecimal.ZERO) == 0) {
            throw new CalculationException(
                    "Cannot calculate MCHC because HCT is zero"
            );
        }

        return hgb
                .multiply(BigDecimal.valueOf(100))
                .divide(hct, 4, RoundingMode.HALF_UP);
    }
}