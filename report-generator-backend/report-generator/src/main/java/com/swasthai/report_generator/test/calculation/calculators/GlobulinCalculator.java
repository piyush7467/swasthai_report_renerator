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
public class GlobulinCalculator implements ParameterCalculator {

    private static final String TP = "TP";
    private static final String ALB = "ALB";

    @Override
    public CalculationType supports() {
        return CalculationType.GLOBULIN;
    }

    @Override
    public Set<String> requiredParameters() {
        return Set.of(TP, ALB);
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

        BigDecimal tp = context.getValue(TP, "TOTAL_PROTEIN", "PROTEIN_TOTAL", "PROTEIN");
        BigDecimal alb = context.getValue(ALB, "ALBUMIN");

        if (tp == null) {
            throw new CalculationException("Required parameter value is missing: TP");
        }
        if (alb == null) {
            throw new CalculationException("Required parameter value is missing: ALB");
        }

        if (tp.compareTo(BigDecimal.ZERO) < 0) {
            throw new CalculationException("Parameter value cannot be negative: TP");
        }
        if (alb.compareTo(BigDecimal.ZERO) < 0) {
            throw new CalculationException("Parameter value cannot be negative: ALB");
        }

        tp = ClinicalUnitValidator.normalizeProteinToGDl(TP, tp, context.getUnit(TP, "TOTAL_PROTEIN"));
        alb = ClinicalUnitValidator.normalizeProteinToGDl(ALB, alb, context.getUnit(ALB, "ALBUMIN"));

        if (alb.compareTo(tp) > 0) {
            throw new CalculationException(
                    "Calculated Globulin is invalid: Albumin (" + alb
                            + " g/dL) cannot exceed Total Protein (" + tp + " g/dL)."
            );
        }

        return tp.subtract(alb).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public String getFormulaDescription() {
        return "Total Protein - Albumin";
    }

    @Override
    public String getExpectedOutputUnit() {
        return "g/dL";
    }
}
