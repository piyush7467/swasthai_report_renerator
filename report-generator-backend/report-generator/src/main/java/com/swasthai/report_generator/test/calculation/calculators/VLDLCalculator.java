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
public class VLDLCalculator implements ParameterCalculator {

    private static final String TG = "TG";
    private static final BigDecimal FIVE = new BigDecimal("5");
    private static final BigDecimal MAX_TG_FOR_ESTIMATION = new BigDecimal("400");

    @Override
    public CalculationType supports() {
        return CalculationType.VLDL;
    }

    @Override
    public Set<String> requiredParameters() {
        return Set.of(TG);
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

        BigDecimal tg = context.getValue(TG, "TRIGLYCERIDES", "TRIGLYCERIDE", "TRIG");

        if (tg == null) {
            throw new CalculationException("Required parameter value is missing: TG");
        }

        if (tg.compareTo(BigDecimal.ZERO) < 0) {
            throw new CalculationException("Parameter value cannot be negative: TG");
        }

        tg = ClinicalUnitValidator.normalizeLipidToMgDl(TG, tg, context.getUnit(TG, "TRIGLYCERIDES"));

        if (tg.compareTo(MAX_TG_FOR_ESTIMATION) >= 0) {
            throw new CalculationException(
                    "Triglycerides >= 400 mg/dL: Friedewald estimation of VLDL-C is invalid. Direct measurement required."
            );
        }

        return tg.divide(FIVE, 2, RoundingMode.HALF_UP);
    }

    @Override
    public String getFormulaDescription() {
        return "Triglycerides / 5 (Friedewald estimation, valid when TG < 400 mg/dL)";
    }

    @Override
    public String getExpectedOutputUnit() {
        return "mg/dL";
    }
}
