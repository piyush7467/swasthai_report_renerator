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

class MCHCalculatorTest {

    private MCHCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new MCHCalculator();
    }

    @Test
    @DisplayName("Calculate MCH: (HGB / RBC) * 10 with whole numbers")
    void shouldCalculateMCH() {

        Map<String, BigDecimal> values = Map.of(
                "HGB", new BigDecimal("15"),
                "RBC", new BigDecimal("5")
        );

        BigDecimal result = calculator.calculate(values);

        assertEquals(
                0,
                result.compareTo(new BigDecimal("30"))
        );
    }

    @Test
    @DisplayName("Calculate MCH: with decimal values")
    void shouldCalculateMCHWithDecimalValues() {

        Map<String, BigDecimal> values = Map.of(
                "HGB", new BigDecimal("14.5"),
                "RBC", new BigDecimal("4.5")
        );

        BigDecimal result = calculator.calculate(values);

        assertEquals(
                0,
                result.compareTo(
                        new BigDecimal("32.2222")
                )
        );
    }

    @Test
    @DisplayName("Calculate MCH: throws CalculationException when HGB is missing")
    void shouldThrowExceptionWhenHGBIsMissing() {

        Map<String, BigDecimal> values = Map.of(
                "RBC", new BigDecimal("5")
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
    @DisplayName("Calculate MCH: throws CalculationException when RBC is missing")
    void shouldThrowExceptionWhenRBCIsMissing() {

        Map<String, BigDecimal> values = Map.of(
                "HGB", new BigDecimal("15")
        );

        CalculationException exception = assertThrows(
                CalculationException.class,
                () -> calculator.calculate(values)
        );

        assertEquals(
                "Required parameter value is missing: RBC",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Calculate MCH: throws CalculationException when RBC is zero (division by zero)")
    void shouldThrowExceptionWhenRBCIsZero() {

        Map<String, BigDecimal> values = Map.of(
                "HGB", new BigDecimal("15"),
                "RBC", BigDecimal.ZERO
        );

        CalculationException exception = assertThrows(
                CalculationException.class,
                () -> calculator.calculate(values)
        );

        assertEquals(
                "Cannot calculate MCH because RBC is zero",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Calculate MCH: throws CalculationException when HGB is null")
    void shouldThrowExceptionWhenHGBIsNull() {
        Map<String, BigDecimal> values = new java.util.HashMap<>();
        values.put("HGB", null);
        values.put("RBC", new BigDecimal("5"));

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
    @DisplayName("Calculate MCH: throws CalculationException when RBC is null")
    void shouldThrowExceptionWhenRBCIsNull() {
        Map<String, BigDecimal> values = new java.util.HashMap<>();
        values.put("HGB", new BigDecimal("15"));
        values.put("RBC", null);

        CalculationException exception = assertThrows(
                CalculationException.class,
                () -> calculator.calculate(values)
        );

        assertEquals(
                "Required parameter value is missing: RBC",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Calculate MCH: throws CalculationException when HGB is negative")
    void shouldThrowExceptionWhenHGBIsNegative() {
        Map<String, BigDecimal> values = Map.of(
                "HGB", new BigDecimal("-15"),
                "RBC", new BigDecimal("5")
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
    @DisplayName("Calculate MCH: throws CalculationException when RBC is negative")
    void shouldThrowExceptionWhenRBCIsNegative() {
        Map<String, BigDecimal> values = Map.of(
                "HGB", new BigDecimal("15"),
                "RBC", new BigDecimal("-5")
        );

        CalculationException exception = assertThrows(
                CalculationException.class,
                () -> calculator.calculate(values)
        );

        assertEquals(
                "Parameter value cannot be negative: RBC",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Supports: returns CalculationType.MCH")
    void shouldReturnCorrectSupportedCalculationType() {

        assertEquals(
                CalculationType.MCH,
                calculator.supports()
        );
    }

    @Test
    @DisplayName("RequiredParameters: returns Set of HGB and RBC")
    void shouldReturnRequiredParameters() {

        assertEquals(
                Set.of("HGB", "RBC"),
                calculator.requiredParameters()
        );
    }
}
