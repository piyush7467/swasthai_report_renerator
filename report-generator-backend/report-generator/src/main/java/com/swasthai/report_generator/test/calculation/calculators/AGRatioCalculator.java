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
public class AGRatioCalculator implements ParameterCalculator {

    private static final String ALB = "ALB";
    private static final String GLOB = "GLOB";

    @Override
    public CalculationType supports() {
        return CalculationType.AG_RATIO;
    }

    @Override
    public Set<String> requiredParameters() {
        return Set.of(ALB, GLOB);
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

        BigDecimal alb = context.getValue(ALB, "ALBUMIN");
        BigDecimal glob = context.getValue(GLOB, "GLOBULIN");

        // If globulin is not directly present, check if TP is present to derive it
        if (glob == null && alb != null) {
            BigDecimal tp = context.getValue("TP", "TOTAL_PROTEIN", "PROTEIN_TOTAL", "PROTEIN");
            if (tp != null) {
                tp = ClinicalUnitValidator.normalizeProteinToGDl("TP", tp, context.getUnit("TP"));
                alb = ClinicalUnitValidator.normalizeProteinToGDl(ALB, alb, context.getUnit(ALB, "ALBUMIN"));
                if (alb.compareTo(tp) <= 0) {
                    glob = tp.subtract(alb);
                }
            }
        }

        if (alb == null) {
            throw new CalculationException("Required parameter value is missing: ALB");
        }
        if (glob == null) {
            throw new CalculationException("Required parameter value is missing: GLOB");
        }

        if (alb.compareTo(BigDecimal.ZERO) < 0) {
            throw new CalculationException("Parameter value cannot be negative: ALB");
        }
        if (glob.compareTo(BigDecimal.ZERO) < 0) {
            throw new CalculationException("Parameter value cannot be negative: GLOB");
        }

        alb = ClinicalUnitValidator.normalizeProteinToGDl(ALB, alb, context.getUnit(ALB, "ALBUMIN"));
        glob = ClinicalUnitValidator.normalizeProteinToGDl(GLOB, glob, context.getUnit(GLOB, "GLOBULIN"));

        if (glob.compareTo(BigDecimal.ZERO) == 0) {
            throw new CalculationException("Cannot calculate A/G ratio because Globulin is zero");
        }

        return alb.divide(glob, 2, RoundingMode.HALF_UP);
    }

    @Override
    public String getFormulaDescription() {
        return "Albumin / Globulin";
    }

    @Override
    public String getExpectedOutputUnit() {
        return "";
    }
}
