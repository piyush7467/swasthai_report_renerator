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
public class CholesterolHDLtoRatioCalculator implements ParameterCalculator {

    private static final String TC = "TC";
    private static final String HDL = "HDL";

    @Override
    public CalculationType supports() {
        return CalculationType.CHOL_HDL_RATIO;
    }

    @Override
    public Set<String> requiredParameters() {
        return Set.of(TC, HDL);
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

        if (tc == null) {
            throw new CalculationException("Required parameter value is missing: TC");
        }
        if (hdl == null) {
            throw new CalculationException("Required parameter value is missing: HDL");
        }

        if (tc.compareTo(BigDecimal.ZERO) < 0) {
            throw new CalculationException("Parameter value cannot be negative: TC");
        }
        if (hdl.compareTo(BigDecimal.ZERO) < 0) {
            throw new CalculationException("Parameter value cannot be negative: HDL");
        }

        tc = ClinicalUnitValidator.normalizeLipidToMgDl(TC, tc, context.getUnit(TC, "CHOLESTEROL"));
        hdl = ClinicalUnitValidator.normalizeLipidToMgDl(HDL, hdl, context.getUnit(HDL, "HDL_C"));

        if (hdl.compareTo(BigDecimal.ZERO) == 0) {
            throw new CalculationException("Cannot calculate Cholesterol / HDL ratio because HDL is zero");
        }

        return tc.divide(hdl, 2, RoundingMode.HALF_UP);
    }

    @Override
    public String getFormulaDescription() {
        return "Total Cholesterol / HDL-C";
    }

    @Override
    public String getExpectedOutputUnit() {
        return "";
    }
}
