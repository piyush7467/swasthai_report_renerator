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
public class UreaCreatinineRatioCalculator implements ParameterCalculator {

    private static final String UREA = "UREA";
    private static final String CREAT = "CREAT";

    @Override
    public CalculationType supports() {
        return CalculationType.UREA_CREATININE_RATIO;
    }

    @Override
    public Set<String> requiredParameters() {
        return Set.of(UREA, CREAT);
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

        BigDecimal urea = context.getValue(UREA, "BLOOD_UREA", "SERUM_UREA");
        if (urea == null) {
            BigDecimal bun = context.getValue("BUN", "BLOOD_UREA_NITROGEN");
            if (bun != null) {
                if (bun.compareTo(BigDecimal.ZERO) < 0) {
                    throw new CalculationException("Parameter value cannot be negative: BUN");
                }
                // Urea (mg/dL) = BUN (mg/dL) * approximately 2.14
                urea = bun.multiply(new BigDecimal("2.14")).setScale(4, RoundingMode.HALF_UP);
            }
        }

        BigDecimal creat = context.getValue(CREAT, "CREA", "CREATININE", "SERUM_CREATININE", "S_CREATININE");

        if (urea == null) {
            throw new CalculationException("Required parameter value is missing: UREA");
        }
        if (creat == null) {
            throw new CalculationException("Required parameter value is missing: CREAT");
        }

        if (urea.compareTo(BigDecimal.ZERO) < 0) {
            throw new CalculationException("Parameter value cannot be negative: UREA");
        }
        if (creat.compareTo(BigDecimal.ZERO) < 0) {
            throw new CalculationException("Parameter value cannot be negative: CREAT");
        }

        creat = ClinicalUnitValidator.normalizeCreatinineToMgDl(creat, context.getUnit(CREAT, "CREA", "CREATININE"));

        if (creat.compareTo(BigDecimal.ZERO) == 0) {
            throw new CalculationException("Cannot calculate Urea / Creatinine ratio because Creatinine is zero");
        }

        return urea.divide(creat, 2, RoundingMode.HALF_UP);
    }

    @Override
    public void validateContext(CalculationContext context) {
        if (context == null) {
            throw new CalculationException("Calculation context cannot be null");
        }

        boolean hasUrea = context.containsKey(UREA, "BLOOD_UREA", "SERUM_UREA");
        boolean hasBun = context.containsKey("BUN", "BLOOD_UREA_NITROGEN");

        if (!hasUrea && !hasBun) {
            throw new CalculationException("Required parameter is missing: UREA");
        }

        if (hasUrea && context.getValue(UREA, "BLOOD_UREA", "SERUM_UREA") == null) {
            throw new CalculationException("Required parameter value is null: UREA");
        }

        if (!hasUrea && hasBun && context.getValue("BUN", "BLOOD_UREA_NITROGEN") == null) {
            throw new CalculationException("Required parameter value is null: BUN");
        }

        if (!context.containsKey(CREAT, "CREA", "CREATININE", "SERUM_CREATININE", "S_CREATININE")) {
            throw new CalculationException("Required parameter is missing: CREAT");
        }

        if (context.getValue(CREAT, "CREA", "CREATININE", "SERUM_CREATININE", "S_CREATININE") == null) {
            throw new CalculationException("Required parameter value is null: CREAT");
        }
    }

    @Override
    public String getFormulaDescription() {
        return "Urea / Creatinine";
    }

    @Override
    public String getExpectedOutputUnit() {
        return "";
    }
}
