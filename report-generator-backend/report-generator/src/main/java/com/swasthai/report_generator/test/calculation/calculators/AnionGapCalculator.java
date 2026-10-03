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
public class AnionGapCalculator implements ParameterCalculator {

    private static final String NA = "NA";
    private static final String CL = "CL";
    private static final String HCO3 = "HCO3";

    @Override
    public CalculationType supports() {
        return CalculationType.ANION_GAP;
    }

    @Override
    public Set<String> requiredParameters() {
        return Set.of(NA, CL, HCO3);
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

        BigDecimal na = context.getValue(NA, "SODIUM");
        BigDecimal cl = context.getValue(CL, "CHLORIDE");
        BigDecimal hco3 = context.getValue(HCO3, "BICARBONATE", "CO2", "TOTAL_CO2");

        if (na == null) {
            throw new CalculationException("Required parameter value is missing: NA");
        }
        if (cl == null) {
            throw new CalculationException("Required parameter value is missing: CL");
        }
        if (hco3 == null) {
            throw new CalculationException("Required parameter value is missing: HCO3");
        }

        if (na.compareTo(BigDecimal.ZERO) < 0) {
            throw new CalculationException("Parameter value cannot be negative: NA");
        }
        if (cl.compareTo(BigDecimal.ZERO) < 0) {
            throw new CalculationException("Parameter value cannot be negative: CL");
        }
        if (hco3.compareTo(BigDecimal.ZERO) < 0) {
            throw new CalculationException("Parameter value cannot be negative: HCO3");
        }

        BigDecimal anions = cl.add(hco3);
        BigDecimal ag = na.subtract(anions);

        return ag.setScale(1, RoundingMode.HALF_UP);
    }

    @Override
    public String getFormulaDescription() {
        return "Na - (Cl + HCO3)";
    }

    @Override
    public String getExpectedOutputUnit() {
        return "mEq/L";
    }
}
