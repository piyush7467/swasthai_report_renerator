package com.swasthai.report_generator.test.calculation.calculators;

import com.swasthai.report_generator.patient.entity.Gender;
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

/**
 * Calculates Estimated Glomerular Filtration Rate (eGFR) using the 2021 CKD-EPI Creatinine equation
 * (refactored without race variable, endorsed by NKF and ASN).
 * <p>
 * Equation:
 * eGFR = 142 * min(Scr / kappa, 1)^alpha * max(Scr / kappa, 1)^-1.200 * 0.9938^Age * (1.012 if female)
 * <p>
 * Where:
 * - Scr is serum creatinine in mg/dL
 * - kappa = 0.7 (female), 0.9 (male)
 * - alpha = -0.241 (female), -0.302 (male)
 * - Age is in years (validated >= 18)
 */
@Component
public class EgfrCkdEpi2021Calculator implements ParameterCalculator {

    private static final String CREAT = "CREAT";

    @Override
    public CalculationType supports() {
        return CalculationType.EGFR_CKD_EPI_2021;
    }

    @Override
    public Set<String> requiredParameters() {
        return Set.of(CREAT);
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

        BigDecimal creat = context.getValue(CREAT, "CREATININE", "SERUM_CREATININE", "S_CREATININE");
        if (creat == null) {
            throw new CalculationException("Required parameter value is missing: CREAT");
        }

        if (creat.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CalculationException("Serum creatinine must be greater than zero to calculate eGFR");
        }

        creat = ClinicalUnitValidator.normalizeCreatinineToMgDl(creat, context.getUnit(CREAT, "CREATININE"));

        Integer age = context.getPatientAgeInYears();
        if (age == null) {
            throw new CalculationException("Patient age is required to calculate eGFR (CKD-EPI 2021)");
        }

        if (age < 18) {
            throw new CalculationException(
                    "eGFR CKD-EPI (2021) equation is only validated for adults aged 18 and older. Pediatric eGFR requires the Schwartz equation."
            );
        }

        Gender gender = context.getPatientGender();
        if (gender == null || (gender != Gender.MALE && gender != Gender.FEMALE)) {
            throw new CalculationException(
                    "Patient biological sex (MALE or FEMALE) is required to calculate eGFR (CKD-EPI 2021)"
            );
        }

        double scr = creat.doubleValue();
        double kappa;
        double alpha;
        double genderMultiplier;

        if (gender == Gender.FEMALE) {
            kappa = 0.7;
            alpha = -0.241;
            genderMultiplier = 1.012;
        } else {
            kappa = 0.9;
            alpha = -0.302;
            genderMultiplier = 1.0;
        }

        double scrOverKappa = scr / kappa;
        double minTerm = Math.min(scrOverKappa, 1.0);
        double maxTerm = Math.max(scrOverKappa, 1.0);

        double egfr = 142.0
                * Math.pow(minTerm, alpha)
                * Math.pow(maxTerm, -1.200)
                * Math.pow(0.9938, age)
                * genderMultiplier;

        return BigDecimal.valueOf(egfr).setScale(1, RoundingMode.HALF_UP);
    }

    @Override
    public String getFormulaDescription() {
        return "CKD-EPI 2021 Creatinine Equation (race-free, age >= 18)";
    }

    @Override
    public String getExpectedOutputUnit() {
        return "mL/min/1.73 m²";
    }
}
