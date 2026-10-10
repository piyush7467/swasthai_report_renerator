package com.swasthai.report_generator.test.calculation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CbcDifferentialValidatorTest {

    private final CbcDifferentialValidator defaultValidator = new CbcDifferentialValidator();

    @Test
    @DisplayName("DLC-VAL-001: Should automatically calculate MONO and set BASO to 0 when 3 manual percentages are entered")
    void shouldCalculateDifferentialWhenThreeManualPercentagesProvided() {
        // NEUT: 68.0%, LYMPH: 12.0%, EOS: 8.0%
        // Expected MONO: 100 - 68 - 12 - 8 = 12.0%
        // Expected BASO: 0.0%
        // Total: 68 + 12 + 12 + 8 + 0 = 100.0%
        Map<String, BigDecimal> values = new HashMap<>();
        values.put("NEUT", new BigDecimal("68.0"));
        values.put("LYMPH", new BigDecimal("12.0"));
        values.put("EOS", new BigDecimal("8.0"));

        CbcDifferentialValidator.DifferentialCalculationResult result =
                defaultValidator.calculateAndValidateDifferential(values);

        assertThat(result.applied()).isTrue();
        assertThat(result.neut()).isEqualByComparingTo("68.0");
        assertThat(result.lymph()).isEqualByComparingTo("12.0");
        assertThat(result.eos()).isEqualByComparingTo("8.0");
        assertThat(result.mono()).isEqualByComparingTo("12.0");
        assertThat(result.baso()).isEqualByComparingTo("0.0");
        assertThat(result.totalSum()).isEqualByComparingTo("100.0");

        assertThat(result.differentialValues())
                .containsEntry("NEUT", new BigDecimal("68.0"))
                .containsEntry("LYMPH", new BigDecimal("12.0"))
                .containsEntry("EOS", new BigDecimal("8.0"))
                .containsKey("MONO")
                .containsKey("BASO");
        assertThat(result.differentialValues().get("MONO")).isEqualByComparingTo("12.0");
        assertThat(result.differentialValues().get("BASO")).isEqualByComparingTo("0.0");
    }

    @Test
    @DisplayName("DLC-VAL-002: Should handle boundary condition where MONO calculates to exactly 0.0%")
    void shouldAcceptBoundaryConditionWhereMonoCalculatesToZero() {
        // NEUT: 70.0%, LYMPH: 20.0%, EOS: 10.0% -> Sum = 100.0%
        // MONO: 100 - 70 - 20 - 10 = 0.0%
        Map<String, BigDecimal> values = Map.of(
                "NEUT", new BigDecimal("70.0"),
                "LYMPH", new BigDecimal("20.0"),
                "EOS", new BigDecimal("10.0")
        );

        CbcDifferentialValidator.DifferentialCalculationResult result =
                defaultValidator.calculateAndValidateDifferential(values);

        assertThat(result.applied()).isTrue();
        assertThat(result.mono()).isEqualByComparingTo("0.0");
        assertThat(result.baso()).isEqualByComparingTo("0.0");
        assertThat(result.totalSum()).isEqualByComparingTo("100.0");
    }

    @Test
    @DisplayName("DLC-VAL-003: Should reject when manual inputs sum exceeds 100% (negative MONO); must not silently clamp to zero")
    void shouldRejectWhenManualPercentagesExceedHundredPercent() {
        // NEUT: 70.0%, LYMPH: 25.0%, EOS: 10.0% -> Sum = 105.0%
        // MONO: 100 - 105 = -5.0% -> REJECT
        Map<String, BigDecimal> values = Map.of(
                "NEUT", new BigDecimal("70.0"),
                "LYMPH", new BigDecimal("25.0"),
                "EOS", new BigDecimal("10.0")
        );

        assertThatThrownBy(() -> defaultValidator.calculateAndValidateDifferential(values))
                .isInstanceOf(CbcDifferentialValidationException.class)
                .hasMessageContaining("Calculated Monocytes percentage is negative (-5%)")
                .hasMessageContaining("Sum of manual differential percentages (105%) exceeds 100.0%")
                .hasMessageContaining("Values must not be silently adjusted");
    }

    @Test
    @DisplayName("DLC-VAL-004: Should accept client request supplying matching non-conflicting MONO and BASO")
    void shouldAcceptMatchingClientSuppliedMonoAndBaso() {
        Map<String, BigDecimal> values = Map.of(
                "NEUT", new BigDecimal("68.0"),
                "LYMPH", new BigDecimal("12.0"),
                "EOS", new BigDecimal("8.0"),
                "MONO", new BigDecimal("12.0"),
                "BASO", new BigDecimal("0.0")
        );

        CbcDifferentialValidator.DifferentialCalculationResult result =
                defaultValidator.calculateAndValidateDifferential(values);

        assertThat(result.applied()).isTrue();
        assertThat(result.mono()).isEqualByComparingTo("12.0");
        assertThat(result.baso()).isEqualByComparingTo("0.0");
        assertThat(result.totalSum()).isEqualByComparingTo("100.0");
    }

    @Test
    @DisplayName("DLC-VAL-005: Should reject client request supplying conflicting MONO value")
    void shouldRejectConflictingClientSuppliedMono() {
        // Calculated MONO would be 12.0%, but client supplied 15.0%
        Map<String, BigDecimal> values = Map.of(
                "NEUT", new BigDecimal("68.0"),
                "LYMPH", new BigDecimal("12.0"),
                "EOS", new BigDecimal("8.0"),
                "MONO", new BigDecimal("15.0")
        );

        assertThatThrownBy(() -> defaultValidator.calculateAndValidateDifferential(values))
                .isInstanceOf(CbcDifferentialValidationException.class)
                .hasMessageContaining("Conflicting value supplied for calculated parameter MONO")
                .hasMessageContaining("provided 15%")
                .hasMessageContaining("does not match calculated 12%");
    }

    @Test
    @DisplayName("DLC-VAL-006: Should reject client request supplying conflicting non-zero BASO value")
    void shouldRejectConflictingClientSuppliedBaso() {
        // BASO must be automatically set to 0.0%, client supplies 2.0%
        Map<String, BigDecimal> values = Map.of(
                "NEUT", new BigDecimal("68.0"),
                "LYMPH", new BigDecimal("12.0"),
                "EOS", new BigDecimal("8.0"),
                "BASO", new BigDecimal("2.0")
        );

        assertThatThrownBy(() -> defaultValidator.calculateAndValidateDifferential(values))
                .isInstanceOf(CbcDifferentialValidationException.class)
                .hasMessageContaining("Conflicting value supplied for calculated parameter BASO")
                .hasMessageContaining("expected 0.0%")
                .hasMessageContaining("got 2%");
    }

    @Test
    @DisplayName("DLC-VAL-007: Should reject incomplete differential when EOS is missing")
    void shouldRejectIncompleteDifferentialMissingEos() {
        Map<String, BigDecimal> values = Map.of(
                "NEUT", new BigDecimal("68.0"),
                "LYMPH", new BigDecimal("12.0")
        );

        assertThatThrownBy(() -> defaultValidator.calculateAndValidateDifferential(values))
                .isInstanceOf(CbcDifferentialValidationException.class)
                .hasMessageContaining("Differential leukocyte count requires manual entry of Neutrophils (NEUT), Lymphocytes (LYMPH), and Eosinophils (EOS)")
                .hasMessageContaining("Missing: [EOS]");
    }

    @Test
    @DisplayName("DLC-VAL-008: Should reject incomplete differential when LYMPH is missing")
    void shouldRejectIncompleteDifferentialMissingLymph() {
        Map<String, BigDecimal> values = Map.of(
                "NEUT", new BigDecimal("68.0"),
                "EOS", new BigDecimal("8.0")
        );

        assertThatThrownBy(() -> defaultValidator.calculateAndValidateDifferential(values))
                .isInstanceOf(CbcDifferentialValidationException.class)
                .hasMessageContaining("Missing: [LYMPH]");
    }

    @Test
    @DisplayName("DLC-VAL-009: Should reject incomplete differential when only one parameter is provided")
    void shouldRejectIncompleteDifferentialWhenOnlyOneParameterPresent() {
        Map<String, BigDecimal> values = Map.of(
                "NEUT", new BigDecimal("60.0")
        );

        assertThatThrownBy(() -> defaultValidator.calculateAndValidateDifferential(values))
                .isInstanceOf(CbcDifferentialValidationException.class)
                .hasMessageContaining("Differential leukocyte count requires manual entry")
                .hasMessageContaining("LYMPH")
                .hasMessageContaining("EOS");
    }

    @Test
    @DisplayName("DLC-VAL-010: Should allow CBC without differential (0 differential parameters provided)")
    void shouldPermitZeroDifferentialParametersForBasicCbcWithoutDifferential() {
        Map<String, BigDecimal> basicCbcValues = Map.of(
                "HGB", new BigDecimal("14.5"),
                "RBC", new BigDecimal("4.8"),
                "HCT", new BigDecimal("43.0"),
                "WBC", new BigDecimal("7.2"),
                "PLT", new BigDecimal("250.0")
        );

        CbcDifferentialValidator.DifferentialCalculationResult result =
                defaultValidator.calculateAndValidateDifferential(basicCbcValues);

        assertThat(result.applied()).isFalse();
        assertThatCode(() -> defaultValidator.validate(basicCbcValues))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("DLC-VAL-011: Should reject negative differential parameter percentage")
    void shouldRejectNegativeDifferentialParameter() {
        Map<String, BigDecimal> values = Map.of(
                "NEUT", new BigDecimal("-5.0"),
                "LYMPH", new BigDecimal("75.0"),
                "EOS", new BigDecimal("10.0")
        );

        assertThatThrownBy(() -> defaultValidator.calculateAndValidateDifferential(values))
                .isInstanceOf(CbcDifferentialValidationException.class)
                .hasMessageContaining("NEUT cannot be negative: -5%");
    }

    @Test
    @DisplayName("DLC-VAL-012: Should reject differential parameter percentage exceeding 100%")
    void shouldRejectDifferentialParameterExceedingHundred() {
        Map<String, BigDecimal> values = Map.of(
                "NEUT", new BigDecimal("105.0"),
                "LYMPH", new BigDecimal("0.0"),
                "EOS", new BigDecimal("0.0")
        );

        assertThatThrownBy(() -> defaultValidator.calculateAndValidateDifferential(values))
                .isInstanceOf(CbcDifferentialValidationException.class)
                .hasMessageContaining("NEUT cannot exceed 100%: 105%");
    }

    @Test
    @DisplayName("DLC-VAL-013: Should resolve aliases and case insensitivity (e.g. neutrophils, LYMPHOCYTES, eosinophils)")
    void shouldResolveAliasesAndCaseInsensitivity() {
        Map<String, BigDecimal> aliasedValues = Map.of(
                "neutrophils", new BigDecimal("68.0"),
                "LYMPHOCYTES", new BigDecimal("12.0"),
                "eosinophils", new BigDecimal("8.0")
        );

        CbcDifferentialValidator.DifferentialCalculationResult result =
                defaultValidator.calculateAndValidateDifferential(aliasedValues);

        assertThat(result.applied()).isTrue();
        assertThat(result.neut()).isEqualByComparingTo("68.0");
        assertThat(result.lymph()).isEqualByComparingTo("12.0");
        assertThat(result.eos()).isEqualByComparingTo("8.0");
        assertThat(result.mono()).isEqualByComparingTo("12.0");
        assertThat(result.baso()).isEqualByComparingTo("0.0");
        assertThat(result.totalSum()).isEqualByComparingTo("100.0");
    }

    @Test
    @DisplayName("DLC-VAL-014: Should isolate non-CBC tests from differential workflow")
    void shouldIsolateNonCbcTests() {
        Map<String, BigDecimal> lftValues = Map.of(
                "NEUT", new BigDecimal("68.0"),
                "LYMPH", new BigDecimal("12.0"),
                "EOS", new BigDecimal("8.0")
        );

        // For non-CBC tests like LFT, processDifferentialForTest must return notApplied()
        CbcDifferentialValidator.DifferentialCalculationResult result =
                defaultValidator.processDifferentialForTest("LFT", lftValues);

        assertThat(result.applied()).isFalse();
    }

    @Test
    @DisplayName("DLC-VAL-015: Should never alter original input map or distribute residuals across parameters")
    void shouldNotAlterInputMapOrDistributeResiduals() {
        Map<String, BigDecimal> original = new HashMap<>();
        original.put("NEUT", new BigDecimal("68.0"));
        original.put("LYMPH", new BigDecimal("12.0"));
        original.put("EOS", new BigDecimal("8.0"));

        Map<String, BigDecimal> snapshot = new HashMap<>(original);

        defaultValidator.calculateAndValidateDifferential(original);

        // Input map must remain completely identical
        assertThat(original).isEqualTo(snapshot);
    }

    @Test
    @DisplayName("DLC-VAL-016: Total sum of the 5 differential parameters must equal exactly 100.0%")
    void shouldEnsureExactSumOfHundredPercent() {
        Map<String, BigDecimal> values = Map.of(
                "NEUT", new BigDecimal("55.5"),
                "LYMPH", new BigDecimal("32.3"),
                "EOS", new BigDecimal("4.2")
        );
        // MONO: 100 - 55.5 - 32.3 - 4.2 = 8.0
        // BASO: 0.0
        // Sum: 55.5 + 32.3 + 8.0 + 4.2 + 0.0 = 100.0

        CbcDifferentialValidator.DifferentialCalculationResult result =
                defaultValidator.calculateAndValidateDifferential(values);

        assertThat(result.applied()).isTrue();
        assertThat(result.mono()).isEqualByComparingTo("8.0");
        assertThat(result.baso()).isEqualByComparingTo("0.0");
        assertThat(result.totalSum()).isEqualByComparingTo("100.0");
    }
}
