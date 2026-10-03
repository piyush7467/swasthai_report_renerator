import type {
  CalculationType,
  ReportParameterItemResponse,
  ReportTestItemResponse,
} from "../types/reportTypes";

export interface PatientDemographics {
  gender?: string | null;
  ageValue?: number | null;
  ageUnit?: string | null;
  dateOfBirth?: string | null;
}

/**
 * Standard clinical parameter codes and aliases matching backend ClinicalParameterAliases.
 */
const PARAMETER_ALIASES: Record<string, string[]> = {
  HGB: ["HGB", "HB", "HEMOGLOBIN", "HAEMOGLOBIN"],
  HCT: ["HCT", "PCV", "HEMATOCRIT", "HAEMATOCRIT"],
  RBC: ["RBC", "RBC_COUNT", "ERYTHROCYTES", "TOTAL_RBC"],

  TC: ["TC", "CHOLESTEROL", "TOTAL_CHOLESTEROL", "CHOL", "SERUM_CHOLESTEROL"],
  TG: ["TG", "TRIGLYCERIDES", "TRIGLYCERIDE", "TRIG", "SERUM_TRIGLYCERIDES"],
  HDL: ["HDL", "HDL_C", "HDL_CHOLESTEROL", "SERUM_HDL"],
  LDL: ["LDL", "LDL_C", "LDL_CHOLESTEROL", "LDL_FRIEDEWALD"],
  VLDL: ["VLDL", "VLDL_C", "VLDL_CHOLESTEROL"],

  TBIL: [
    "TBIL",
    "TOTAL_BILIRUBIN",
    "BILIRUBIN_TOTAL",
    "T_BIL",
    "T_BILIRUBIN",
    "SERUM_BILIRUBIN_TOTAL",
  ],
  DBIL: [
    "DBIL",
    "DIRECT_BILIRUBIN",
    "BILIRUBIN_DIRECT",
    "D_BIL",
    "D_BILIRUBIN",
    "CONJUGATED_BILIRUBIN",
  ],
  IBIL: [
    "IBIL",
    "INDIRECT_BILIRUBIN",
    "BILIRUBIN_INDIRECT",
    "I_BIL",
    "UNCONJUGATED_BILIRUBIN",
  ],
  TP: ["TP", "TOTAL_PROTEIN", "PROTEIN_TOTAL", "PROTEIN", "SERUM_PROTEIN"],
  ALB: ["ALB", "ALBUMIN", "SERUM_ALBUMIN"],
  GLOB: ["GLOB", "GLOBULIN", "SERUM_GLOBULIN"],

  BUN: ["BUN", "BLOOD_UREA_NITROGEN"],
  UREA: ["UREA", "BLOOD_UREA", "SERUM_UREA"],
  CREAT: ["CREAT", "CREATININE", "SERUM_CREATININE", "S_CREATININE"],

  NA: ["NA", "SODIUM", "SERUM_SODIUM"],
  K: ["K", "POTASSIUM", "SERUM_POTASSIUM"],
  CL: ["CL", "CHLORIDE", "SERUM_CHLORIDE"],
  HCO3: ["HCO3", "BICARBONATE", "SERUM_BICARBONATE", "CO2", "TOTAL_CO2"],
};

// Build reverse lookup: Alias -> Canonical Code
const ALIAS_TO_CANONICAL: Record<string, string> = {};
for (const [canonical, aliases] of Object.entries(PARAMETER_ALIASES)) {
  for (const alias of aliases) {
    ALIAS_TO_CANONICAL[alias.toUpperCase()] = canonical;
  }
}

/**
 * Normalizes parameter codes for reliable clinical comparison (removes spaces, hyphens, slashes).
 */
export function normalizeCode(code?: string | null): string {
  if (!code) return "";
  return code
    .trim()
    .toUpperCase()
    .replace(/[-\s/:]+/g, "_");
}

/**
 * Target output parameters and aliases associated with each CalculationType.
 * Prevents cross-contamination (e.g. MCV executing for MCH or MCHC).
 */
export const TARGET_ALIASES_BY_CALCULATION: Record<CalculationType, string[]> = {
  NONE: [],
  MCV: ["MCV", "MEAN_CORPUSCULAR_VOLUME", "MEAN_CELL_VOLUME"],
  MCH: ["MCH", "MEAN_CORPUSCULAR_HEMOGLOBIN", "MEAN_CELL_HEMOGLOBIN", "MEAN_CORPUSCULAR_HAEMOGLOBIN"],
  MCHC: [
    "MCHC",
    "MEAN_CORPUSCULAR_HEMOGLOBIN_CONCENTRATION",
    "MEAN_CORPUSCULAR_HAEMOGLOBIN_CONCENTRATION",
    "MEAN_CELL_HEMOGLOBIN_CONCENTRATION",
  ],
  VLDL: ["VLDL", "VLDL_C", "VLDL_CHOLESTEROL", "SERUM_VLDL"],
  LDL_FRIEDEWALD: ["LDL", "LDL_C", "LDL_CHOLESTEROL", "LDL_FRIEDEWALD", "SERUM_LDL"],
  NON_HDL_CHOLESTEROL: ["NON_HDL", "NON_HDL_C", "NON_HDL_CHOLESTEROL", "NON_HDL_CHOL"],
  CHOL_HDL_RATIO: [
    "CHOL_HDL_RATIO",
    "TC_HDL_RATIO",
    "CHOLESTEROL_HDL_RATIO",
    "CHOL_TO_HDL_RATIO",
    "TC_HDL",
    "CHOL_HDL",
  ],
  LDL_HDL_RATIO: ["LDL_HDL_RATIO", "LDL_TO_HDL_RATIO", "LDL_HDL"],
  INDIRECT_BILIRUBIN: ["IBIL", "INDIRECT_BILIRUBIN", "BILIRUBIN_INDIRECT", "I_BIL", "UNCONJUGATED_BILIRUBIN"],
  GLOBULIN: ["GLOB", "GLOBULIN", "SERUM_GLOBULIN"],
  AG_RATIO: ["AG_RATIO", "ALB_GLOB_RATIO", "A_G_RATIO", "A_G", "AG", "ALBUMIN_GLOBULIN_RATIO"],
  BUN_CREATININE_RATIO: ["BUN_CREATININE_RATIO", "BUN_CREAT_RATIO", "BUN_TO_CREATININE_RATIO", "BUN_CREAT"],
  UREA_CREATININE_RATIO: ["UREA_CREATININE_RATIO", "UREA_CREAT_RATIO", "UREA_TO_CREATININE_RATIO", "UREA_CREAT"],
  EGFR_CKD_EPI_2021: ["EGFR", "E_GFR", "EGFR_CKD_EPI", "EGFR_CKD_EPI_2021", "ESTIMATED_GFR"],
  ANION_GAP: ["ANION_GAP", "AGAP", "SERUM_ANION_GAP"],
  ANION_GAP_K: ["ANION_GAP_K", "AGAP_K", "ANION_GAP_WITH_K"],
};

// Build reverse lookup: Normalized Alias -> CalculationType
const CALCULATION_BY_TARGET_ALIAS: Record<string, CalculationType> = {};
for (const [calcType, aliases] of Object.entries(TARGET_ALIASES_BY_CALCULATION)) {
  for (const alias of aliases) {
    CALCULATION_BY_TARGET_ALIAS[normalizeCode(alias)] = calcType as CalculationType;
  }
}

/**
 * Infers the clinical CalculationType from a parameter's code or label.
 */
export function inferCalculationTypeFromCode(code?: string | null): CalculationType | null {
  if (!code) return null;
  const norm = normalizeCode(code);
  return CALCULATION_BY_TARGET_ALIAS[norm] || null;
}

/**
 * Returns true if the CalculationType is clinically compatible with the parameter code.
 */
export function isCalculationCompatible(calcType: CalculationType, code?: string | null): boolean {
  if (!calcType || calcType === "NONE" || !code) return false;
  const aliases = TARGET_ALIASES_BY_CALCULATION[calcType];
  if (!aliases) return false;
  const norm = normalizeCode(code);
  return aliases.some((a) => normalizeCode(a) === norm);
}

/**
 * Defensively resolves the effective CalculationType for a parameter.
 * Strictly prevents cross-contamination (e.g. MCV executing for MCH or MCHC parameters).
 */
export function resolveEffectiveCalculationType(param: {
  parameterCode?: string | null;
  inputType?: string | null;
  calculationType?: CalculationType | null;
}): CalculationType {
  const code = param.parameterCode;
  const configured = param.calculationType;
  const isCalculatedMode = param.inputType === "CALCULATED";

  const inferred = inferCalculationTypeFromCode(code);

  if (configured && configured !== "NONE") {
    // If the parameter code belongs to a DIFFERENT clinical calculation,
    // prevent collision and preserve parameter identity!
    if (inferred && inferred !== configured) {
      return inferred;
    }
    return configured;
  }

  if (isCalculatedMode && inferred) {
    return inferred;
  }

  return "NONE";
}

/**
 * Returns canonical parameter code for any known laboratory alias.
 */
export function getCanonicalCode(code?: string | null): string {
  if (!code) return "";
  const upper = normalizeCode(code);
  return ALIAS_TO_CANONICAL[upper] || upper;
}

/**
 * Required input parameter codes for each CalculationType.
 */
const REQUIRED_INPUTS_BY_CALCULATION: Record<CalculationType, string[]> = {
  NONE: [],
  MCV: ["HCT", "RBC"],
  MCH: ["HGB", "RBC"],
  MCHC: ["HGB", "HCT"],
  VLDL: ["TG"],
  LDL_FRIEDEWALD: ["TC", "HDL", "TG"],
  NON_HDL_CHOLESTEROL: ["TC", "HDL"],
  CHOL_HDL_RATIO: ["TC", "HDL"],
  LDL_HDL_RATIO: ["LDL", "HDL"],
  INDIRECT_BILIRUBIN: ["TBIL", "DBIL"],
  GLOBULIN: ["TP", "ALB"],
  AG_RATIO: ["ALB", "GLOB"],
  BUN_CREATININE_RATIO: ["BUN", "CREAT"],
  UREA_CREATININE_RATIO: ["UREA", "CREAT"],
  EGFR_CKD_EPI_2021: ["CREAT"],
  ANION_GAP: ["NA", "CL", "HCO3"],
  ANION_GAP_K: ["NA", "K", "CL", "HCO3"],
};

/**
 * Resolves a parameter's current numeric value from available inputs by canonical code or aliases.
 */
function resolveNumericValue(
  canonicalReq: string,
  paramsByCanonical: Map<string, ReportParameterItemResponse>,
  currentValues: Record<string, string>,
  derivedValues: Record<string, string>
): { val: number; unit?: string } | null {
  const param = paramsByCanonical.get(canonicalReq);
  if (!param) return null;

  // Prefer derived calculated value if already computed, else user input value, else default
  const rawStr =
    derivedValues[param.parameterRefId] ??
    currentValues[param.parameterRefId] ??
    param.value ??
    "";

  if (!rawStr || rawStr.trim() === "") return null;

  const num = parseFloat(rawStr.trim());
  if (isNaN(num)) return null;

  return { val: num, unit: param.unit ?? undefined };
}

/**
 * Calculates patient age in years from patient snapshot.
 */
function resolvePatientAgeInYears(patient?: PatientDemographics | null): number | null {
  if (!patient) return null;

  if (patient.dateOfBirth) {
    const dob = new Date(patient.dateOfBirth);
    if (!isNaN(dob.getTime())) {
      const today = new Date();
      let age = today.getFullYear() - dob.getFullYear();
      const m = today.getMonth() - dob.getMonth();
      if (m < 0 || (m === 0 && today.getDate() < dob.getDate())) {
        age--;
      }
      return age;
    }
  }

  if (patient.ageValue != null && !isNaN(patient.ageValue)) {
    const unit = patient.ageUnit?.toUpperCase() ?? "YEARS";
    if (unit === "YEARS") return patient.ageValue;
    if (unit === "MONTHS") return Math.floor(patient.ageValue / 12);
  }

  return null;
}

/**
 * Topologically sorts calculated parameters to ensure chained dependencies
 * (e.g. Globulin before A/G Ratio, LDL before LDL/HDL Ratio) execute in proper order.
 */
function sortCalculatedParametersTopologically(
  calculatedParams: ReportParameterItemResponse[]
): ReportParameterItemResponse[] {
  if (calculatedParams.length <= 1) return calculatedParams;

  const codeToParam = new Map<string, ReportParameterItemResponse>();
  for (const p of calculatedParams) {
    const canon = getCanonicalCode(p.parameterCode);
    codeToParam.set(canon, p);
    codeToParam.set(p.parameterCode.toUpperCase(), p);
  }

  const inDegree = new Map<ReportParameterItemResponse, number>();
  const graph = new Map<ReportParameterItemResponse, Set<ReportParameterItemResponse>>();

  for (const p of calculatedParams) {
    inDegree.set(p, 0);
    graph.set(p, new Set());
  }

  for (const p of calculatedParams) {
    const reqCodes = REQUIRED_INPUTS_BY_CALCULATION[p.calculationType] || [];
    for (const reqCode of reqCodes) {
      const upstream = codeToParam.get(reqCode);
      if (upstream && upstream !== p) {
        if (!graph.get(upstream)?.has(p)) {
          graph.get(upstream)?.add(p);
          inDegree.set(p, (inDegree.get(p) ?? 0) + 1);
        }
      }
    }
  }

  const queue: ReportParameterItemResponse[] = [];
  for (const p of calculatedParams) {
    if ((inDegree.get(p) ?? 0) === 0) {
      queue.push(p);
    }
  }

  const sorted: ReportParameterItemResponse[] = [];
  while (queue.length > 0) {
    const current = queue.shift()!;
    sorted.push(current);

    const neighbors = graph.get(current);
    if (neighbors) {
      for (const neighbor of neighbors) {
        const newDeg = (inDegree.get(neighbor) ?? 1) - 1;
        inDegree.set(neighbor, newDeg);
        if (newDeg === 0) {
          queue.push(neighbor);
        }
      }
    }
  }

  return sorted.length === calculatedParams.length ? sorted : calculatedParams;
}

/**
 * Calculates a single parameter given resolved canonical numeric values and patient demographics.
 */
function calculateSingleParameter(
  calcType: CalculationType,
  paramsByCanonical: Map<string, ReportParameterItemResponse>,
  currentValues: Record<string, string>,
  derivedValues: Record<string, string>,
  patient?: PatientDemographics | null
): string {
  switch (calcType) {
    case "MCV": {
      // MCV (fL) = PCV (%) × 10 / RBC (million/µL)
      const hctRes = resolveNumericValue("HCT", paramsByCanonical, currentValues, derivedValues);
      const rbcRes = resolveNumericValue("RBC", paramsByCanonical, currentValues, derivedValues);
      if (!hctRes || !rbcRes) return "";

      let hct = hctRes.val;
      const rbc = rbcRes.val;

      // Normalization: if HCT is expressed as fraction (0 < val <= 1.0)
      if (hct > 0 && hct <= 1.0 && hctRes.unit?.toLowerCase().includes("l/l")) {
        hct = hct * 100;
      }

      if (hct <= 0 || rbc <= 0) return "";
      const mcv = (hct * 10) / rbc;
      return mcv.toFixed(1);
    }

    case "MCH": {
      // MCH (pg) = Hemoglobin (g/dL) × 10 / RBC (million/µL)
      const hbRes = resolveNumericValue("HGB", paramsByCanonical, currentValues, derivedValues);
      const rbcRes = resolveNumericValue("RBC", paramsByCanonical, currentValues, derivedValues);
      if (!hbRes || !rbcRes) return "";

      const hb = hbRes.val;
      const rbc = rbcRes.val;
      if (hb <= 0 || rbc <= 0) return "";

      const mch = (hb * 10) / rbc;
      return mch.toFixed(1);
    }

    case "MCHC": {
      // MCHC (g/dL) = Hemoglobin (g/dL) × 100 / PCV (%)
      const hbRes = resolveNumericValue("HGB", paramsByCanonical, currentValues, derivedValues);
      const hctRes = resolveNumericValue("HCT", paramsByCanonical, currentValues, derivedValues);
      if (!hbRes || !hctRes) return "";

      const hb = hbRes.val;
      let hct = hctRes.val;
      if (hct > 0 && hct <= 1.0 && hctRes.unit?.toLowerCase().includes("l/l")) {
        hct = hct * 100;
      }
      if (hb <= 0 || hct <= 0) return "";

      const mchc = (hb * 100) / hct;
      return mchc.toFixed(2);
    }

    case "VLDL": {
      // VLDL (mg/dL) = TG / 5 (Valid only if TG < 400 mg/dL)
      const tgRes = resolveNumericValue("TG", paramsByCanonical, currentValues, derivedValues);
      if (!tgRes) return "";
      const tg = tgRes.val;
      if (tg < 0 || tg >= 400) return ""; // Friedewald criteria invalid when TG >= 400
      return (tg / 5).toFixed(2);
    }

    case "LDL_FRIEDEWALD": {
      // LDL (mg/dL) = TC - HDL - (TG / 5) (Valid only if TG < 400 mg/dL, non-negative)
      const tcRes = resolveNumericValue("TC", paramsByCanonical, currentValues, derivedValues);
      const hdlRes = resolveNumericValue("HDL", paramsByCanonical, currentValues, derivedValues);
      const tgRes = resolveNumericValue("TG", paramsByCanonical, currentValues, derivedValues);
      if (!tcRes || !hdlRes || !tgRes) return "";

      const tc = tcRes.val;
      const hdl = hdlRes.val;
      const tg = tgRes.val;

      if (tg < 0 || tg >= 400 || tc < 0 || hdl < 0) return "";
      const ldl = tc - hdl - tg / 5;
      if (ldl < 0) return ""; // Biologically implausible
      return ldl.toFixed(2);
    }

    case "NON_HDL_CHOLESTEROL": {
      // Non-HDL = TC - HDL (Valid if TC >= HDL)
      const tcRes = resolveNumericValue("TC", paramsByCanonical, currentValues, derivedValues);
      const hdlRes = resolveNumericValue("HDL", paramsByCanonical, currentValues, derivedValues);
      if (!tcRes || !hdlRes) return "";

      const tc = tcRes.val;
      const hdl = hdlRes.val;
      if (tc < hdl || tc < 0 || hdl < 0) return "";
      return (tc - hdl).toFixed(2);
    }

    case "CHOL_HDL_RATIO": {
      // TC / HDL
      const tcRes = resolveNumericValue("TC", paramsByCanonical, currentValues, derivedValues);
      const hdlRes = resolveNumericValue("HDL", paramsByCanonical, currentValues, derivedValues);
      if (!tcRes || !hdlRes) return "";

      const tc = tcRes.val;
      const hdl = hdlRes.val;
      if (hdl <= 0 || tc < 0) return "";
      return (tc / hdl).toFixed(2);
    }

    case "LDL_HDL_RATIO": {
      // LDL / HDL
      const ldlRes = resolveNumericValue("LDL", paramsByCanonical, currentValues, derivedValues);
      const hdlRes = resolveNumericValue("HDL", paramsByCanonical, currentValues, derivedValues);
      if (!ldlRes || !hdlRes) return "";

      const ldl = ldlRes.val;
      const hdl = hdlRes.val;
      if (hdl <= 0 || ldl < 0) return "";
      return (ldl / hdl).toFixed(2);
    }

    case "INDIRECT_BILIRUBIN": {
      // Indirect Bilirubin = Total Bilirubin - Direct Bilirubin
      const tbilRes = resolveNumericValue("TBIL", paramsByCanonical, currentValues, derivedValues);
      const dbilRes = resolveNumericValue("DBIL", paramsByCanonical, currentValues, derivedValues);
      if (!tbilRes || !dbilRes) return "";

      const tbil = tbilRes.val;
      const dbil = dbilRes.val;
      if (dbil > tbil || tbil < 0 || dbil < 0) return "";
      return (tbil - dbil).toFixed(2);
    }

    case "GLOBULIN": {
      // Globulin = Total Protein - Albumin
      const tpRes = resolveNumericValue("TP", paramsByCanonical, currentValues, derivedValues);
      const albRes = resolveNumericValue("ALB", paramsByCanonical, currentValues, derivedValues);
      if (!tpRes || !albRes) return "";

      const tp = tpRes.val;
      const alb = albRes.val;
      if (alb > tp || tp < 0 || alb < 0) return "";
      return (tp - alb).toFixed(2);
    }

    case "AG_RATIO": {
      // A/G Ratio = Albumin / Globulin
      const albRes = resolveNumericValue("ALB", paramsByCanonical, currentValues, derivedValues);
      if (!albRes) return "";

      const alb = albRes.val;
      if (alb < 0) return "";

      let glob: number | null = null;
      const globRes = resolveNumericValue("GLOB", paramsByCanonical, currentValues, derivedValues);
      if (globRes) {
        glob = globRes.val;
      } else {
        // Fallback: derive globulin from TP - ALB if TP is present
        const tpRes = resolveNumericValue("TP", paramsByCanonical, currentValues, derivedValues);
        if (tpRes && tpRes.val >= alb) {
          glob = tpRes.val - alb;
        }
      }

      if (glob == null || glob <= 0) return "";
      return (alb / glob).toFixed(2);
    }

    case "BUN_CREATININE_RATIO": {
      // BUN / Creatinine
      const bunRes = resolveNumericValue("BUN", paramsByCanonical, currentValues, derivedValues);
      const creatRes = resolveNumericValue("CREAT", paramsByCanonical, currentValues, derivedValues);
      if (!bunRes || !creatRes) return "";

      const bun = bunRes.val;
      const creat = creatRes.val;
      if (creat <= 0 || bun < 0) return "";
      return (bun / creat).toFixed(2);
    }

    case "UREA_CREATININE_RATIO": {
      // Urea / Creatinine
      const ureaRes = resolveNumericValue("UREA", paramsByCanonical, currentValues, derivedValues);
      const creatRes = resolveNumericValue("CREAT", paramsByCanonical, currentValues, derivedValues);
      if (!ureaRes || !creatRes) return "";

      const urea = ureaRes.val;
      const creat = creatRes.val;
      if (creat <= 0 || urea < 0) return "";
      return (urea / creat).toFixed(2);
    }

    case "EGFR_CKD_EPI_2021": {
      // CKD-EPI 2021 equation (race-free, adults >= 18)
      const creatRes = resolveNumericValue("CREAT", paramsByCanonical, currentValues, derivedValues);
      if (!creatRes) return "";

      let creat = creatRes.val;
      if (creat <= 0) return "";

      // Unit normalization: convert umol/L to mg/dL if needed
      if (creatRes.unit?.toLowerCase().includes("umol") || creatRes.unit?.includes("µmol")) {
        creat = creat / 88.4;
      }

      const age = resolvePatientAgeInYears(patient);
      if (age == null || age < 18) return ""; // Only validated for adults >= 18

      const gender = patient?.gender?.trim().toUpperCase();
      if (gender !== "MALE" && gender !== "FEMALE") return "";

      const isFemale = gender === "FEMALE";
      const kappa = isFemale ? 0.7 : 0.9;
      const alpha = isFemale ? -0.241 : -0.302;
      const genderMultiplier = isFemale ? 1.012 : 1.0;

      const scrOverKappa = creat / kappa;
      const minTerm = Math.min(scrOverKappa, 1.0);
      const maxTerm = Math.max(scrOverKappa, 1.0);

      const egfr =
        142.0 *
        Math.pow(minTerm, alpha) *
        Math.pow(maxTerm, -1.2) *
        Math.pow(0.9938, age) *
        genderMultiplier;

      return egfr.toFixed(1);
    }

    case "ANION_GAP": {
      // Anion Gap = Na - (Cl + HCO3)
      const naRes = resolveNumericValue("NA", paramsByCanonical, currentValues, derivedValues);
      const clRes = resolveNumericValue("CL", paramsByCanonical, currentValues, derivedValues);
      const hco3Res = resolveNumericValue("HCO3", paramsByCanonical, currentValues, derivedValues);
      if (!naRes || !clRes || !hco3Res) return "";

      const na = naRes.val;
      const cl = clRes.val;
      const hco3 = hco3Res.val;
      return (na - (cl + hco3)).toFixed(1);
    }

    case "ANION_GAP_K": {
      // Anion Gap K = (Na + K) - (Cl + HCO3)
      const naRes = resolveNumericValue("NA", paramsByCanonical, currentValues, derivedValues);
      const kRes = resolveNumericValue("K", paramsByCanonical, currentValues, derivedValues);
      const clRes = resolveNumericValue("CL", paramsByCanonical, currentValues, derivedValues);
      const hco3Res = resolveNumericValue("HCO3", paramsByCanonical, currentValues, derivedValues);
      if (!naRes || !kRes || !clRes || !hco3Res) return "";

      const na = naRes.val;
      const k = kRes.val;
      const cl = clRes.val;
      const hco3 = hco3Res.val;
      return (na + k - (cl + hco3)).toFixed(1);
    }

    default:
      return "";
  }
}

/**
 * Calculates all CALCULATED parameters for a given test based on current input values.
 *
 * @param test The report test item containing its parameter definitions
 * @param currentValues Current parameter values keyed by parameterRefId
 * @param patient Patient demographics required for demographic-dependent calculations (e.g. eGFR)
 * @returns Map of parameterRefId -> calculated value string (empty string if cannot be calculated)
 */
export function computeCalculationsForTest(
  test: ReportTestItemResponse,
  currentValues: Record<string, string>,
  patient?: PatientDemographics | null
): Record<string, string> {
  const derivedValues: Record<string, string> = {};

  // Build canonical index of all parameters in this test
  const paramsByCanonical = new Map<string, ReportParameterItemResponse>();
  for (const param of test.parameters) {
    const canon = getCanonicalCode(param.parameterCode);
    paramsByCanonical.set(canon, param);
    paramsByCanonical.set(param.parameterCode.toUpperCase(), param);
  }

  // Filter and defensively resolve calculated parameters
  const calculatedParams: ReportParameterItemResponse[] = [];
  for (const param of test.parameters) {
    const effectiveType = resolveEffectiveCalculationType(param);
    if (effectiveType !== "NONE") {
      calculatedParams.push({
        ...param,
        calculationType: effectiveType,
      });
    }
  }

  const sortedCalculated = sortCalculatedParametersTopologically(calculatedParams);

  for (const param of sortedCalculated) {
    const calcResult = calculateSingleParameter(
      param.calculationType,
      paramsByCanonical,
      currentValues,
      derivedValues,
      patient
    );

    derivedValues[param.parameterRefId] = calcResult;
  }

  return derivedValues;
}

/**
 * Computes calculations for all tests in a report.
 */
export function computeAllCalculatedValues(
  tests: ReportTestItemResponse[],
  localParamValues: Record<string, Record<string, string>>,
  patient?: PatientDemographics | null
): Record<string, Record<string, string>> {
  const result: Record<string, Record<string, string>> = {};

  for (const test of tests) {
    const testCurrentValues = localParamValues[test.refId] ?? {};
    result[test.refId] = computeCalculationsForTest(test, testCurrentValues, patient);
  }

  return result;
}
