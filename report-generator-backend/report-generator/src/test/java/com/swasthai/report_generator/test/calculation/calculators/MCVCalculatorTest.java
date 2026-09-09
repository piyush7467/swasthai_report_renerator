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

class MCVCalculatorTest {

    private MCVCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new MCVCalculator();
    }

    @Test
    @DisplayName("Calculate MCV: (HCT / RBC) * 10 with whole numbers")
    void shouldCalculateMCV() {

        Map<String, BigDecimal> values = Map.of(
                "HCT", new BigDecimal("40"),
                "RBC", new BigDecimal("5")
        );

        BigDecimal result = calculator.calculate(values);

        assertEquals(
                0,
                result.compareTo(new BigDecimal("80"))
        );
    }

    @Test
    @DisplayName("Calculate MCV: with decimal values")
    void shouldCalculateMCVWithDecimalValues() {

        Map<String, BigDecimal> values = Map.of(
                "HCT", new BigDecimal("42.5"),
                "RBC", new BigDecimal("4.25")
        );

        BigDecimal result = calculator.calculate(values);

        assertEquals(
                0,
                result.compareTo(new BigDecimal("100"))
        );
    }

    @Test
    @DisplayName("Calculate MCV: throws CalculationException when HCT is missing")
    void shouldThrowExceptionWhenHCTIsMissing() {

        Map<String, BigDecimal> values = Map.of(
                "RBC", new BigDecimal("5")
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
    @DisplayName("Calculate MCV: throws CalculationException when RBC is missing")
    void shouldThrowExceptionWhenRBCIsMissing() {

        Map<String, BigDecimal> values = Map.of(
                "HCT", new BigDecimal("40")
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
    @DisplayName("Calculate MCV: throws CalculationException when RBC is zero (division by zero)")
    void shouldThrowExceptionWhenRBCIsZero() {

        Map<String, BigDecimal> values = Map.of(
                "HCT", new BigDecimal("40"),
                "RBC", BigDecimal.ZERO
        );

        CalculationException exception = assertThrows(
                CalculationException.class,
                () -> calculator.calculate(values)
        );

        assertEquals(
                "Cannot calculate MCV because RBC is zero",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Calculate MCV: throws CalculationException when HCT is null")
    void shouldThrowExceptionWhenHCTIsNull() {
        Map<String, BigDecimal> values = new java.util.HashMap<>();
        values.put("HCT", null);
        values.put("RBC", new BigDecimal("5"));

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
    @DisplayName("Calculate MCV: throws CalculationException when RBC is null")
    void shouldThrowExceptionWhenRBCIsNull() {
        Map<String, BigDecimal> values = new java.util.HashMap<>();
        values.put("HCT", new BigDecimal("40"));
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
    @DisplayName("Calculate MCV: throws CalculationException when HCT is negative")
    void shouldThrowExceptionWhenHCTIsNegative() {
        Map<String, BigDecimal> values = Map.of(
                "HCT", new BigDecimal("-40"),
                "RBC", new BigDecimal("5")
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
    @DisplayName("Calculate MCV: throws CalculationException when RBC is negative")
    void shouldThrowExceptionWhenRBCIsNegative() {
        Map<String, BigDecimal> values = Map.of(
                "HCT", new BigDecimal("40"),
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
    @DisplayName("Supports: returns CalculationType.MCV")
    void shouldReturnCorrectSupportedCalculationType() {

        assertEquals(
                CalculationType.MCV,
                calculator.supports()
        );
    }

    @Test
    @DisplayName("RequiredParameters: returns Set of HCT and RBC")
    void shouldReturnRequiredParameters() {

        assertEquals(
                Set.of("HCT", "RBC"),
                calculator.requiredParameters()
        );
    }
}
