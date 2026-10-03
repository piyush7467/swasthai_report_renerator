package com.swasthai.report_generator.test.calculation;

import java.util.*;

/**
 * Standard laboratory parameter codes and their commonly used clinical aliases.
 * Enables robust matching regardless of laboratory naming conventions (e.g. HGB vs HB, PCV vs HCT).
 */
public final class ClinicalParameterAliases {

    private ClinicalParameterAliases() {}

    private static final Map<String, Set<String>> ALIASES_BY_CANONICAL = new HashMap<>();
    private static final Map<String, String> CANONICAL_BY_ALIAS = new HashMap<>();

    static {
        registerAliases("HGB", "HGB", "HB", "HEMOGLOBIN", "HAEMOGLOBIN");
        registerAliases("HCT", "HCT", "PCV", "HEMATOCRIT", "HAEMATOCRIT");
        registerAliases("RBC", "RBC", "RBC_COUNT", "ERYTHROCYTES", "TOTAL_RBC");

        registerAliases("TC", "TC", "CHOLESTEROL", "TOTAL_CHOLESTEROL", "CHOL", "SERUM_CHOLESTEROL");
        registerAliases("TG", "TG", "TRIGLYCERIDES", "TRIGLYCERIDE", "TRIG", "SERUM_TRIGLYCERIDES");
        registerAliases("HDL", "HDL", "HDL_C", "HDL_CHOLESTEROL", "SERUM_HDL");
        registerAliases("LDL", "LDL", "LDL_C", "LDL_CHOLESTEROL", "LDL_FRIEDEWALD");
        registerAliases("VLDL", "VLDL", "VLDL_C", "VLDL_CHOLESTEROL");

        registerAliases("TBIL", "TBIL", "TOTAL_BILIRUBIN", "BILIRUBIN_TOTAL", "T_BIL", "T_BILIRUBIN", "SERUM_BILIRUBIN_TOTAL");
        registerAliases("DBIL", "DBIL", "DIRECT_BILIRUBIN", "BILIRUBIN_DIRECT", "D_BIL", "D_BILIRUBIN", "CONJUGATED_BILIRUBIN");
        registerAliases("IBIL", "IBIL", "INDIRECT_BILIRUBIN", "BILIRUBIN_INDIRECT", "I_BIL", "UNCONJUGATED_BILIRUBIN");
        registerAliases("TP", "TP", "TOTAL_PROTEIN", "PROTEIN_TOTAL", "PROTEIN", "SERUM_PROTEIN");
        registerAliases("ALB", "ALB", "ALBUMIN", "SERUM_ALBUMIN");
        registerAliases("GLOB", "GLOB", "GLOBULIN", "SERUM_GLOBULIN");

        registerAliases("BUN", "BUN", "BLOOD_UREA_NITROGEN");
        registerAliases("UREA", "UREA", "BLOOD_UREA", "SERUM_UREA");
        registerAliases("CREAT", "CREAT", "CREATININE", "SERUM_CREATININE", "S_CREATININE");

        registerAliases("NA", "NA", "SODIUM", "SERUM_SODIUM");
        registerAliases("K", "K", "POTASSIUM", "SERUM_POTASSIUM");
        registerAliases("CL", "CL", "CHLORIDE", "SERUM_CHLORIDE");
        registerAliases("HCO3", "HCO3", "BICARBONATE", "SERUM_BICARBONATE", "CO2", "TOTAL_CO2");
    }

    private static void registerAliases(String canonical, String... aliases) {
        Set<String> set = new HashSet<>();
        for (String alias : aliases) {
            String upper = alias.trim().toUpperCase();
            set.add(upper);
            CANONICAL_BY_ALIAS.put(upper, canonical);
        }
        ALIASES_BY_CANONICAL.put(canonical, Collections.unmodifiableSet(set));
    }

    public static Set<String> getAliases(String canonicalCode) {
        if (canonicalCode == null) return Set.of();
        Set<String> aliases = ALIASES_BY_CANONICAL.get(canonicalCode.trim().toUpperCase());
        return aliases != null ? aliases : Set.of(canonicalCode.trim().toUpperCase());
    }

    public static String getCanonical(String code) {
        if (code == null) return null;
        String canonical = CANONICAL_BY_ALIAS.get(code.trim().toUpperCase());
        return canonical != null ? canonical : code.trim().toUpperCase();
    }

    /**
     * Resolves which parameter code from the test's available codes matches the required canonical code.
     */
    public static Optional<String> resolveAvailableCode(String canonicalCode, Set<String> availableCodes) {
        if (canonicalCode == null || availableCodes == null) return Optional.empty();
        Set<String> aliases = getAliases(canonicalCode);
        for (String avail : availableCodes) {
            if (aliases.contains(avail.trim().toUpperCase())) {
                return Optional.of(avail);
            }
        }
        return Optional.empty();
    }
}
