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
public class MCHCCalculator implements ParameterCalculator {

    private static final String HGB = "HGB";
    private static final String HCT = "HCT";
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

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
        return calculate(CalculationContext.of(values));
    }

    @Override
    public BigDecimal calculate(CalculationContext context) {
        if (context == null) {
            throw new CalculationException("Calculation context cannot be null");
        }

        BigDecimal hgb = context.getValue(HGB, "HB", "HEMOGLOBIN");
        BigDecimal hct = context.getValue(HCT, "PCV", "HEMATOCRIT");

        if (hgb == null) {
            throw new CalculationException("Required parameter value is missing: HGB");
        }

        if (hct == null) {
            throw new CalculationException("Required parameter value is missing: HCT");
        }

        if (hgb.compareTo(BigDecimal.ZERO) < 0) {
            throw new CalculationException("Parameter value cannot be negative: HGB");
        }

        if (hct.compareTo(BigDecimal.ZERO) < 0) {
            throw new CalculationException("Parameter value cannot be negative: HCT");
        }

        if (hct.compareTo(BigDecimal.ZERO) == 0) {
            throw new CalculationException("Cannot calculate MCHC because HCT is zero");
        }

        hgb = ClinicalUnitValidator.normalizeProteinToGDl(HGB, hgb, context.getUnit(HGB, "HB"));
        hct = ClinicalUnitValidator.normalizeHematocritToPercent(hct, context.getUnit(HCT, "PCV"));

        return hgb
                .multiply(ONE_HUNDRED)
                .divide(hct, 4, RoundingMode.HALF_UP);
    }

    @Override
    public String getFormulaDescription() {
        return "Hemoglobin (g/dL) × 100 / PCV (%)";
    }

    @Override
    public String getExpectedOutputUnit() {
        return "g/dL";
    }
}