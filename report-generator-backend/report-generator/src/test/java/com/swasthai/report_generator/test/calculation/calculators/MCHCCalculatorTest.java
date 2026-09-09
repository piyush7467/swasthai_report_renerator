package com.swasthai.report_generator.test.calculation.calculators;

import com.swasthai.report_generator.test.calculation.CalculationException;
import com.swasthai.report_generator.test.entity.CalculationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MCHCCalculatorTest {

    private MCHCCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new MCHCCalculator();
    }

    @Test
    @DisplayName("Calculate MCHC: (HGB * 100) / HCT with whole numbers")
    void shouldCalculateMCHC() {

        Map<String, BigDecimal> values = Map.of(
                "HGB", new BigDecimal("15"),
                "HCT", new BigDecimal("45")
        );

        BigDecimal result = calculator.calculate(values);

        assertEquals(
                0,
                result.compareTo(new BigDecimal("33.3333"))
        );
    }

    @Test
    @DisplayName("Calculate MCHC: with exact decimal values")
    void shouldCalculateMCHCWithDecimalValues() {

        Map<String, BigDecimal> values = Map.of(
                "HGB", new BigDecimal("14"),
                "HCT", new BigDecimal("40")
        );

        BigDecimal result = calculator.calculate(values);

        assertEquals(
                0,
                result.compareTo(new BigDecimal("35.0000"))
        );
    }

    @Test
    @DisplayName("Calculate MCHC: throws CalculationException when HGB is missing")
    void shouldThrowExceptionWhenHGBIsMissing() {

        Map<String, BigDecimal> values = Map.of(
                "HCT", new BigDecimal("45")
        );

        CalculationException exception = assertThrows(
                CalculationException.class,
                () -> calculator.calculate(values)
        );

        assertEquals(
                "Required parameter value is missing: HGB",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Calculate MCHC: throws CalculationException when HCT is missing")
    void shouldThrowExceptionWhenHCTIsMissing() {

        Map<String, BigDecimal> values = Map.of(
                "HGB", new BigDecimal("15")
        );

        CalculationException exception = assertThrows(
                CalculationException.class,
                () -> calculator.calculate(values)
        );

        assertEquals(
                "Required parameter value is missing: HCT",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Calculate MCHC: throws CalculationException when HCT is zero (division by zero)")
    void shouldThrowExceptionWhenHCTIsZero() {

        Map<String, BigDecimal> values = Map.of(
                "HGB", new BigDecimal("15"),
                "HCT", BigDecimal.ZERO
        );

        CalculationException exception = assertThrows(
                CalculationException.class,
                () -> calculator.calculate(values)
        );

        assertEquals(
                "Cannot calculate MCHC because HCT is zero",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Calculate MCHC: throws CalculationException when HGB is null")
    void shouldThrowExceptionWhenHGBIsNull() {
        Map<String, BigDecimal> values = new java.util.HashMap<>();
        values.put("HGB", null);
        values.put("HCT", new BigDecimal("45"));

        CalculationException exception = assertThrows(
                CalculationException.class,
                () -> calculator.calculate(values)
        );

        assertEquals(
                "Required parameter value is missing: HGB",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Calculate MCHC: throws CalculationException when HCT is null")
    void shouldThrowExceptionWhenHCTIsNull() {
        Map<String, BigDecimal> values = new java.util.HashMap<>();
        values.put("HGB", new BigDecimal("15"));
        values.put("HCT", null);

        CalculationException exception = assertThrows(
                CalculationException.class,
                () -> calculator.calculate(values)
        );

        assertEquals(
                "Required parameter value is missing: HCT",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Calculate MCHC: throws CalculationException when HGB is negative")
    void shouldThrowExceptionWhenHGBIsNegative() {
        Map<String, BigDecimal> values = Map.of(
                "HGB", new BigDecimal("-15"),
                "HCT", new BigDecimal("45")
        );

        CalculationException exception = assertThrows(
                CalculationException.class,
                () -> calculator.calculate(values)
        );

        assertEquals(
                "Parameter value cannot be negative: HGB",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Calculate MCHC: throws CalculationException when HCT is negative")
    void shouldThrowExceptionWhenHCTIsNegative() {
        Map<String, BigDecimal> values = Map.of(
                "HGB", new BigDecimal("15"),
                "HCT", new BigDecimal("-45")
        );

        CalculationException exception = assertThrows(
                CalculationException.class,
                () -> calculator.calculate(values)
        );

        assertEquals(
                "Parameter value cannot be negative: HCT",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Supports: returns CalculationType.MCHC")
    void shouldReturnCorrectSupportedCalculationType() {

        assertEquals(
                CalculationType.MCHC,
                calculator.supports()
        );
    }

    @Test
    @DisplayName("RequiredParameters: returns Set of HGB and HCT")
    void shouldReturnRequiredParameters() {

        assertEquals(
                Set.of("HGB", "HCT"),
                calculator.requiredParameters()
        );
    }
}
