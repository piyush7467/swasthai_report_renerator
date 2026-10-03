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
        BigDecimal creat = context.getValue(CREAT, "CREATININE", "SERUM_CREATININE", "S_CREATININE");

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

        creat = ClinicalUnitValidator.normalizeCreatinineToMgDl(creat, context.getUnit(CREAT, "CREATININE"));

        if (creat.compareTo(BigDecimal.ZERO) == 0) {
            throw new CalculationException("Cannot calculate Urea / Creatinine ratio because Creatinine is zero");
        }

        return urea.divide(creat, 2, RoundingMode.HALF_UP);
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
