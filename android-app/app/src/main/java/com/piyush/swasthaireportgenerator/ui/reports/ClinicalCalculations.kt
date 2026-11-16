package com.piyush.swasthaireportgenerator.ui.reports

import com.piyush.swasthaireportgenerator.data.model.ReportParameterResultResponse
import com.piyush.swasthaireportgenerator.data.model.ReportResponse
import com.piyush.swasthaireportgenerator.data.model.ReportTestItemResponse
import java.util.Locale

data class CbcDifferentialValidation(
    val isValid: Boolean,
    val hasDifferential: Boolean,
    val missingParams: List<String> = emptyList(),
    val sum: Double,
    val mono: Double?,
    val isNegativeMono: Boolean,
    val errorMessage: String? = null
)

/**
 * Clinical calculation engine matching backend ClinicalParameterCalculators and web clinicalCalculations.ts.
 * Computes laboratory formulas in real time as the user enters parameter values.
 */
object ClinicalCalculations {

    private val PARAMETER_ALIASES: Map<String, List<String>> = mapOf(
        "HGB" to listOf("HGB", "HB", "HEMOGLOBIN", "HAEMOGLOBIN"),
        "HCT" to listOf("HCT", "PCV", "HEMATOCRIT", "HAEMATOCRIT"),
        "RBC" to listOf("RBC", "RBC_COUNT", "ERYTHROCYTES", "TOTAL_RBC"),
        "NEUT" to listOf("NEUT", "NEUTROPHILS", "NEUTROPHIL", "POLYMORPHS", "SEGS"),
        "LYMPH" to listOf("LYMPH", "LYMPHOCYTES", "LYMPHOCYTE"),
        "EOS" to listOf("EOS", "EOSINOPHILS", "EOSINOPHIL"),
        "MONO" to listOf("MONO", "MONOCYTES", "MONOCYTE"),
        "BASO" to listOf("BASO", "BASOPHILS", "BASOPHIL"),
        "TC" to listOf("TC", "CHOLESTEROL", "TOTAL_CHOLESTEROL", "CHOL", "SERUM_CHOLESTEROL"),
        "TG" to listOf("TG", "TRIGLYCERIDES", "TRIGLYCERIDE", "TRIG", "SERUM_TRIGLYCERIDES"),
        "HDL" to listOf("HDL", "HDL_C", "HDL_CHOLESTEROL", "SERUM_HDL"),
        "LDL" to listOf("LDL", "LDL_C", "LDL_CHOLESTEROL", "LDL_FRIEDEWALD"),
        "VLDL" to listOf("VLDL", "VLDL_C", "VLDL_CHOLESTEROL"),
        "TBIL" to listOf("TBIL", "TOTAL_BILIRUBIN", "BILIRUBIN_TOTAL", "T_BIL", "T_BILIRUBIN", "SERUM_BILIRUBIN_TOTAL"),
        "DBIL" to listOf("DBIL", "DIRECT_BILIRUBIN", "BILIRUBIN_DIRECT", "D_BIL", "D_BILIRUBIN", "CONJUGATED_BILIRUBIN"),
        "IBIL" to listOf("IBIL", "INDIRECT_BILIRUBIN", "BILIRUBIN_INDIRECT", "I_BIL", "UNCONJUGATED_BILIRUBIN"),
        "TP" to listOf("TP", "TOTAL_PROTEIN", "PROTEIN_TOTAL", "PROTEIN", "SERUM_PROTEIN"),
        "ALB" to listOf("ALB", "ALBUMIN", "SERUM_ALBUMIN"),
        "GLOB" to listOf("GLOB", "GLOBULIN", "SERUM_GLOBULIN"),
        "BUN" to listOf("BUN", "BLOOD_UREA_NITROGEN"),
        "UREA" to listOf("UREA", "BLOOD_UREA", "SERUM_UREA"),
        "CREAT" to listOf("CREAT", "CREATININE", "SERUM_CREATININE", "S_CREATININE"),
        "NA" to listOf("NA", "SODIUM", "SERUM_SODIUM"),
        "K" to listOf("K", "POTASSIUM", "SERUM_POTASSIUM"),
        "CL" to listOf("CL", "CHLORIDE", "SERUM_CHLORIDE"),
        "HCO3" to listOf("HCO3", "BICARBONATE", "SERUM_BICARBONATE", "CO2", "TOTAL_CO2")
    )

    private val ALIAS_TO_CANONICAL: Map<String, String> by lazy {
        val map = mutableMapOf<String, String>()
        for ((canonical, aliases) in PARAMETER_ALIASES) {
            for (alias in aliases) {
                map[alias.uppercase()] = canonical
            }
        }
        map
    }

    fun normalizeCode(code: String?): String {
        if (code.isNullOrBlank()) return ""
        return code.trim().uppercase().replace(Regex("[-\\s/:]+"), "_")
    }

    fun getCanonicalCode(code: String?): String {
        if (code.isNullOrBlank()) return ""
        val norm = normalizeCode(code)
        return ALIAS_TO_CANONICAL[norm] ?: norm
    }

    fun isCalculatedParameter(param: ReportParameterResultResponse, testCode: String? = null): Boolean {
        if (param.inputType.equals("CALCULATED", ignoreCase = true)) return true
        val norm = normalizeCode(param.parameterCode)
        val isCbc = testCode?.trim()?.uppercase() == "CBC"
        if (isCbc && (norm == "MONO" || norm == "BASO")) return true
        return norm in setOf(
            "MCV", "MCH", "MCHC", "VLDL", "LDL", "LDL_FRIEDEWALD", "NON_HDL",
            "NON_HDL_CHOLESTEROL", "CHOL_HDL_RATIO", "LDL_HDL_RATIO",
            "IBIL", "INDIRECT_BILIRUBIN", "GLOB", "GLOBULIN", "AG_RATIO",
            "BUN_CREATININE_RATIO", "UREA_CREATININE_RATIO", "ANION_GAP", "ANION_GAP_K",
            "MONO", "BASO"
        )
    }

    fun getRequiredInputsHint(param: ReportParameterResultResponse): String {
        val norm = normalizeCode(param.parameterCode)
        val nameNorm = param.parameterName.uppercase()
        return when {
            norm == "MCV" || nameNorm.contains("CORPUSCULAR VOLUME") -> "Requires HCT, RBC"
            norm == "MCH" || (nameNorm.contains("CORPUSCULAR HEMOGLOBIN") && !nameNorm.contains("CONCENTRATION")) -> "Requires HGB, RBC"
            norm == "MCHC" || nameNorm.contains("CONCENTRATION") -> "Requires HGB, HCT"
            norm == "MONO" || nameNorm.contains("MONOCYTE") -> "Requires NEUT, LYMPH, EOS"
            norm == "BASO" || nameNorm.contains("BASOPHIL") -> "Auto set (0%)"
            norm == "VLDL" -> "Requires TG"
            norm.contains("LDL") && !norm.contains("RATIO") -> "Requires TC, HDL, TG"
            norm.contains("NON_HDL") -> "Requires TC, HDL"
            norm.contains("CHOL_HDL") -> "Requires TC, HDL"
            norm.contains("LDL_HDL") -> "Requires LDL, HDL"
            norm == "IBIL" || nameNorm.contains("INDIRECT BILIRUBIN") -> "Requires Total & Direct Bilirubin"
            norm == "GLOB" || nameNorm == "GLOBULIN" -> "Requires Total Protein, Albumin"
            norm.contains("AG_RATIO") || nameNorm.contains("A/G") -> "Requires Albumin, Globulin"
            norm.contains("BUN_CREAT") -> "Requires BUN, Creatinine"
            norm.contains("UREA_CREAT") -> "Requires Urea, Creatinine"
            norm.contains("ANION_GAP") -> "Requires Na, Cl, HCO3"
            else -> "Auto calculated"
        }
    }

    private fun resolveNumericValue(
        canonicalReq: String,
        parameters: List<ReportParameterResultResponse>,
        valuesMap: Map<String, String>
    ): Double? {
        val param = parameters.firstOrNull {
            getCanonicalCode(it.parameterCode) == canonicalReq ||
            getCanonicalCode(it.parameterName) == canonicalReq
        } ?: return null

        val strVal = valuesMap[param.parameterRefId]?.takeIf { it.isNotBlank() }
            ?: param.value?.takeIf { it.isNotBlank() }
            ?: return null

        return strVal.toDoubleOrNull()
    }

    fun formatNumber(value: Double): String {
        return if (value.isNaN() || value.isInfinite()) {
            ""
        } else if (value % 1.0 == 0.0) {
            value.toInt().toString()
        } else {
            String.format(Locale.US, "%.4f", value).trimEnd('0').trimEnd('.')
        }
    }

    /**
     * Dynamically calculates value for a specific parameter if its inputs exist in valuesMap.
     */
    fun calculateParameterValue(
        param: ReportParameterResultResponse,
        allParameters: List<ReportParameterResultResponse>,
        valuesMap: Map<String, String>
    ): String? {
        val norm = normalizeCode(param.parameterCode)
        val nameNorm = param.parameterName.uppercase()

        when {
            // MCV = (HCT * 10) / RBC
            norm == "MCV" || nameNorm.contains("CORPUSCULAR VOLUME") -> {
                val hct = resolveNumericValue("HCT", allParameters, valuesMap) ?: return null
                val rbc = resolveNumericValue("RBC", allParameters, valuesMap) ?: return null
                if (hct <= 0 || rbc <= 0) return null
                return formatNumber((hct * 10.0) / rbc)
            }

            // MCHC = (HGB * 100) / HCT
            norm == "MCHC" || nameNorm.contains("CONCENTRATION") -> {
                val hgb = resolveNumericValue("HGB", allParameters, valuesMap) ?: return null
                val hct = resolveNumericValue("HCT", allParameters, valuesMap) ?: return null
                if (hgb <= 0 || hct <= 0) return null
                return formatNumber((hgb * 100.0) / hct)
            }

            // MCH = (HGB * 10) / RBC
            norm == "MCH" || nameNorm.contains("CORPUSCULAR HEMOGLOBIN") -> {
                val hgb = resolveNumericValue("HGB", allParameters, valuesMap) ?: return null
                val rbc = resolveNumericValue("RBC", allParameters, valuesMap) ?: return null
                if (hgb <= 0 || rbc <= 0) return null
                return formatNumber((hgb * 10.0) / rbc)
            }

            // VLDL = TG / 5
            norm == "VLDL" || nameNorm.contains("VLDL") -> {
                val tg = resolveNumericValue("TG", allParameters, valuesMap) ?: return null
                if (tg < 0 || tg >= 400) return null
                return formatNumber(tg / 5.0)
            }

            // LDL Friedewald = TC - HDL - (TG / 5)
            norm == "LDL" || norm == "LDL_FRIEDEWALD" || (nameNorm.contains("LDL") && !nameNorm.contains("RATIO")) -> {
                val tc = resolveNumericValue("TC", allParameters, valuesMap) ?: return null
                val hdl = resolveNumericValue("HDL", allParameters, valuesMap) ?: return null
                val tg = resolveNumericValue("TG", allParameters, valuesMap) ?: return null
                if (tg < 0 || tg >= 400 || tc < 0 || hdl < 0) return null
                val ldl = tc - hdl - (tg / 5.0)
                if (ldl < 0) return null
                return formatNumber(ldl)
            }

            // Non-HDL = TC - HDL
            norm == "NON_HDL" || norm == "NON_HDL_CHOLESTEROL" || nameNorm.contains("NON-HDL") -> {
                val tc = resolveNumericValue("TC", allParameters, valuesMap) ?: return null
                val hdl = resolveNumericValue("HDL", allParameters, valuesMap) ?: return null
                if (tc < hdl || tc < 0 || hdl < 0) return null
                return formatNumber(tc - hdl)
            }

            // TC / HDL Ratio
            norm == "CHOL_HDL_RATIO" || nameNorm.contains("CHOLESTEROL / HDL") || nameNorm.contains("CHOL/HDL") -> {
                val tc = resolveNumericValue("TC", allParameters, valuesMap) ?: return null
                val hdl = resolveNumericValue("HDL", allParameters, valuesMap) ?: return null
                if (hdl <= 0 || tc < 0) return null
                return formatNumber(tc / hdl)
            }

            // LDL / HDL Ratio
            norm == "LDL_HDL_RATIO" || nameNorm.contains("LDL / HDL") || nameNorm.contains("LDL/HDL") -> {
                val ldl = resolveNumericValue("LDL", allParameters, valuesMap) ?: return null
                val hdl = resolveNumericValue("HDL", allParameters, valuesMap) ?: return null
                if (hdl <= 0 || ldl < 0) return null
                return formatNumber(ldl / hdl)
            }

            // Indirect Bilirubin = Total - Direct
            norm == "IBIL" || norm == "INDIRECT_BILIRUBIN" || nameNorm.contains("INDIRECT BILIRUBIN") -> {
                val tbil = resolveNumericValue("TBIL", allParameters, valuesMap) ?: return null
                val dbil = resolveNumericValue("DBIL", allParameters, valuesMap) ?: return null
                if (tbil < dbil) return null
                return formatNumber(tbil - dbil)
            }

            // Globulin = Total Protein - Albumin
            norm == "GLOB" || norm == "GLOBULIN" || nameNorm == "GLOBULIN" -> {
                val tp = resolveNumericValue("TP", allParameters, valuesMap) ?: return null
                val alb = resolveNumericValue("ALB", allParameters, valuesMap) ?: return null
                if (tp < alb) return null
                return formatNumber(tp - alb)
            }

            // A/G Ratio = Albumin / Globulin
            norm == "AG_RATIO" || nameNorm.contains("A/G") || nameNorm.contains("ALBUMIN / GLOBULIN") -> {
                val alb = resolveNumericValue("ALB", allParameters, valuesMap) ?: return null
                val glob = resolveNumericValue("GLOB", allParameters, valuesMap) ?: return null
                if (glob <= 0 || alb <= 0) return null
                return formatNumber(alb / glob)
            }

            // BUN / Creatinine Ratio
            norm == "BUN_CREATININE_RATIO" || nameNorm.contains("BUN / CREATININE") -> {
                val bun = resolveNumericValue("BUN", allParameters, valuesMap) ?: return null
                val creat = resolveNumericValue("CREAT", allParameters, valuesMap) ?: return null
                if (creat <= 0 || bun <= 0) return null
                return formatNumber(bun / creat)
            }

            // Urea / Creatinine Ratio
            norm == "UREA_CREATININE_RATIO" || nameNorm.contains("UREA / CREATININE") -> {
                val urea = resolveNumericValue("UREA", allParameters, valuesMap) ?: return null
                val creat = resolveNumericValue("CREAT", allParameters, valuesMap) ?: return null
                if (creat <= 0 || urea <= 0) return null
                return formatNumber(urea / creat)
            }

            // Anion Gap = Na - (Cl + HCO3)
            norm == "ANION_GAP" || nameNorm.contains("ANION GAP") -> {
                val na = resolveNumericValue("NA", allParameters, valuesMap) ?: return null
                val cl = resolveNumericValue("CL", allParameters, valuesMap) ?: return null
                val hco3 = resolveNumericValue("HCO3", allParameters, valuesMap) ?: return null
                return formatNumber(na - (cl + hco3))
            }

            // Monocytes = 100 - (NEUT + LYMPH + EOS + BASO)
            norm == "MONO" || nameNorm.contains("MONOCYTE") -> {
                val neut = resolveNumericValue("NEUT", allParameters, valuesMap) ?: return null
                val lymph = resolveNumericValue("LYMPH", allParameters, valuesMap) ?: return null
                val eos = resolveNumericValue("EOS", allParameters, valuesMap) ?: return null
                val baso = resolveNumericValue("BASO", allParameters, valuesMap) ?: 0.0
                val sum = neut + lymph + eos + baso
                val mono = 100.0 - sum
                return formatNumber(mono)
            }

            // Basophils in CBC profile defaults to 0%
            norm == "BASO" || nameNorm.contains("BASOPHIL") -> {
                return "0"
            }

            else -> return null
        }
    }

    /**
     * Validates CBC differential leukocyte count inputs according to laboratory and backend rules.
     */
    fun validateCbcDifferential(
        test: ReportTestItemResponse,
        valuesMap: Map<String, String>
    ): CbcDifferentialValidation? {
        if (!test.testCode.trim().equals("CBC", ignoreCase = true)) return null

        val neutParam = test.parameters.firstOrNull { getCanonicalCode(it.parameterCode) == "NEUT" }
        val lymphParam = test.parameters.firstOrNull { getCanonicalCode(it.parameterCode) == "LYMPH" }
        val eosParam = test.parameters.firstOrNull { getCanonicalCode(it.parameterCode) == "EOS" }

        val neutStr = neutParam?.let { valuesMap[it.parameterRefId]?.takeIf { s -> s.isNotBlank() } ?: it.value?.takeIf { s -> s.isNotBlank() } } ?: ""
        val lymphStr = lymphParam?.let { valuesMap[it.parameterRefId]?.takeIf { s -> s.isNotBlank() } ?: it.value?.takeIf { s -> s.isNotBlank() } } ?: ""
        val eosStr = eosParam?.let { valuesMap[it.parameterRefId]?.takeIf { s -> s.isNotBlank() } ?: it.value?.takeIf { s -> s.isNotBlank() } } ?: ""

        val hasAny = neutStr.isNotBlank() || lymphStr.isNotBlank() || eosStr.isNotBlank()
        if (!hasAny) {
            return CbcDifferentialValidation(
                isValid = true,
                hasDifferential = false,
                missingParams = emptyList(),
                sum = 0.0,
                mono = null,
                isNegativeMono = false
            )
        }

        val missing = mutableListOf<String>()
        if (neutStr.isBlank()) missing.add("NEUT")
        if (lymphStr.isBlank()) missing.add("LYMPH")
        if (eosStr.isBlank()) missing.add("EOS")

        val neut = neutStr.toDoubleOrNull()
        val lymph = lymphStr.toDoubleOrNull()
        val eos = eosStr.toDoubleOrNull()

        if (missing.isNotEmpty()) {
            val partialSum = (neut ?: 0.0) + (lymph ?: 0.0) + (eos ?: 0.0)
            return CbcDifferentialValidation(
                isValid = false,
                hasDifferential = true,
                missingParams = missing,
                sum = partialSum,
                mono = null,
                isNegativeMono = false,
                errorMessage = "CBC Differential requires Neutrophils (NEUT), Lymphocytes (LYMPH), and Eosinophils (EOS). Missing: ${missing.joinToString(", ")}"
            )
        }

        if (neut == null || lymph == null || eos == null) {
            return CbcDifferentialValidation(
                isValid = false,
                hasDifferential = true,
                sum = 0.0,
                mono = null,
                isNegativeMono = false,
                errorMessage = "Differential percentages must be valid numbers."
            )
        }

        if (neut < 0 || neut > 100 || lymph < 0 || lymph > 100 || eos < 0 || eos > 100) {
            return CbcDifferentialValidation(
                isValid = false,
                hasDifferential = true,
                sum = neut + lymph + eos,
                mono = null,
                isNegativeMono = false,
                errorMessage = "Differential percentages must each be between 0% and 100%."
            )
        }

        val sum = neut + lymph + eos
        val mono = 100.0 - sum
        val isNegative = mono < 0.0

        if (isNegative) {
            return CbcDifferentialValidation(
                isValid = false,
                hasDifferential = true,
                sum = sum,
                mono = mono,
                isNegativeMono = true,
                errorMessage = "Calculated Monocytes is negative (${formatNumber(mono)}%). Sum of differential percentages (${formatNumber(sum)}%) exceeds 100.0%."
            )
        }

        return CbcDifferentialValidation(
            isValid = true,
            hasDifferential = true,
            sum = sum,
            mono = mono,
            isNegativeMono = false
        )
    }

    /**
     * Recomputes all calculated parameters for a report and returns an updated map of parameter values.
     */
    fun computeAllCalculations(report: ReportResponse, currentValues: Map<String, String>): Map<String, String> {
        val allParams = report.tests.flatMap { it.parameters }
        val updatedMap = currentValues.toMutableMap()

        // Two passes to handle dependencies (e.g. Globulin before A/G Ratio)
        for (pass in 1..2) {
            for (param in allParams) {
                if (isCalculatedParameter(param)) {
                    val calcVal = calculateParameterValue(param, allParams, updatedMap)
                    if (!calcVal.isNullOrBlank()) {
                        updatedMap[param.parameterRefId] = calcVal
                    }
                }
            }
        }

        return updatedMap
    }
}

