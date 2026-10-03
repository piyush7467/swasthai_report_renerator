import { computeCalculationsForTest } from "../clinicalCalculations";
import type { ReportTestItemResponse } from "../../types/reportTypes";

function assert(condition: boolean, message: string) {
  if (!condition) {
    throw new Error(`Assertion failed: ${message}`);
  }
}

function runTests() {
  console.log("=== Running Clinical Calculation Engine Frontend Tests ===");

  // 1. CBC Test Panel
  console.log("1. Testing CBC Real-Time Calculations (MCV, MCH, MCHC)...");
  const cbcTest: ReportTestItemResponse = {
    refId: "test_cbc_1",
    testRefId: "master_cbc",
    testCode: "CBC",
    testName: "Complete Blood Count",
    displayOrder: 1,
    testVersion: 1,
    parameters: [
      {
        refId: "p_hb",
        parameterRefId: "param_hb",
        parameterCode: "HB",
        parameterName: "Hemoglobin",
        dataType: "DECIMAL",
        inputType: "MANUAL",
        calculationType: "NONE",
        unit: "g/dL",
        displayOrder: 1,
      },
      {
        refId: "p_rbc",
        parameterRefId: "param_rbc",
        parameterCode: "RBC",
        parameterName: "RBC Count",
        dataType: "DECIMAL",
        inputType: "MANUAL",
        calculationType: "NONE",
        unit: "million/µL",
        displayOrder: 2,
      },
      {
        refId: "p_pcv",
        parameterRefId: "param_pcv",
        parameterCode: "PCV",
        parameterName: "PCV (Hematocrit)",
        dataType: "DECIMAL",
        inputType: "MANUAL",
        calculationType: "NONE",
        unit: "%",
        displayOrder: 3,
      },
      {
        refId: "p_mcv",
        parameterRefId: "param_mcv",
        parameterCode: "MCV",
        parameterName: "Mean Corpuscular Volume",
        dataType: "DECIMAL",
        inputType: "CALCULATED",
        calculationType: "MCV",
        unit: "fL",
        displayOrder: 4,
      },
      {
        refId: "p_mch",
        parameterRefId: "param_mch",
        parameterCode: "MCH",
        parameterName: "Mean Corpuscular Hemoglobin",
        dataType: "DECIMAL",
        inputType: "CALCULATED",
        calculationType: "MCH",
        unit: "pg",
        displayOrder: 5,
      },
      {
        refId: "p_mchc",
        parameterRefId: "param_mchc",
        parameterCode: "MCHC",
        parameterName: "Mean Corpuscular Hemoglobin Concentration",
        dataType: "DECIMAL",
        inputType: "CALCULATED",
        calculationType: "MCHC",
        unit: "g/dL",
        displayOrder: 6,
      },
    ],
  };

  // Case 1a: Partial input - only HB entered
  let results = computeCalculationsForTest(cbcTest, { param_hb: "14" });
  assert(results["param_mcv"] === "", "MCV should be empty when PCV and RBC missing");
  assert(results["param_mch"] === "", "MCH should be empty when RBC missing");
  assert(results["param_mchc"] === "", "MCHC should be empty when PCV missing");
  console.log("  ✓ Partial inputs remain empty (no NaN or 0)");

  // Case 1b: User enters RBC = 5.0
  results = computeCalculationsForTest(cbcTest, { param_hb: "14", param_rbc: "5.0" });
  assert(results["param_mch"] === "28.0", `MCH expected 28.0, got ${results["param_mch"]}`);
  assert(results["param_mcv"] === "", "MCV still empty because PCV missing");
  assert(results["param_mchc"] === "", "MCHC still empty because PCV missing");
  console.log("  ✓ MCH automatically calculated to 28.0 when HB and RBC present");

  // Case 1c: User enters PCV = 42
  results = computeCalculationsForTest(cbcTest, {
    param_hb: "14",
    param_rbc: "5.0",
    param_pcv: "42",
  });
  assert(results["param_mcv"] === "84.0", `MCV expected 84.0, got ${results["param_mcv"]}`);
  assert(results["param_mch"] === "28.0", `MCH expected 28.0, got ${results["param_mch"]}`);
  assert(results["param_mchc"] === "33.33", `MCHC expected 33.33, got ${results["param_mchc"]}`);
  console.log("  ✓ User example verified: HB=14, RBC=5.0, PCV=42 => MCV=84.0, MCH=28.0, MCHC=33.33");

  // Case 1d: Zero division protection
  results = computeCalculationsForTest(cbcTest, {
    param_hb: "14",
    param_rbc: "0",
    param_pcv: "0",
  });
  assert(results["param_mcv"] === "", "MCV must be empty on zero RBC");
  assert(results["param_mch"] === "", "MCH must be empty on zero RBC");
  assert(results["param_mchc"] === "", "MCHC must be empty on zero PCV");
  console.log("  ✓ Zero division safely handled");

  // 2. Lipid Profile Test Panel
  console.log("2. Testing Lipid Profile (VLDL, LDL Friedewald, Non-HDL, Ratios)...");
  const lipidTest: ReportTestItemResponse = {
    refId: "test_lipid_1",
    testRefId: "master_lipid",
    testCode: "LIPID",
    testName: "Lipid Profile",
    displayOrder: 2,
    testVersion: 1,
    parameters: [
      {
        refId: "p_tc",
        parameterRefId: "param_tc",
        parameterCode: "CHOLESTEROL",
        parameterName: "Total Cholesterol",
        dataType: "DECIMAL",
        inputType: "MANUAL",
        calculationType: "NONE",
        unit: "mg/dL",
        displayOrder: 1,
      },
      {
        refId: "p_tg",
        parameterRefId: "param_tg",
        parameterCode: "TRIGLYCERIDES",
        parameterName: "Triglycerides",
        dataType: "DECIMAL",
        inputType: "MANUAL",
        calculationType: "NONE",
        unit: "mg/dL",
        displayOrder: 2,
      },
      {
        refId: "p_hdl",
        parameterRefId: "param_hdl",
        parameterCode: "HDL_C",
        parameterName: "HDL Cholesterol",
        dataType: "DECIMAL",
        inputType: "MANUAL",
        calculationType: "NONE",
        unit: "mg/dL",
        displayOrder: 3,
      },
      {
        refId: "p_vldl",
        parameterRefId: "param_vldl",
        parameterCode: "VLDL",
        parameterName: "VLDL Cholesterol",
        dataType: "DECIMAL",
        inputType: "CALCULATED",
        calculationType: "VLDL",
        unit: "mg/dL",
        displayOrder: 4,
      },
      {
        refId: "p_ldl",
        parameterRefId: "param_ldl",
        parameterCode: "LDL",
        parameterName: "LDL Cholesterol",
        dataType: "DECIMAL",
        inputType: "CALCULATED",
        calculationType: "LDL_FRIEDEWALD",
        unit: "mg/dL",
        displayOrder: 5,
      },
      {
        refId: "p_non_hdl",
        parameterRefId: "param_non_hdl",
        parameterCode: "NON_HDL",
        parameterName: "Non-HDL Cholesterol",
        dataType: "DECIMAL",
        inputType: "CALCULATED",
        calculationType: "NON_HDL_CHOLESTEROL",
        unit: "mg/dL",
        displayOrder: 6,
      },
      {
        refId: "p_chol_ratio",
        parameterRefId: "param_chol_ratio",
        parameterCode: "CHOL_HDL_RATIO",
        parameterName: "Cholesterol / HDL Ratio",
        dataType: "DECIMAL",
        inputType: "CALCULATED",
        calculationType: "CHOL_HDL_RATIO",
        unit: "",
        displayOrder: 7,
      },
      {
        refId: "p_ldl_ratio",
        parameterRefId: "param_ldl_ratio",
        parameterCode: "LDL_HDL_RATIO",
        parameterName: "LDL / HDL Ratio",
        dataType: "DECIMAL",
        inputType: "CALCULATED",
        calculationType: "LDL_HDL_RATIO",
        unit: "",
        displayOrder: 8,
      },
    ],
  };

  // Normal lipid calculation with chained LDL -> LDL/HDL Ratio
  let lipidResults = computeCalculationsForTest(lipidTest, {
    param_tc: "200",
    param_tg: "150",
    param_hdl: "50",
  });
  assert(lipidResults["param_vldl"] === "30.00", `VLDL expected 30.00, got ${lipidResults["param_vldl"]}`);
  assert(lipidResults["param_ldl"] === "120.00", `LDL expected 120.00, got ${lipidResults["param_ldl"]}`);
  assert(lipidResults["param_non_hdl"] === "150.00", `Non-HDL expected 150.00, got ${lipidResults["param_non_hdl"]}`);
  assert(lipidResults["param_chol_ratio"] === "4.00", `Chol/HDL ratio expected 4.00, got ${lipidResults["param_chol_ratio"]}`);
  assert(lipidResults["param_ldl_ratio"] === "2.40", `LDL/HDL ratio expected 2.40, got ${lipidResults["param_ldl_ratio"]}`);
  console.log("  ✓ Lipid Profile calculations and chained LDL/HDL ratio calculated correctly");

  // Friedewald criteria invalid when TG >= 400 mg/dL
  lipidResults = computeCalculationsForTest(lipidTest, {
    param_tc: "200",
    param_tg: "450",
    param_hdl: "50",
  });
  assert(lipidResults["param_vldl"] === "", "VLDL must be empty when TG >= 400");
  assert(lipidResults["param_ldl"] === "", "LDL must be empty when TG >= 400");
  assert(lipidResults["param_ldl_ratio"] === "", "LDL/HDL must be empty when LDL is invalid");
  assert(lipidResults["param_non_hdl"] === "150.00", "Non-HDL is still valid when TG >= 400");
  console.log("  ✓ Friedewald TG >= 400 mg/dL guardrail enforced");

  // 3. LFT Chained Calculations (Total Protein + Albumin -> Globulin -> A/G Ratio)
  console.log("3. Testing LFT Chained Calculations (Total Protein + Albumin -> Globulin -> A/G Ratio)...");
  const lftTest: ReportTestItemResponse = {
    refId: "test_lft_1",
    testRefId: "master_lft",
    testCode: "LFT",
    testName: "Liver Function Tests",
    displayOrder: 3,
    testVersion: 1,
    parameters: [
      {
        refId: "p_tp",
        parameterRefId: "param_tp",
        parameterCode: "TP",
        parameterName: "Total Protein",
        dataType: "DECIMAL",
        inputType: "MANUAL",
        calculationType: "NONE",
        unit: "g/dL",
        displayOrder: 1,
      },
      {
        refId: "p_alb",
        parameterRefId: "param_alb",
        parameterCode: "ALB",
        parameterName: "Albumin",
        dataType: "DECIMAL",
        inputType: "MANUAL",
        calculationType: "NONE",
        unit: "g/dL",
        displayOrder: 2,
      },
      {
        refId: "p_glob",
        parameterRefId: "param_glob",
        parameterCode: "GLOB",
        parameterName: "Globulin",
        dataType: "DECIMAL",
        inputType: "CALCULATED",
        calculationType: "GLOBULIN",
        unit: "g/dL",
        displayOrder: 3,
      },
      {
        refId: "p_ag",
        parameterRefId: "param_ag",
        parameterCode: "AG_RATIO",
        parameterName: "A/G Ratio",
        dataType: "DECIMAL",
        inputType: "CALCULATED",
        calculationType: "AG_RATIO",
        unit: "",
        displayOrder: 4,
      },
      {
        refId: "p_tbil",
        parameterRefId: "param_tbil",
        parameterCode: "TBIL",
        parameterName: "Total Bilirubin",
        dataType: "DECIMAL",
        inputType: "MANUAL",
        calculationType: "NONE",
        unit: "mg/dL",
        displayOrder: 5,
      },
      {
        refId: "p_dbil",
        parameterRefId: "param_dbil",
        parameterCode: "DBIL",
        parameterName: "Direct Bilirubin",
        dataType: "DECIMAL",
        inputType: "MANUAL",
        calculationType: "NONE",
        unit: "mg/dL",
        displayOrder: 6,
      },
      {
        refId: "p_ibil",
        parameterRefId: "param_ibil",
        parameterCode: "IBIL",
        parameterName: "Indirect Bilirubin",
        dataType: "DECIMAL",
        inputType: "CALCULATED",
        calculationType: "INDIRECT_BILIRUBIN",
        unit: "mg/dL",
        displayOrder: 7,
      },
    ],
  };

  const lftResults = computeCalculationsForTest(lftTest, {
    param_tp: "7.0",
    param_alb: "4.2",
    param_tbil: "2.5",
    param_dbil: "0.7",
  });
  // Globulin = 7.0 - 4.2 = 2.80
  assert(lftResults["param_glob"] === "2.80", `Globulin expected 2.80, got ${lftResults["param_glob"]}`);
  // A/G Ratio = 4.2 / 2.8 = 1.50
  assert(lftResults["param_ag"] === "1.50", `A/G Ratio expected 1.50, got ${lftResults["param_ag"]}`);
  // Indirect Bilirubin = 2.5 - 0.7 = 1.80
  assert(lftResults["param_ibil"] === "1.80", `Indirect Bilirubin expected 1.80, got ${lftResults["param_ibil"]}`);
  console.log("  ✓ LFT Chained calculations (Globulin and A/G ratio) and Indirect Bilirubin passed");

  // 4. KFT & eGFR with Patient Demographics
  console.log("4. Testing KFT & eGFR CKD-EPI 2021 with Patient Demographics...");
  const kftTest: ReportTestItemResponse = {
    refId: "test_kft_1",
    testRefId: "master_kft",
    testCode: "KFT",
    testName: "Kidney Function Tests",
    displayOrder: 4,
    testVersion: 1,
    parameters: [
      {
        refId: "p_creat",
        parameterRefId: "param_creat",
        parameterCode: "CREATININE",
        parameterName: "Serum Creatinine",
        dataType: "DECIMAL",
        inputType: "MANUAL",
        calculationType: "NONE",
        unit: "mg/dL",
        displayOrder: 1,
      },
      {
        refId: "p_urea",
        parameterRefId: "param_urea",
        parameterCode: "UREA",
        parameterName: "Serum Urea",
        dataType: "DECIMAL",
        inputType: "MANUAL",
        calculationType: "NONE",
        unit: "mg/dL",
        displayOrder: 2,
      },
      {
        refId: "p_egfr",
        parameterRefId: "param_egfr",
        parameterCode: "EGFR",
        parameterName: "Estimated GFR (CKD-EPI 2021)",
        dataType: "DECIMAL",
        inputType: "CALCULATED",
        calculationType: "EGFR_CKD_EPI_2021",
        unit: "mL/min/1.73 m²",
        displayOrder: 3,
      },
      {
        refId: "p_urea_creat",
        parameterRefId: "param_urea_creat",
        parameterCode: "UREA_CREAT_RATIO",
        parameterName: "Urea / Creatinine Ratio",
        dataType: "DECIMAL",
        inputType: "CALCULATED",
        calculationType: "UREA_CREATININE_RATIO",
        unit: "",
        displayOrder: 4,
      },
    ],
  };

  // 40yo Female, Creatinine 0.8
  let kftResults = computeCalculationsForTest(
    kftTest,
    { param_creat: "0.8", param_urea: "28.0" },
    { gender: "FEMALE", ageValue: 40, ageUnit: "YEARS" }
  );
  assert(kftResults["param_egfr"] === "95.5", `eGFR expected 95.5, got ${kftResults["param_egfr"]}`);
  assert(kftResults["param_urea_creat"] === "35.00", `Urea/Creat ratio expected 35.00, got ${kftResults["param_urea_creat"]}`);
  console.log("  ✓ eGFR (CKD-EPI 2021) 40yo Female = 95.5 and Urea/Creat Ratio = 35.00");

  // 50yo Male, Creatinine 1.2
  kftResults = computeCalculationsForTest(
    kftTest,
    { param_creat: "1.2" },
    { gender: "MALE", ageValue: 50, ageUnit: "YEARS" }
  );
  assert(kftResults["param_egfr"] === "73.7", `eGFR expected 73.7, got ${kftResults["param_egfr"]}`);
  console.log("  ✓ eGFR (CKD-EPI 2021) 50yo Male = 73.7");

  // Pediatric age (< 18) guardrail
  kftResults = computeCalculationsForTest(
    kftTest,
    { param_creat: "0.7" },
    { gender: "FEMALE", ageValue: 15, ageUnit: "YEARS" }
  );
  assert(kftResults["param_egfr"] === "", "eGFR must be empty for pediatric patients (< 18)");
  console.log("  ✓ Pediatric age (< 18) guardrail enforced");

  // 5. Electrolytes (Anion Gap, Anion Gap K)
  console.log("5. Testing Electrolytes (Anion Gap and Anion Gap K)...");
  const electrolyteTest: ReportTestItemResponse = {
    refId: "test_lytes_1",
    testRefId: "master_lytes",
    testCode: "LYTES",
    testName: "Serum Electrolytes",
    displayOrder: 5,
    testVersion: 1,
    parameters: [
      {
        refId: "p_na",
        parameterRefId: "param_na",
        parameterCode: "SODIUM",
        parameterName: "Sodium",
        dataType: "DECIMAL",
        inputType: "MANUAL",
        calculationType: "NONE",
        unit: "mmol/L",
        displayOrder: 1,
      },
      {
        refId: "p_k",
        parameterRefId: "param_k",
        parameterCode: "POTASSIUM",
        parameterName: "Potassium",
        dataType: "DECIMAL",
        inputType: "MANUAL",
        calculationType: "NONE",
        unit: "mmol/L",
        displayOrder: 2,
      },
      {
        refId: "p_cl",
        parameterRefId: "param_cl",
        parameterCode: "CHLORIDE",
        parameterName: "Chloride",
        dataType: "DECIMAL",
        inputType: "MANUAL",
        calculationType: "NONE",
        unit: "mmol/L",
        displayOrder: 3,
      },
      {
        refId: "p_hco3",
        parameterRefId: "param_hco3",
        parameterCode: "BICARBONATE",
        parameterName: "Bicarbonate",
        dataType: "DECIMAL",
        inputType: "MANUAL",
        calculationType: "NONE",
        unit: "mmol/L",
        displayOrder: 4,
      },
      {
        refId: "p_ag",
        parameterRefId: "param_ag",
        parameterCode: "ANION_GAP",
        parameterName: "Anion Gap",
        dataType: "DECIMAL",
        inputType: "CALCULATED",
        calculationType: "ANION_GAP",
        unit: "mmol/L",
        displayOrder: 5,
      },
      {
        refId: "p_agk",
        parameterRefId: "param_agk",
        parameterCode: "ANION_GAP_K",
        parameterName: "Anion Gap (with K)",
        dataType: "DECIMAL",
        inputType: "CALCULATED",
        calculationType: "ANION_GAP_K",
        unit: "mmol/L",
        displayOrder: 6,
      },
    ],
  };

  const lytesResults = computeCalculationsForTest(electrolyteTest, {
    param_na: "140.0",
    param_k: "4.5",
    param_cl: "100.0",
    param_hco3: "24.0",
  });
  // Anion Gap = 140 - (100 + 24) = 16.0
  assert(lytesResults["param_ag"] === "16.0", `Anion Gap expected 16.0, got ${lytesResults["param_ag"]}`);
  // Anion Gap K = (140 + 4.5) - (100 + 24) = 20.5
  assert(lytesResults["param_agk"] === "20.5", `Anion Gap K expected 20.5, got ${lytesResults["param_agk"]}`);
  console.log("  ✓ Anion Gap = 16.0 and Anion Gap K = 20.5 verified");

  // 6. Anti-Collision & Parameter Identity Preservation (The Bug Verification Test)
  console.log("\n6. Testing Anti-Collision & Parameter Identity Preservation (Root-Cause Bug Test)...");
  
  // Test case matching user screenshot: HCT/PCV = 39.9, RBC = 5.0, HB = 14.0
  const cbcAntiCollisionTest: ReportTestItemResponse = {
    refId: "test_cbc_collision",
    testRefId: "master_cbc",
    testCode: "CBC",
    testName: "Complete Blood Count",
    displayOrder: 1,
    testVersion: 1,
    parameters: [
      {
        refId: "p_hb",
        parameterRefId: "param_hb",
        parameterCode: "HB",
        parameterName: "Hemoglobin",
        dataType: "DECIMAL",
        inputType: "MANUAL",
        calculationType: "NONE",
        unit: "g/dL",
        displayOrder: 1,
      },
      {
        refId: "p_rbc",
        parameterRefId: "param_rbc",
        parameterCode: "RBC",
        parameterName: "RBC Count",
        dataType: "DECIMAL",
        inputType: "MANUAL",
        calculationType: "NONE",
        unit: "million/µL",
        displayOrder: 2,
      },
      {
        refId: "p_pcv",
        parameterRefId: "param_pcv",
        parameterCode: "PCV",
        parameterName: "PCV (Hematocrit)",
        dataType: "DECIMAL",
        inputType: "MANUAL",
        calculationType: "NONE",
        unit: "%",
        displayOrder: 3,
      },
      // Note: In legacy bad configuration, MCH and MCHC were misconfigured as MCV!
      {
        refId: "p_mcv",
        parameterRefId: "param_mcv",
        parameterCode: "MCV",
        parameterName: "Mean Corpuscular Volume",
        dataType: "DECIMAL",
        inputType: "CALCULATED",
        calculationType: "MCV",
        unit: "fL",
        displayOrder: 4,
      },
      {
        refId: "p_mch",
        parameterRefId: "param_mch",
        parameterCode: "MCH",
        parameterName: "Mean Corpuscular Hemoglobin",
        dataType: "DECIMAL",
        inputType: "CALCULATED",
        calculationType: "MCV", // Misconfigured as MCV in legacy DB!
        unit: "pg",
        displayOrder: 5,
      },
      {
        refId: "p_mchc",
        parameterRefId: "param_mchc",
        parameterCode: "MCHC",
        parameterName: "Mean Corpuscular Hemoglobin Concentration",
        dataType: "DECIMAL",
        inputType: "CALCULATED",
        calculationType: "MCV", // Misconfigured as MCV in legacy DB!
        unit: "g/dL",
        displayOrder: 6,
      },
    ],
  };

  const antiCollisionResults = computeCalculationsForTest(cbcAntiCollisionTest, {
    param_hb: "14",
    param_rbc: "5.0",
    param_pcv: "39.9",
  });

  // HCT (39.9) * 10 / RBC (5.0) = 79.8
  assert(
    antiCollisionResults["param_mcv"] === "79.8",
    `MCV expected 79.8, got ${antiCollisionResults["param_mcv"]}`
  );

  // HB (14) * 10 / RBC (5.0) = 28.0 (MUST NOT BE 79.8!)
  assert(
    antiCollisionResults["param_mch"] === "28.0",
    `MCH expected 28.0, got ${antiCollisionResults["param_mch"]}. Cross-contamination detected!`
  );

  // HB (14) * 100 / HCT (39.9) = 35.09 (MUST NOT BE 79.8!)
  assert(
    antiCollisionResults["param_mchc"] === "35.09",
    `MCHC expected 35.09, got ${antiCollisionResults["param_mchc"]}. Cross-contamination detected!`
  );

  // Assert all 3 are distinct
  assert(
    antiCollisionResults["param_mcv"] !== antiCollisionResults["param_mch"],
    "MCV and MCH must have different values"
  );
  assert(
    antiCollisionResults["param_mch"] !== antiCollisionResults["param_mchc"],
    "MCH and MCHC must have different values"
  );
  assert(
    antiCollisionResults["param_mcv"] !== antiCollisionResults["param_mchc"],
    "MCV and MCHC must have different values"
  );

  console.log("  ✓ Anti-collision verified: MCV=79.8, MCH=28.0, MCHC=35.09 are all distinct and correctly computed!");

  console.log("\nALL 6 TEST SUITES PASSED 100% SUCCESSFULLY!");
}

runTests();

