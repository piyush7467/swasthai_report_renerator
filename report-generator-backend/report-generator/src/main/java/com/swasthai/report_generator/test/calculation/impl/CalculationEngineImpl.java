package com.swasthai.report_generator.test.calculation.impl;

import com.swasthai.report_generator.test.calculation.CalculationContext;
import com.swasthai.report_generator.test.calculation.CalculationEngine;
import com.swasthai.report_generator.test.calculation.CalculationException;
import com.swasthai.report_generator.test.calculation.ClinicalParameterAliases;
import com.swasthai.report_generator.test.calculation.ParameterCalculator;
import com.swasthai.report_generator.test.entity.CalculationType;
import com.swasthai.report_generator.test.entity.TestParameterDataType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Service
public class CalculationEngineImpl implements CalculationEngine {

    private final Map<CalculationType, ParameterCalculator> calculators;

    public CalculationEngineImpl(List<ParameterCalculator> calculatorList) {
        this.calculators = buildCalculatorRegistry(calculatorList);
    }

    @Override
    public BigDecimal calculate(
            CalculationType calculationType,
            Map<String, BigDecimal> values
    ) {
        if (values == null) {
            throw new CalculationException("Calculation values cannot be null");
        }
        return calculate(calculationType, CalculationContext.of(values));
    }

    @Override
    public BigDecimal calculate(
            CalculationType calculationType,
            CalculationContext context
    ) {
        if (calculationType == null) {
            throw new CalculationException("Calculation type cannot be null");
        }

        if (calculationType == CalculationType.NONE) {
            throw new CalculationException("Calculation type NONE cannot be calculated");
        }

        if (context == null) {
            throw new CalculationException("Calculation context cannot be null");
        }

        ParameterCalculator calculator = calculators.get(calculationType);

        if (calculator == null) {
            throw new CalculationException("No calculator registered for: " + calculationType);
        }

        validateRequiredParameters(calculator, context);

        return calculator.calculate(context);
    }

    private Map<CalculationType, ParameterCalculator> buildCalculatorRegistry(
            List<ParameterCalculator> calculatorList
    ) {
        if (calculatorList == null) {
            return Map.of();
        }

        Map<CalculationType, ParameterCalculator> registry =
                new EnumMap<>(CalculationType.class);

        for (ParameterCalculator calculator : calculatorList) {
            if (calculator == null) {
                throw new IllegalStateException("Calculator in registry cannot be null");
            }

            CalculationType calculationType = calculator.supports();

            if (calculationType == null) {
                throw new IllegalStateException(
                        "Calculator returned null CalculationType: " + calculator.getClass().getName()
                );
            }

            if (calculationType == CalculationType.NONE) {
                throw new IllegalStateException(
                        "Calculator cannot support CalculationType.NONE: " + calculator.getClass().getName()
                );
            }

            if (registry.containsKey(calculationType)) {
                throw new IllegalStateException(
                        "Duplicate calculator registered for: " + calculationType
                );
            }

            registry.put(calculationType, calculator);
        }

        return Map.copyOf(registry);
    }

    @Override
    public boolean isSupported(CalculationType calculationType) {
        if (calculationType == null || calculationType == CalculationType.NONE) {
            return false;
        }
        return calculators.containsKey(calculationType);
    }

    @Override
    public Set<String> getRequiredParameters(CalculationType calculationType) {
        if (calculationType == null || calculationType == CalculationType.NONE) {
            return Set.of();
        }
        ParameterCalculator calculator = calculators.get(calculationType);
        if (calculator == null) {
            throw new CalculationException("No calculator registered for: " + calculationType);
        }
        return calculator.requiredParameters();
    }

    @Override
    public Set<String> getRequiredParameters(
            CalculationType calculationType,
            Set<String> availableCodes
    ) {
        Set<String> baseRequired = getRequiredParameters(calculationType);
        if (baseRequired.isEmpty() || availableCodes == null || availableCodes.isEmpty()) {
            return baseRequired;
        }

        Set<String> resolved = new LinkedHashSet<>();
        for (String req : baseRequired) {
            Optional<String> matched = ClinicalParameterAliases.resolveAvailableCode(req, availableCodes);
            resolved.add(matched.orElse(req));
        }
        return Collections.unmodifiableSet(resolved);
    }

    @Override
    public boolean isResultDataTypeSupported(
            CalculationType calculationType,
            TestParameterDataType dataType
    ) {
        if (calculationType == null || calculationType == CalculationType.NONE || dataType == null) {
            return false;
        }
        ParameterCalculator calculator = calculators.get(calculationType);
        if (calculator == null) {
            return false;
        }
        return calculator.isResultDataTypeSupported(dataType);
    }

    private void validateRequiredParameters(
            ParameterCalculator calculator,
            CalculationContext context
    ) {
        for (String parameterCode : calculator.requiredParameters()) {
            if (!context.containsKey(parameterCode)) {
                throw new CalculationException(
                        "Required parameter is missing: " + parameterCode
                );
            }
            if (context.getValue(parameterCode) == null) {
                throw new CalculationException(
                        "Required parameter value is null: " + parameterCode
                );
            }
        }
    }
}