package com.swasthai.report_generator.test.calculation;

import com.swasthai.report_generator.patient.entity.Gender;
import com.swasthai.report_generator.test.calculation.calculators.*;
import com.swasthai.report_generator.test.calculation.impl.CalculationEngineImpl;
import com.swasthai.report_generator.test.entity.CalculationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ClinicalCalculationEngineTest {

    private CalculationEngine calculationEngine;

    @BeforeEach
    void setUp() {
        calculationEngine = new CalculationEngineImpl(List.of(
                new MCVCalculator(),
                new MCHCalculator(),
                new MCHCCalculator(),
                new VLDLCalculator(),
                new LDLFriedewaldCalculator(),
                new NonHDLCholesterolCalculator(),
                new CholesterolHDLtoRatioCalculator(),
                new LDLtoHDLratioCalculator(),
                new IndirectBilirubinCalculator(),
                new GlobulinCalculator(),
                new AGRatioCalculator(),
                new BUNCreatinineRatioCalculator(),
                new UreaCreatinineRatioCalculator(),
                new EgfrCkdEpi2021Calculator(),
                new AnionGapCalculator(),
                new AnionGapKCalculator()
        ));
    }

    @Nested
    @DisplayName("Lipid Profile Calculations")
    class LipidProfileTests {

        @Test
        @DisplayName("VLDL: calculates TG / 5 correctly when TG < 400 mg/dL")
        void shouldCalculateVLDL() {
            Map<String, BigDecimal> values = Map.of("TG", new BigDecimal("150"));
            BigDecimal vldl = calculationEngine.calculate(CalculationType.VLDL, values);
            assertEquals(new BigDecimal("30.00"), vldl);
        }

        @Test
        @DisplayName("VLDL: throws CalculationException when TG >= 400 mg/dL (Friedewald invalid)")
        void shouldRejectVLDLWhenTGTooHigh() {
            Map<String, BigDecimal> values = Map.of("TG", new BigDecimal("450"));
            CalculationException ex = assertThrows(
                    CalculationException.class,
                    () -> calculationEngine.calculate(CalculationType.VLDL, values)
            );
            assertTrue(ex.getMessage().contains("Triglycerides >= 400 mg/dL"));
        }

        @Test
        @DisplayName("LDL Friedewald: calculates TC - HDL - (TG / 5)")
        void shouldCalculateLDLFriedewald() {
            Map<String, BigDecimal> values = Map.of(
                    "TC", new BigDecimal("200"),
                    "HDL", new BigDecimal("50"),
                    "TG", new BigDecimal("150")
            );
            BigDecimal ldl = calculationEngine.calculate(CalculationType.LDL_FRIEDEWALD, values);
            assertEquals(new BigDecimal("120.00"), ldl);
        }

        @Test
        @DisplayName("LDL Friedewald: rejects calculation when TG >= 400 mg/dL")
        void shouldRejectLDLFriedewaldWhenTGTooHigh() {
            Map<String, BigDecimal> values = Map.of(
                    "TC", new BigDecimal("200"),
                    "HDL", new BigDecimal("50"),
                    "TG", new BigDecimal("400")
            );
            CalculationException ex = assertThrows(
                    CalculationException.class,
                    () -> calculationEngine.calculate(CalculationType.LDL_FRIEDEWALD, values)
            );
            assertTrue(ex.getMessage().contains("Triglycerides >= 400 mg/dL"));
        }

        @Test
        @DisplayName("LDL Friedewald: rejects biologically implausible negative result")
        void shouldRejectNegativeLDLFriedewald() {
            Map<String, BigDecimal> values = Map.of(
                    "TC", new BigDecimal("100"),
                    "HDL", new BigDecimal("60"),
                    "TG", new BigDecimal("300") // VLDL = 60 => 100 - 60 - 60 = -20
            );
            CalculationException ex = assertThrows(
                    CalculationException.class,
                    () -> calculationEngine.calculate(CalculationType.LDL_FRIEDEWALD, values)
            );
            assertTrue(ex.getMessage().contains("Calculated LDL-C is negative"));
        }

        @Test
        @DisplayName("Non-HDL Cholesterol: calculates TC - HDL")
        void shouldCalculateNonHDL() {
            Map<String, BigDecimal> values = Map.of(
                    "TC", new BigDecimal("220"),
                    "HDL", new BigDecimal("45")
            );
            BigDecimal nonHdl = calculationEngine.calculate(CalculationType.NON_HDL_CHOLESTEROL, values);
            assertEquals(new BigDecimal("175.00"), nonHdl);
        }

        @Test
        @DisplayName("Non-HDL Cholesterol: rejects when TC < HDL")
        void shouldRejectNonHDLWhenTCBelowHDL() {
            Map<String, BigDecimal> values = Map.of(
                    "TC", new BigDecimal("40"),
                    "HDL", new BigDecimal("50")
            );
            CalculationException ex = assertThrows(
                    CalculationException.class,
                    () -> calculationEngine.calculate(CalculationType.NON_HDL_CHOLESTEROL, values)
            );
            assertTrue(ex.getMessage().contains("cannot be less than HDL"));
        }

        @Test
        @DisplayName("Cholesterol / HDL Ratio: calculates TC / HDL with zero check")
        void shouldCalculateCholHDLratio() {
            Map<String, BigDecimal> values = Map.of(
                    "TC", new BigDecimal("200"),
                    "HDL", new BigDecimal("50")
            );
            BigDecimal ratio = calculationEngine.calculate(CalculationType.CHOL_HDL_RATIO, values);
            assertEquals(new BigDecimal("4.00"), ratio);

            Map<String, BigDecimal> zeroHdl = Map.of(
                    "TC", new BigDecimal("200"),
                    "HDL", BigDecimal.ZERO
            );
            assertThrows(CalculationException.class, () -> calculationEngine.calculate(CalculationType.CHOL_HDL_RATIO, zeroHdl));
        }

        @Test
        @DisplayName("LDL / HDL Ratio: calculates LDL / HDL with zero check")
        void shouldCalculateLDLHDLratio() {
            Map<String, BigDecimal> values = Map.of(
                    "LDL", new BigDecimal("130"),
                    "HDL", new BigDecimal("50")
            );
            BigDecimal ratio = calculationEngine.calculate(CalculationType.LDL_HDL_RATIO, values);
            assertEquals(new BigDecimal("2.60"), ratio);
        }
    }

    @Nested
    @DisplayName("Liver Function Tests (LFT)")
    class LftTests {

        @Test
        @DisplayName("Indirect Bilirubin: calculates Total Bilirubin - Direct Bilirubin")
        void shouldCalculateIndirectBilirubin() {
            Map<String, BigDecimal> values = Map.of(
                    "TBIL", new BigDecimal("2.50"),
                    "DBIL", new BigDecimal("0.70")
            );
            BigDecimal indirect = calculationEngine.calculate(CalculationType.INDIRECT_BILIRUBIN, values);
            assertEquals(new BigDecimal("1.80"), indirect);
        }

        @Test
        @DisplayName("Indirect Bilirubin: rejects when Direct Bilirubin > Total Bilirubin")
        void shouldRejectWhenDirectBilirubinExceedsTotal() {
            Map<String, BigDecimal> values = Map.of(
                    "TBIL", new BigDecimal("1.00"),
                    "DBIL", new BigDecimal("1.50")
            );
            CalculationException ex = assertThrows(
                    CalculationException.class,
                    () -> calculationEngine.calculate(CalculationType.INDIRECT_BILIRUBIN, values)
            );
            assertTrue(ex.getMessage().contains("Direct Bilirubin (1.50) cannot exceed Total Bilirubin (1.00)"));
        }

        @Test
        @DisplayName("Globulin: calculates Total Protein - Albumin")
        void shouldCalculateGlobulin() {
            Map<String, BigDecimal> values = Map.of(
                    "TP", new BigDecimal("7.4"),
                    "ALB", new BigDecimal("4.2")
            );
            BigDecimal globulin = calculationEngine.calculate(CalculationType.GLOBULIN, values);
            assertEquals(new BigDecimal("3.20"), globulin);
        }

        @Test
        @DisplayName("Globulin: rejects when Albumin > Total Protein")
        void shouldRejectWhenAlbuminExceedsTotalProtein() {
            Map<String, BigDecimal> values = Map.of(
                    "TP", new BigDecimal("5.0"),
                    "ALB", new BigDecimal("5.5")
            );
            CalculationException ex = assertThrows(
                    CalculationException.class,
                    () -> calculationEngine.calculate(CalculationType.GLOBULIN, values)
            );
            assertTrue(ex.getMessage().contains("Albumin (5.5 g/dL) cannot exceed Total Protein (5.0 g/dL)"));
        }

        @Test
        @DisplayName("A/G Ratio: calculates Albumin / Globulin")
        void shouldCalculateAGRatio() {
            Map<String, BigDecimal> values = Map.of(
                    "ALB", new BigDecimal("4.2"),
                    "GLOB", new BigDecimal("2.8")
            );
            BigDecimal agRatio = calculationEngine.calculate(CalculationType.AG_RATIO, values);
            assertEquals(new BigDecimal("1.50"), agRatio);
        }

        @Test
        @DisplayName("A/G Ratio: can derive Globulin directly in AGRatioCalculator if GLOB not in map")
        void shouldCalculateAGRatioWithFallbackFromTP() {
            CalculationContext context = CalculationContext.of(
                    Map.of(
                            "ALB", new BigDecimal("4.0"),
                            "TP", new BigDecimal("6.0")
                    )
            );
            BigDecimal agRatio = new AGRatioCalculator().calculate(context);
            assertEquals(new BigDecimal("2.00"), agRatio);
        }

        @Test
        @DisplayName("A/G Ratio: protects against Globulin = 0")
        void shouldProtectAgainstGlobulinZero() {
            Map<String, BigDecimal> values = Map.of(
                    "ALB", new BigDecimal("4.0"),
                    "GLOB", BigDecimal.ZERO
            );
            CalculationException ex = assertThrows(
                    CalculationException.class,
                    () -> calculationEngine.calculate(CalculationType.AG_RATIO, values)
            );
            assertTrue(ex.getMessage().contains("Globulin is zero"));
        }
    }

    @Nested
    @DisplayName("Kidney Function Tests (KFT) & eGFR")
    class KftTests {

        @Test
        @DisplayName("BUN / Creatinine Ratio: calculates BUN / Creatinine")
        void shouldCalculateBUNCreatinineRatio() {
            Map<String, BigDecimal> values = Map.of(
                    "BUN", new BigDecimal("18.0"),
                    "CREAT", new BigDecimal("0.9")
            );
            BigDecimal ratio = calculationEngine.calculate(CalculationType.BUN_CREATININE_RATIO, values);
            assertEquals(new BigDecimal("20.00"), ratio);
        }

        @Test
        @DisplayName("Urea / Creatinine Ratio: calculates Urea / Creatinine")
        void shouldCalculateUreaCreatinineRatio() {
            Map<String, BigDecimal> values = Map.of(
                    "UREA", new BigDecimal("35.0"),
                    "CREAT", new BigDecimal("1.0")
            );
            BigDecimal ratio = calculationEngine.calculate(CalculationType.UREA_CREATININE_RATIO, values);
            assertEquals(new BigDecimal("35.00"), ratio);
        }

        @Test
        @DisplayName("eGFR CKD-EPI 2021: calculates adult female eGFR accurately")
        void shouldCalculateFemaleEgfr() {
            CalculationContext context = CalculationContext.of(
                    Map.of("CREAT", new BigDecimal("0.8")),
                    Map.of("CREAT", "mg/dL"),
                    40,
                    Gender.FEMALE
            );
            BigDecimal egfr = calculationEngine.calculate(CalculationType.EGFR_CKD_EPI_2021, context);
            assertNotNull(egfr);
            assertEquals(new BigDecimal("95.5"), egfr);
        }

        @Test
        @DisplayName("eGFR CKD-EPI 2021: calculates adult male eGFR accurately")
        void shouldCalculateMaleEgfr() {
            CalculationContext context = CalculationContext.of(
                    Map.of("CREAT", new BigDecimal("1.2")),
                    Map.of("CREAT", "mg/dL"),
                    50,
                    Gender.MALE
            );
            BigDecimal egfr = calculationEngine.calculate(CalculationType.EGFR_CKD_EPI_2021, context);
            assertNotNull(egfr);
            assertEquals(new BigDecimal("73.7"), egfr);
        }

        @Test
        @DisplayName("eGFR CKD-EPI 2021: rejects pediatric age (< 18)")
        void shouldRejectPediatricAge() {
            CalculationContext context = CalculationContext.of(
                    Map.of("CREAT", new BigDecimal("0.7")),
                    Map.of(),
                    15,
                    Gender.FEMALE
            );
            CalculationException ex = assertThrows(
                    CalculationException.class,
                    () -> calculationEngine.calculate(CalculationType.EGFR_CKD_EPI_2021, context)
            );
            assertTrue(ex.getMessage().contains("adults aged 18 and older"));
        }

        @Test
        @DisplayName("eGFR CKD-EPI 2021: rejects missing demographics")
        void shouldRejectMissingDemographics() {
            CalculationContext noAge = CalculationContext.of(
                    Map.of("CREAT", new BigDecimal("0.9")),
                    Map.of(),
                    null,
                    Gender.MALE
            );
            assertThrows(CalculationException.class, () -> calculationEngine.calculate(CalculationType.EGFR_CKD_EPI_2021, noAge));

            CalculationContext noGender = CalculationContext.of(
                    Map.of("CREAT", new BigDecimal("0.9")),
                    Map.of(),
                    35,
                    null
            );
            assertThrows(CalculationException.class, () -> calculationEngine.calculate(CalculationType.EGFR_CKD_EPI_2021, noGender));
        }

        @Test
        @DisplayName("eGFR CKD-EPI 2021: normalizes umol/L creatinine to mg/dL")
        void shouldNormalizeUmolCreatinine() {
            // 88.4 umol/L = 1.0 mg/dL. 50yo Male with 88.4 umol/L:
            CalculationContext context = CalculationContext.of(
                    Map.of("CREAT", new BigDecimal("88.4")),
                    Map.of("CREAT", "umol/L"),
                    50,
                    Gender.MALE
            );
            BigDecimal egfr = calculationEngine.calculate(CalculationType.EGFR_CKD_EPI_2021, context);
            assertNotNull(egfr);
            assertTrue(egfr.compareTo(BigDecimal.ZERO) > 0);
        }
    }

    @Nested
    @DisplayName("Electrolytes Calculations")
    class ElectrolyteTests {

        @Test
        @DisplayName("Anion Gap: calculates Na - (Cl + HCO3)")
        void shouldCalculateAnionGap() {
            Map<String, BigDecimal> values = Map.of(
                    "NA", new BigDecimal("140.0"),
                    "CL", new BigDecimal("100.0"),
                    "HCO3", new BigDecimal("24.0")
            );
            BigDecimal ag = calculationEngine.calculate(CalculationType.ANION_GAP, values);
            assertEquals(new BigDecimal("16.0"), ag);
        }

        @Test
        @DisplayName("Anion Gap K: calculates (Na + K) - (Cl + HCO3)")
        void shouldCalculateAnionGapWithPotassium() {
            Map<String, BigDecimal> values = Map.of(
                    "NA", new BigDecimal("140.0"),
                    "K", new BigDecimal("4.5"),
                    "CL", new BigDecimal("100.0"),
                    "HCO3", new BigDecimal("24.0")
            );
            BigDecimal agk = calculationEngine.calculate(CalculationType.ANION_GAP_K, values);
            assertEquals(new BigDecimal("20.5"), agk);
        }
    }

    @Nested
    @DisplayName("CBC Aliases and Normalization")
    class CbcTests {

        @Test
        @DisplayName("MCV: accepts PCV as alias for HCT")
        void shouldAcceptPcvAliasForMcv() {
            Map<String, BigDecimal> values = Map.of(
                    "PCV", new BigDecimal("45.0"),
                    "RBC", new BigDecimal("5.0")
            );
            BigDecimal mcv = calculationEngine.calculate(CalculationType.MCV, values);
            assertEquals(new BigDecimal("90.0000"), mcv);
        }

        @Test
        @DisplayName("MCH: accepts HB as alias for HGB")
        void shouldAcceptHbAliasForMch() {
            Map<String, BigDecimal> values = Map.of(
                    "HB", new BigDecimal("15.0"),
                    "RBC", new BigDecimal("5.0")
            );
            BigDecimal mch = calculationEngine.calculate(CalculationType.MCH, values);
            assertEquals(new BigDecimal("30.0000"), mch);
        }

        @Test
        @DisplayName("MCHC: accepts HB and PCV aliases")
        void shouldAcceptAliasesForMchc() {
            Map<String, BigDecimal> values = Map.of(
                    "HB", new BigDecimal("15.0"),
                    "PCV", new BigDecimal("45.0")
            );
            BigDecimal mchc = calculationEngine.calculate(CalculationType.MCHC, values);
            assertEquals(new BigDecimal("33.3333"), mchc);
        }
    }
}
