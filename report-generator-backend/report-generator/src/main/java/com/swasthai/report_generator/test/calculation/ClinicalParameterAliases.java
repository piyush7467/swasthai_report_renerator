package com.swasthai.report_generator.test.calculation;

import com.swasthai.report_generator.test.entity.CalculationType;

import java.util.*;

/**
 * Standard laboratory parameter codes and their commonly used clinical aliases.
 * Enables robust matching regardless of laboratory naming conventions (e.g. HGB vs HB, PCV vs HCT),
 * as well as strict bidirectional binding between CalculationType formulas and target parameters.
 */
public final class ClinicalParameterAliases {

    private ClinicalParameterAliases() {}

    private static final Map<String, Set<String>> ALIASES_BY_CANONICAL = new HashMap<>();
    private static final Map<String, String> CANONICAL_BY_ALIAS = new HashMap<>();

    private static final Map<CalculationType, String> TARGET_CANONICAL_BY_CALCULATION = new EnumMap<>(CalculationType.class);
    private static final Map<CalculationType, Set<String>> TARGET_ALIASES_BY_CALCULATION = new EnumMap<>(CalculationType.class);
    private static final Map<String, CalculationType> CALCULATION_BY_TARGET_ALIAS = new HashMap<>();

    static {
        // --- INPUT PARAMETERS ---
        registerAliases("HGB", "HGB", "HB", "HEMOGLOBIN", "HAEMOGLOBIN");
        registerAliases("HCT", "HCT", "PCV", "HEMATOCRIT", "HAEMATOCRIT");
        registerAliases("RBC", "RBC", "RBC_COUNT", "ERYTHROCYTES", "TOTAL_RBC");

        registerAliases("NEUT", "NEUT", "NEUTROPHILS", "NEUTROPHIL", "NEUTS", "POLYS", "SEGS");
        registerAliases("LYMPH", "LYMPH", "LYMPHOCYTES", "LYMPHOCYTE", "LYMPHS");
        registerAliases("MONO", "MONO", "MONOCYTES", "MONOCYTE", "MONOS");
        registerAliases("EOS", "EOS", "EOSINOPHILS", "EOSINOPHIL");
        registerAliases("BASO", "BASO", "BASOPHILS", "BASOPHIL");

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

        // --- TARGET CALCULATED PARAMETERS ---
        registerTarget(CalculationType.MCV, "MCV", "MCV", "MEAN_CORPUSCULAR_VOLUME", "MEAN_CELL_VOLUME");
        registerTarget(CalculationType.MCH, "MCH", "MCH", "MEAN_CORPUSCULAR_HEMOGLOBIN", "MEAN_CELL_HEMOGLOBIN", "MEAN_CORPUSCULAR_HAEMOGLOBIN");
        registerTarget(CalculationType.MCHC, "MCHC", "MCHC", "MEAN_CORPUSCULAR_HEMOGLOBIN_CONCENTRATION", "MEAN_CORPUSCULAR_HAEMOGLOBIN_CONCENTRATION", "MEAN_CELL_HEMOGLOBIN_CONCENTRATION");
        registerTarget(CalculationType.VLDL, "VLDL", "VLDL", "VLDL_C", "VLDL_CHOLESTEROL", "SERUM_VLDL");
        registerTarget(CalculationType.LDL_FRIEDEWALD, "LDL", "LDL", "LDL_C", "LDL_CHOLESTEROL", "LDL_FRIEDEWALD", "SERUM_LDL");
        registerTarget(CalculationType.NON_HDL_CHOLESTEROL, "NON_HDL", "NON_HDL", "NON_HDL_C", "NON_HDL_CHOLESTEROL", "NON_HDL_CHOL");
        registerTarget(CalculationType.CHOL_HDL_RATIO, "CHOL_HDL_RATIO", "CHOL_HDL_RATIO", "TC_HDL_RATIO", "CHOLESTEROL_HDL_RATIO", "CHOL_TO_HDL_RATIO", "TC_HDL", "CHOL_HDL");
        registerTarget(CalculationType.LDL_HDL_RATIO, "LDL_HDL_RATIO", "LDL_HDL_RATIO", "LDL_TO_HDL_RATIO", "LDL_HDL");
        registerTarget(CalculationType.INDIRECT_BILIRUBIN, "IBIL", "IBIL", "INDIRECT_BILIRUBIN", "BILIRUBIN_INDIRECT", "I_BIL", "UNCONJUGATED_BILIRUBIN");
        registerTarget(CalculationType.GLOBULIN, "GLOB", "GLOB", "GLOBULIN", "SERUM_GLOBULIN");
        registerTarget(CalculationType.AG_RATIO, "AG_RATIO", "AG_RATIO", "ALB_GLOB_RATIO", "A_G_RATIO", "A_G", "AG", "ALBUMIN_GLOBULIN_RATIO");
        registerTarget(CalculationType.BUN_CREATININE_RATIO, "BUN_CREATININE_RATIO", "BUN_CREATININE_RATIO", "BUN_CREAT_RATIO", "BUN_TO_CREATININE_RATIO", "BUN_CREAT");
        registerTarget(CalculationType.UREA_CREATININE_RATIO, "UREA_CREATININE_RATIO", "UREA_CREATININE_RATIO", "UREA_CREAT_RATIO", "UREA_TO_CREATININE_RATIO", "UREA_CREAT");
        registerTarget(CalculationType.EGFR_CKD_EPI_2021, "EGFR", "EGFR", "E_GFR", "EGFR_CKD_EPI", "EGFR_CKD_EPI_2021", "ESTIMATED_GFR");
        registerTarget(CalculationType.ANION_GAP, "ANION_GAP", "ANION_GAP", "AGAP", "SERUM_ANION_GAP");
        registerTarget(CalculationType.ANION_GAP_K, "ANION_GAP_K", "ANION_GAP_K", "AGAP_K", "ANION_GAP_WITH_K");
    }

    private static void registerAliases(String canonical, String... aliases) {
        Set<String> set = new HashSet<>();
        for (String alias : aliases) {
            String upper = normalizeKey(alias);
            set.add(upper);
            CANONICAL_BY_ALIAS.put(upper, canonical);
        }
        ALIASES_BY_CANONICAL.put(canonical, Collections.unmodifiableSet(set));
    }

    private static void registerTarget(CalculationType type, String canonical, String... aliases) {
        TARGET_CANONICAL_BY_CALCULATION.put(type, canonical);
        Set<String> set = new HashSet<>();
        for (String alias : aliases) {
            String norm = normalizeKey(alias);
            set.add(norm);
            CALCULATION_BY_TARGET_ALIAS.put(norm, type);
        }
        TARGET_ALIASES_BY_CALCULATION.put(type, Collections.unmodifiableSet(set));
    }

    public static String normalizeKey(String code) {
        if (code == null) return "";
        return code.trim()
                .toUpperCase()
                .replace("-", "_")
                .replace(" ", "_")
                .replace("/", "_")
                .replace(":", "_");
    }

    public static Set<String> getAliases(String canonicalCode) {
        if (canonicalCode == null) return Set.of();
        Set<String> aliases = ALIASES_BY_CANONICAL.get(canonicalCode.trim().toUpperCase());
        return aliases != null ? aliases : Set.of(canonicalCode.trim().toUpperCase());
    }

    public static String getCanonical(String code) {
        if (code == null) return null;
        String canonical = CANONICAL_BY_ALIAS.get(normalizeKey(code));
        return canonical != null ? canonical : code.trim().toUpperCase();
    }

    /**
     * Resolves which parameter code from the test's available codes matches the required canonical code.
     */
    public static Optional<String> resolveAvailableCode(String canonicalCode, Set<String> availableCodes) {
        if (canonicalCode == null || availableCodes == null) return Optional.empty();
        Set<String> aliases = getAliases(canonicalCode);
        for (String avail : availableCodes) {
            if (aliases.contains(normalizeKey(avail))) {
                return Optional.of(avail);
            }
        }
        return Optional.empty();
    }

    /**
     * Infers the appropriate CalculationType based strictly on a parameter code or clinical name.
     */
    public static CalculationType inferFromCode(String code) {
        if (code == null || code.isBlank()) return null;
        String norm = normalizeKey(code);
        return CALCULATION_BY_TARGET_ALIAS.get(norm);
    }

    /**
     * Returns true if the given CalculationType formula is clinically compatible with the parameter code.
     */
    public static boolean isCompatible(CalculationType calculationType, String code) {
        if (calculationType == null || calculationType == CalculationType.NONE || code == null || code.isBlank()) {
            return false;
        }
        Set<String> targetAliases = TARGET_ALIASES_BY_CALCULATION.get(calculationType);
        if (targetAliases == null) return false;
        String norm = normalizeKey(code);
        return targetAliases.contains(norm);
    }

    /**
     * Defensively resolves the effective CalculationType for a parameter.
     * Prevents cross-contamination (e.g. MCV formula executing for MCH or MCHC parameters).
     */
    public static CalculationType resolveCalculationType(String parameterCode, CalculationType configuredType) {
        if (parameterCode == null || parameterCode.isBlank()) {
            return configuredType != null ? configuredType : CalculationType.NONE;
        }

        CalculationType inferred = inferFromCode(parameterCode);

        // 1. If configured calculation type is explicitly set
        if (configuredType != null && configuredType != CalculationType.NONE) {
            // If the parameter code canonically belongs to a DIFFERENT clinical calculation,
            // prevent collision and preserve parameter identity!
            if (inferred != null && inferred != configuredType) {
                return inferred;
            }
            return configuredType;
        }

        // 2. Configured type is NONE or null, but parameter code corresponds to a known calculation
        return inferred != null ? inferred : CalculationType.NONE;
    }
}
