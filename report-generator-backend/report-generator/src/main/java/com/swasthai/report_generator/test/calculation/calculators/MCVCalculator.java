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
        return calculate(CalculationContext.of(values));
    }

    @Override
    public BigDecimal calculate(CalculationContext context) {
        if (context == null) {
            throw new CalculationException("Calculation context cannot be null");
        }

        BigDecimal hct = context.getValue(HCT, "PCV");
        BigDecimal rbc = context.getValue(RBC, "RBC_COUNT");

        if (hct == null) {
            throw new CalculationException("Required parameter value is missing: HCT");
        }

        if (rbc == null) {
            throw new CalculationException("Required parameter value is missing: RBC");
        }

        if (hct.compareTo(BigDecimal.ZERO) < 0) {
            throw new CalculationException("Parameter value cannot be negative: HCT");
        }

        if (rbc.compareTo(BigDecimal.ZERO) < 0) {
            throw new CalculationException("Parameter value cannot be negative: RBC");
        }

        if (rbc.compareTo(BigDecimal.ZERO) == 0) {
            throw new CalculationException("Cannot calculate MCV because RBC is zero");
        }

        hct = ClinicalUnitValidator.normalizeHematocritToPercent(hct, context.getUnit(HCT, "PCV"));

        return hct
                .multiply(BigDecimal.TEN)
                .divide(rbc, 4, RoundingMode.HALF_UP);
    }

    @Override
    public String getFormulaDescription() {
        return "PCV (%) × 10 / RBC (million/µL)";
    }

    @Override
    public String getExpectedOutputUnit() {
        return "fL";
    }
}