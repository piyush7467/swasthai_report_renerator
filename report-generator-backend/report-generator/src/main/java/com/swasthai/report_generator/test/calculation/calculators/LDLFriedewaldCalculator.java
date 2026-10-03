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
public class LDLFriedewaldCalculator implements ParameterCalculator {

    private static final String TC = "TC";
    private static final String HDL = "HDL";
    private static final String TG = "TG";
    private static final BigDecimal FIVE = new BigDecimal("5");
    private static final BigDecimal MAX_TG_FOR_ESTIMATION = new BigDecimal("400");

    @Override
    public CalculationType supports() {
        return CalculationType.LDL_FRIEDEWALD;
    }

    @Override
    public Set<String> requiredParameters() {
        return Set.of(TC, HDL, TG);
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

        BigDecimal tc = context.getValue(TC, "CHOLESTEROL", "TOTAL_CHOLESTEROL", "CHOL");
        BigDecimal hdl = context.getValue(HDL, "HDL_C", "HDL_CHOLESTEROL");
        BigDecimal tg = context.getValue(TG, "TRIGLYCERIDES", "TRIGLYCERIDE", "TRIG");

        if (tc == null) {
            throw new CalculationException("Required parameter value is missing: TC");
        }
        if (hdl == null) {
            throw new CalculationException("Required parameter value is missing: HDL");
        }
        if (tg == null) {
            throw new CalculationException("Required parameter value is missing: TG");
        }

        if (tc.compareTo(BigDecimal.ZERO) < 0) {
            throw new CalculationException("Parameter value cannot be negative: TC");
        }
        if (hdl.compareTo(BigDecimal.ZERO) < 0) {
            throw new CalculationException("Parameter value cannot be negative: HDL");
        }
        if (tg.compareTo(BigDecimal.ZERO) < 0) {
            throw new CalculationException("Parameter value cannot be negative: TG");
        }

        tc = ClinicalUnitValidator.normalizeLipidToMgDl(TC, tc, context.getUnit(TC, "CHOLESTEROL"));
        hdl = ClinicalUnitValidator.normalizeLipidToMgDl(HDL, hdl, context.getUnit(HDL, "HDL_C"));
        tg = ClinicalUnitValidator.normalizeLipidToMgDl(TG, tg, context.getUnit(TG, "TRIGLYCERIDES"));

        if (tg.compareTo(MAX_TG_FOR_ESTIMATION) >= 0) {
            throw new CalculationException(
                    "Triglycerides >= 400 mg/dL: Friedewald equation is invalid. Direct LDL measurement required."
            );
        }

        BigDecimal vldl = tg.divide(FIVE, 4, RoundingMode.HALF_UP);
        BigDecimal ldl = tc.subtract(hdl).subtract(vldl);

        if (ldl.compareTo(BigDecimal.ZERO) < 0) {
            throw new CalculationException(
                    "Calculated LDL-C is negative (" + ldl.setScale(2, RoundingMode.HALF_UP)
                            + " mg/dL), which is biologically implausible (Total Cholesterol < HDL + VLDL). Please verify input values."
            );
        }

        return ldl.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public String getFormulaDescription() {
        return "Total Cholesterol - HDL-C - (Triglycerides / 5) [Friedewald equation, valid when TG < 400 mg/dL]";
    }

    @Override
    public String getExpectedOutputUnit() {
        return "mg/dL";
    }
}
