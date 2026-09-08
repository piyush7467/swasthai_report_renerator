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
public class MCVCalculator implements ParameterCalculator {

    private static final String HCT = "HCT";
    private static final String RBC = "RBC";

    @Override
    public CalculationType supports() {
        return CalculationType.MCV;
    }

    @Override
    public Set<String> requiredParameters() {
        return Set.of(HCT, RBC);
    }

    @Override
    public BigDecimal calculate(Map<String, BigDecimal> values) {

        BigDecimal hct = values.get(HCT);
        BigDecimal rbc = values.get(RBC);

        if (hct == null) {
            throw new CalculationException(
                    "Required parameter value is missing: HCT"
            );
        }

        if (rbc == null) {
            throw new CalculationException(
                    "Required parameter value is missing: RBC"
            );
        }

        if (hct.compareTo(BigDecimal.ZERO) < 0) {
            throw new CalculationException(
                    "Parameter value cannot be negative: HCT"
            );
        }

        if (rbc.compareTo(BigDecimal.ZERO) < 0) {
            throw new CalculationException(
                    "Parameter value cannot be negative: RBC"
            );
        }

        if (rbc.compareTo(BigDecimal.ZERO) == 0) {
            throw new CalculationException(
                    "Cannot calculate MCV because RBC is zero"
            );
        }

        return hct
                .multiply(BigDecimal.TEN)
                .divide(rbc, 4, RoundingMode.HALF_UP);
    }
}