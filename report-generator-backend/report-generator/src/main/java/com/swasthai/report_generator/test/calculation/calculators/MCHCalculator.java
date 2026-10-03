package com.swasthai.report_generator.test.calculation.calculators;

import com.swasthai.report_generator.test.calculation.CalculationContext;
import com.swasthai.report_generator.test.calculation.CalculationException;
import com.swasthai.report_generator.test.calculation.ClinicalUnitValidator;
import com.swasthai.report_generator.test.calculation.ParameterCalculator;
import com.swasthai.report_generator.test.entity.CalculationType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Set;

@Component
public class MCHCalculator implements ParameterCalculator {

    private static final String HGB = "HGB";
    private static final String RBC = "RBC";

    @Override
    public CalculationType supports() {
        return CalculationType.MCH;
    }

    @Override
    public Set<String> requiredParameters() {
        return Set.of(HGB, RBC);
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

        BigDecimal hgb = context.getValue(HGB, "HB", "HEMOGLOBIN");
        BigDecimal rbc = context.getValue(RBC, "RBC_COUNT");

        if (hgb == null) {
            throw new CalculationException("Required parameter value is missing: HGB");
        }

        if (rbc == null) {
            throw new CalculationException("Required parameter value is missing: RBC");
        }

        if (hgb.compareTo(BigDecimal.ZERO) < 0) {
            throw new CalculationException("Parameter value cannot be negative: HGB");
        }

        if (rbc.compareTo(BigDecimal.ZERO) < 0) {
            throw new CalculationException("Parameter value cannot be negative: RBC");
        }

        if (rbc.compareTo(BigDecimal.ZERO) == 0) {
            throw new CalculationException("Cannot calculate MCH because RBC is zero");
        }

        hgb = ClinicalUnitValidator.normalizeProteinToGDl(HGB, hgb, context.getUnit(HGB, "HB"));

        return hgb
                .multiply(BigDecimal.TEN)
                .divide(rbc, 4, RoundingMode.HALF_UP);
    }

    @Override
    public String getFormulaDescription() {
        return "Hemoglobin (g/dL) × 10 / RBC (million/µL)";
    }

    @Override
    public String getExpectedOutputUnit() {
        return "pg";
    }
}