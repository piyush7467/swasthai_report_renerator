package com.swasthai.report_generator.test.calculation.calculators;

import com.swasthai.report_generator.test.calculation.CalculationException;
import com.swasthai.report_generator.test.calculation.ParameterCalculator;
import com.swasthai.report_generator.test.calculation.impl.CalculationEngineImpl;
import com.swasthai.report_generator.test.entity.CalculationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CalculationEngineImplTest {

    private CalculationEngineImpl calculationEngine;

    @BeforeEach
    void setUp() {
        calculationEngine = new CalculationEngineImpl(List.of(
                new MCVCalculator(),
                new MCHCalculator(),
                new MCHCCalculator()
        ));
    }

    @Test
    @DisplayName("Calculate: MCV with valid parameters")
    void shouldCalculateMCVSuccessfully() {
        Map<String, BigDecimal> values = Map.of(
                "HCT", new BigDecimal("45"),
                "RBC", new BigDecimal("5")
        );

        BigDecimal result = calculationEngine.calculate(CalculationType.MCV, values);

        assertNotNull(result);
        assertEquals(0, result.compareTo(new BigDecimal("90")));
    }

    @Test
    @DisplayName("Calculate: MCH with valid parameters")
    void shouldCalculateMCHSuccessfully() {
        Map<String, BigDecimal> values = Map.of(
                "HGB", new BigDecimal("15"),
                "RBC", new BigDecimal("5")
        );

        BigDecimal result = calculationEngine.calculate(CalculationType.MCH, values);

        assertNotNull(result);
        assertEquals(0, result.compareTo(new BigDecimal("30")));
    }

    @Test
    @DisplayName("Calculate: MCHC with valid parameters")
    void shouldCalculateMCHCSuccessfully() {
        Map<String, BigDecimal> values = Map.of(
                "HGB", new BigDecimal("15"),
                "HCT", new BigDecimal("45")
        );

        BigDecimal result = calculationEngine.calculate(CalculationType.MCHC, values);

        assertNotNull(result);
        assertEquals(0, result.compareTo(new BigDecimal("33.3333")));
    }

    @Test
    @DisplayName("Calculate: throws exception when calculationType is null")
    void shouldThrowExceptionWhenCalculationTypeIsNull() {
        Map<String, BigDecimal> values = Map.of("HCT", new BigDecimal("40"));

        CalculationException exception = assertThrows(
                CalculationException.class,
                () -> calculationEngine.calculate(null, values)
        );

        assertEquals("Calculation type cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("Calculate: throws exception when calculationType is NONE")
    void shouldThrowExceptionWhenCalculationTypeIsNone() {
        Map<String, BigDecimal> values = Map.of("HCT", new BigDecimal("40"));

        CalculationException exception = assertThrows(
                CalculationException.class,
                () -> calculationEngine.calculate(CalculationType.NONE, values)
        );

        assertEquals("Calculation type NONE cannot be calculated", exception.getMessage());
    }

    @Test
    @DisplayName("Calculate: throws exception when values map is null")
    void shouldThrowExceptionWhenValuesNull() {
        CalculationException exception = assertThrows(
                CalculationException.class,
                () -> calculationEngine.calculate(CalculationType.MCV, null)
        );

        assertEquals("Calculation values cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("Calculate: throws exception when required parameter is missing")
    void shouldThrowExceptionWhenRequiredParameterMissing() {
        Map<String, BigDecimal> values = Map.of("HCT", new BigDecimal("40"));

        CalculationException exception = assertThrows(
                CalculationException.class,
                () -> calculationEngine.calculate(CalculationType.MCV, values)
        );

        assertTrue(exception.getMessage().contains("Required parameter is missing: RBC"));
    }

    @Test
    @DisplayName("Calculate: throws exception when required parameter is null")
    void shouldThrowExceptionWhenRequiredParameterIsNull() {
        Map<String, BigDecimal> values = new java.util.HashMap<>();
        values.put("HCT", new BigDecimal("40"));
        values.put("RBC", null);

        CalculationException exception = assertThrows(
                CalculationException.class,
                () -> calculationEngine.calculate(CalculationType.MCV, values)
        );

        assertTrue(exception.getMessage().contains("Required parameter value is null: RBC"));
    }

    @Test
    @DisplayName("Registry: throws IllegalStateException on duplicate calculator registration")
    void shouldThrowExceptionOnDuplicateCalculatorRegistration() {
        assertThrows(
                IllegalStateException.class,
                () -> new CalculationEngineImpl(List.of(new MCVCalculator(), new MCVCalculator()))
        );
    }

    @Test
    @DisplayName("Registry: throws IllegalStateException when calculator supports NONE")
    void shouldThrowExceptionWhenCalculatorSupportsNone() {
        ParameterCalculator noneCalculator = new ParameterCalculator() {
            @Override
            public CalculationType supports() {
                return CalculationType.NONE;
            }

            @Override
            public Set<String> requiredParameters() {
                return Set.of();
            }

            @Override
            public BigDecimal calculate(Map<String, BigDecimal> values) {
                return BigDecimal.ZERO;
            }
        };

        assertThrows(
                IllegalStateException.class,
                () -> new CalculationEngineImpl(List.of(noneCalculator))
        );
    }

    @Test
    @DisplayName("Registry: throws IllegalStateException when calculator supports null")
    void shouldThrowExceptionWhenCalculatorSupportsNull() {
        ParameterCalculator nullCalculator = new ParameterCalculator() {
            @Override
            public CalculationType supports() {
                return null;
            }

            @Override
            public Set<String> requiredParameters() {
                return Set.of();
            }

            @Override
            public BigDecimal calculate(Map<String, BigDecimal> values) {
                return BigDecimal.ZERO;
            }
        };

        assertThrows(
                IllegalStateException.class,
                () -> new CalculationEngineImpl(List.of(nullCalculator))
        );
    }

    @Test
    @DisplayName("Registry: throws CalculationException when no calculator is registered for calculation type")
    void shouldThrowExceptionWhenNoCalculatorRegistered() {
        CalculationEngineImpl engine = new CalculationEngineImpl(List.of(new MCVCalculator()));

        Map<String, BigDecimal> values = Map.of(
                "HGB", new BigDecimal("15"),
                "RBC", new BigDecimal("5")
        );

        CalculationException exception = assertThrows(
                CalculationException.class,
                () -> engine.calculate(CalculationType.MCH, values)
        );

        assertEquals("No calculator registered for: MCH", exception.getMessage());
    }

    @Test
    @DisplayName("Registry: isSupported correctly identifies registered calculation types")
    void shouldVerifySupportedCalculationTypes() {
        assertTrue(calculationEngine.isSupported(CalculationType.MCV));
        assertTrue(calculationEngine.isSupported(CalculationType.MCH));
        assertTrue(calculationEngine.isSupported(CalculationType.MCHC));
        assertFalse(calculationEngine.isSupported(CalculationType.NONE));
        assertFalse(calculationEngine.isSupported(null));
    }

    @Test
    @DisplayName("Registry: getRequiredParameters returns expected sets")
    void shouldReturnRequiredParametersForCalculationType() {
        assertEquals(Set.of("HCT", "RBC"), calculationEngine.getRequiredParameters(CalculationType.MCV));
        assertEquals(Set.of("HGB", "RBC"), calculationEngine.getRequiredParameters(CalculationType.MCH));
        assertEquals(Set.of("HGB", "HCT"), calculationEngine.getRequiredParameters(CalculationType.MCHC));
    }

    @Test
    @DisplayName("Registry: isResultDataTypeSupported validates data types correctly")
    void shouldValidateResultDataTypes() {
        assertTrue(calculationEngine.isResultDataTypeSupported(
                CalculationType.MCV,
                com.swasthai.report_generator.test.entity.TestParameterDataType.DECIMAL
        ));
        assertFalse(calculationEngine.isResultDataTypeSupported(
                CalculationType.MCV,
                com.swasthai.report_generator.test.entity.TestParameterDataType.TEXT
        ));
    }
}
