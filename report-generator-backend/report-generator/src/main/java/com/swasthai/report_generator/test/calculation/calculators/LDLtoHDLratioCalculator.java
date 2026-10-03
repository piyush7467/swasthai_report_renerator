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
public class LDLtoHDLratioCalculator implements ParameterCalculator {

    private static final String LDL = "LDL";
    private static final String HDL = "HDL";

    @Override
    public CalculationType supports() {
        return CalculationType.LDL_HDL_RATIO;
    }

    @Override
    public Set<String> requiredParameters() {
        return Set.of(LDL, HDL);
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

        BigDecimal ldl = context.getValue(LDL, "LDL_C", "LDL_CHOLESTEROL", "LDL_FRIEDEWALD");
        BigDecimal hdl = context.getValue(HDL, "HDL_C", "HDL_CHOLESTEROL");

        if (ldl == null) {
            throw new CalculationException("Required parameter value is missing: LDL");
        }
        if (hdl == null) {
            throw new CalculationException("Required parameter value is missing: HDL");
        }

        if (ldl.compareTo(BigDecimal.ZERO) < 0) {
            throw new CalculationException("Parameter value cannot be negative: LDL");
        }
        if (hdl.compareTo(BigDecimal.ZERO) < 0) {
            throw new CalculationException("Parameter value cannot be negative: HDL");
        }

        ldl = ClinicalUnitValidator.normalizeLipidToMgDl(LDL, ldl, context.getUnit(LDL, "LDL_C"));
        hdl = ClinicalUnitValidator.normalizeLipidToMgDl(HDL, hdl, context.getUnit(HDL, "HDL_C"));

        if (hdl.compareTo(BigDecimal.ZERO) == 0) {
            throw new CalculationException("Cannot calculate LDL / HDL ratio because HDL is zero");
        }

        return ldl.divide(hdl, 2, RoundingMode.HALF_UP);
    }

    @Override
    public String getFormulaDescription() {
        return "LDL-C / HDL-C";
    }

    @Override
    public String getExpectedOutputUnit() {
        return "";
    }
}
