package com.swasthai.report_generator.test.calculation.calculators;

import com.swasthai.report_generator.test.calculation.CalculationContext;
import com.swasthai.report_generator.test.calculation.CalculationException;
import com.swasthai.report_generator.test.calculation.ParameterCalculator;
import com.swasthai.report_generator.test.entity.CalculationType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Set;

@Component
public class IndirectBilirubinCalculator implements ParameterCalculator {

    private static final String TBIL = "TBIL";
    private static final String DBIL = "DBIL";

    @Override
    public CalculationType supports() {
        return CalculationType.INDIRECT_BILIRUBIN;
    }

    @Override
    public Set<String> requiredParameters() {
        return Set.of(TBIL, DBIL);
    }

    @Override
    public BigDecimal calculate(Map<String, BigDecimal> values) {
        return calculate(CalculationContext.of(values));
    }

    @Override
    public BigDecimal calculate(CalculationContext context) {
        if (context == null) {
            throw new CalculationException("Calculation context cannot be null");
        }

        BigDecimal tbil = context.getValue(TBIL, "TOTAL_BILIRUBIN", "BILIRUBIN_TOTAL", "T_BIL", "T_BILIRUBIN");
        BigDecimal dbil = context.getValue(DBIL, "DIRECT_BILIRUBIN", "BILIRUBIN_DIRECT", "D_BIL", "D_BILIRUBIN");

        if (tbil == null) {
            throw new CalculationException("Required parameter value is missing: TBIL");
        }
        if (dbil == null) {
            throw new CalculationException("Required parameter value is missing: DBIL");
        }

        if (tbil.compareTo(BigDecimal.ZERO) < 0) {
            throw new CalculationException("Parameter value cannot be negative: TBIL");
        }
        if (dbil.compareTo(BigDecimal.ZERO) < 0) {
            throw new CalculationException("Parameter value cannot be negative: DBIL");
        }

        if (dbil.compareTo(tbil) > 0) {
            throw new CalculationException(
                    "Calculated Indirect Bilirubin is invalid: Direct Bilirubin (" + dbil
                            + ") cannot exceed Total Bilirubin (" + tbil + ")."
            );
        }

        return tbil.subtract(dbil).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public String getFormulaDescription() {
        return "Total Bilirubin - Direct Bilirubin";
    }

    @Override
    public String getExpectedOutputUnit() {
        return "mg/dL";
    }
}
