package com.swasthai.report_generator.test.calculation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;

/**
 * Validates clinical units and performs verified standard conversions when required.
 */
public final class ClinicalUnitValidator {

    private ClinicalUnitValidator() {}

    private static final Set<String> MG_DL_UNITS = Set.of("MG/DL", "MG/100ML", "MG%", "MGDL");
    private static final Set<String> MMOL_L_UNITS = Set.of("MMOL/L", "MMOLL");
    private static final Set<String> G_DL_UNITS = Set.of("G/DL", "GM/DL", "GM%", "G/100ML", "GDL");
    private static final Set<String> G_L_UNITS = Set.of("G/L", "GM/L", "GL");

    /**
     * Normalizes a unit string.
     */
    public static String normalizeUnit(String unit) {
        if (unit == null) return "";
        return unit.trim().toUpperCase().replace(" ", "");
    }

    /**
     * Validates and converts lipid parameters (TC, TG, HDL, etc.) to mg/dL.
     * Standard laboratory reference in this system is mg/dL.
     */
    public static BigDecimal normalizeLipidToMgDl(String parameterCode, BigDecimal value, String unit) {
        if (value == null) return null;
        if (unit == null || unit.isBlank()) return value; // Unspecified defaults to configured standard

        String norm = normalizeUnit(unit);
        if (MG_DL_UNITS.contains(norm)) {
            return value;
        }

        if (MMOL_L_UNITS.contains(norm)) {
            String canon = ClinicalParameterAliases.getCanonical(parameterCode);
            if ("TG".equalsIgnoreCase(canon)) {
                // 1 mmol/L Triglycerides = 88.57 mg/dL
                return value.multiply(new BigDecimal("88.57")).setScale(4, RoundingMode.HALF_UP);
            } else {
                // 1 mmol/L Cholesterol = 38.67 mg/dL
                return value.multiply(new BigDecimal("38.67")).setScale(4, RoundingMode.HALF_UP);
            }
        }

        throw new CalculationException(
                "Unsupported unit '" + unit + "' for lipid parameter " + parameterCode
                        + ". Expected mg/dL or mmol/L."
        );
    }

    /**
     * Validates and converts serum creatinine to mg/dL.
     */
    public static BigDecimal normalizeCreatinineToMgDl(BigDecimal value, String unit) {
        if (value == null) return null;
        if (unit == null || unit.isBlank()) return value;

        String norm = normalizeUnit(unit);
        if (MG_DL_UNITS.contains(norm)) {
            return value;
        }

        // Handle µmol/L, umol/L, micromol/L regardless of source file character encoding
        if (norm.endsWith("MOL/L") || norm.endsWith("MOLL")) {
            if (norm.startsWith("U") || norm.startsWith("MC") || norm.startsWith("MICRO") || norm.contains("MOL")) {
                // 1 mg/dL = 88.4 µmol/L => µmol/L / 88.4 = mg/dL
                return value.divide(new BigDecimal("88.4"), 4, RoundingMode.HALF_UP);
            }
        }

        throw new CalculationException(
                "Unsupported unit '" + unit + "' for creatinine. Expected mg/dL or µmol/L."
        );
    }

    /**
     * Validates and converts protein / albumin / hemoglobin to g/dL.
     */
    public static BigDecimal normalizeProteinToGDl(String parameterCode, BigDecimal value, String unit) {
        if (value == null) return null;
        if (unit == null || unit.isBlank()) return value;

        String norm = normalizeUnit(unit);
        if (G_DL_UNITS.contains(norm)) {
            return value;
        }

        if (G_L_UNITS.contains(norm)) {
            // 1 g/dL = 10 g/L => g/L / 10 = g/dL
            return value.divide(BigDecimal.TEN, 4, RoundingMode.HALF_UP);
        }

        throw new CalculationException(
                "Unsupported unit '" + unit + "' for " + parameterCode + ". Expected g/dL or g/L."
        );
    }

    /**
     * Validates and converts PCV / Hematocrit to percentage.
     */
    public static BigDecimal normalizeHematocritToPercent(BigDecimal value, String unit) {
        if (value == null) return null;
        // If between 0 and 1 exclusive (fraction), convert to percentage
        if (value.compareTo(BigDecimal.ZERO) > 0 && value.compareTo(BigDecimal.ONE) < 0) {
            return value.multiply(new BigDecimal("100"));
        }
        return value;
    }
}
