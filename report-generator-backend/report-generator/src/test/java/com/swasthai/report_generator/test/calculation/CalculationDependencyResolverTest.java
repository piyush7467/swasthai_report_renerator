package com.swasthai.report_generator.test.calculation;

import com.swasthai.report_generator.test.calculation.calculators.*;
import com.swasthai.report_generator.test.calculation.impl.CalculationEngineImpl;
import com.swasthai.report_generator.test.entity.CalculationType;
import com.swasthai.report_generator.test.entity.ParameterInputType;
import com.swasthai.report_generator.test.entity.TestParameter;
import com.swasthai.report_generator.test.entity.TestParameterResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CalculationDependencyResolverTest {

    private CalculationEngine calculationEngine;

    @BeforeEach
    void setUp() {
        calculationEngine = new CalculationEngineImpl(List.of(
                new GlobulinCalculator(),
                new AGRatioCalculator(),
                new VLDLCalculator(),
                new LDLFriedewaldCalculator(),
                new LDLtoHDLratioCalculator(),
                new MCVCalculator(),
                new MCHCalculator(),
                new MCHCCalculator()
        ));
    }

    @Test
    @DisplayName("Should sort Globulin before A/G Ratio even when listed in reverse order")
    void shouldSortGlobulinBeforeAGRatio() {
        TestParameter agRatio = new TestParameter();
        agRatio.setCode("AG_RATIO");
        agRatio.setInputType(ParameterInputType.CALCULATED);
        agRatio.setCalculationType(CalculationType.AG_RATIO);

        TestParameter globulin = new TestParameter();
        globulin.setCode("GLOB");
        globulin.setInputType(ParameterInputType.CALCULATED);
        globulin.setCalculationType(CalculationType.GLOBULIN);

        TestParameter tp = new TestParameter();
        tp.setCode("TP");
        tp.setInputType(ParameterInputType.MANUAL);
        tp.setCalculationType(CalculationType.NONE);

        TestParameter alb = new TestParameter();
        alb.setCode("ALB");
        alb.setInputType(ParameterInputType.MANUAL);
        alb.setCalculationType(CalculationType.NONE);

        // Intentionally provide in reverse order: AG_RATIO first, then GLOBULIN
        List<TestParameter> params = List.of(tp, alb, agRatio, globulin);

        List<TestParameter> sorted = CalculationDependencyResolver.resolveParameterOrder(params, calculationEngine);

        assertEquals(2, sorted.size());
        assertEquals("GLOB", sorted.get(0).getCode());
        assertEquals("AG_RATIO", sorted.get(1).getCode());
    }

    @Test
    @DisplayName("Should sort TestParameterResult dependencies topologically")
    void shouldSortParameterResultsTopologically() {
        TestParameterResult ratioResult = TestParameterResult.builder()
                .parameterCode("LDL_HDL_RATIO")
                .inputType(ParameterInputType.CALCULATED)
                .calculationType(CalculationType.LDL_HDL_RATIO)
                .build();

        TestParameterResult ldlResult = TestParameterResult.builder()
                .parameterCode("LDL")
                .inputType(ParameterInputType.CALCULATED)
                .calculationType(CalculationType.LDL_FRIEDEWALD)
                .build();

        List<TestParameterResult> sorted = CalculationDependencyResolver.resolveResultOrder(
                List.of(ratioResult, ldlResult),
                calculationEngine
        );

        assertEquals(2, sorted.size());
        assertEquals("LDL", sorted.get(0).getParameterCode());
        assertEquals("LDL_HDL_RATIO", sorted.get(1).getParameterCode());
    }

    @Test
    @DisplayName("Should detect circular calculation dependency and throw CalculationException")
    void shouldDetectCircularDependency() {
        // Create a custom calculator with a circular dependency for testing
        ParameterCalculator calcA = new ParameterCalculator() {
            @Override public CalculationType supports() { return CalculationType.MCV; }
            @Override public java.util.Set<String> requiredParameters() { return java.util.Set.of("PARAM_B"); }
            @Override public java.math.BigDecimal calculate(java.util.Map<String, java.math.BigDecimal> v) { return java.math.BigDecimal.ONE; }
        };
        ParameterCalculator calcB = new ParameterCalculator() {
            @Override public CalculationType supports() { return CalculationType.MCH; }
            @Override public java.util.Set<String> requiredParameters() { return java.util.Set.of("PARAM_A"); }
            @Override public java.math.BigDecimal calculate(java.util.Map<String, java.math.BigDecimal> v) { return java.math.BigDecimal.ONE; }
        };
        CalculationEngine cyclicEngine = new CalculationEngineImpl(List.of(calcA, calcB));

        TestParameter paramA = new TestParameter();
        paramA.setCode("PARAM_A");
        paramA.setInputType(ParameterInputType.CALCULATED);
        paramA.setCalculationType(CalculationType.MCV);

        TestParameter paramB = new TestParameter();
        paramB.setCode("PARAM_B");
        paramB.setInputType(ParameterInputType.CALCULATED);
        paramB.setCalculationType(CalculationType.MCH);

        assertThrows(
                CalculationException.class,
                () -> CalculationDependencyResolver.resolveParameterOrder(List.of(paramA, paramB), cyclicEngine)
        );
    }
}
