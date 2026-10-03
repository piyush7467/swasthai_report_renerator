package com.swasthai.report_generator.test.calculation;

import com.swasthai.report_generator.test.entity.CalculationType;
import com.swasthai.report_generator.test.entity.ParameterInputType;
import com.swasthai.report_generator.test.entity.TestParameter;
import com.swasthai.report_generator.test.entity.TestParameterResult;

import java.util.*;

/**
 * Resolves calculation dependencies among test parameters and orders calculations topologically.
 * Detects circular dependencies and ensures upstream calculations (e.g. Globulin) complete
 * before downstream dependent calculations (e.g. A/G Ratio).
 */
public final class CalculationDependencyResolver {

    private CalculationDependencyResolver() {}

    /**
     * Sorts calculated TestParameters in topological execution order.
     */
    public static List<TestParameter> resolveParameterOrder(
            List<TestParameter> parameters,
            CalculationEngine calculationEngine
    ) {
        if (parameters == null || parameters.isEmpty()) {
            return Collections.emptyList();
        }

        List<TestParameter> calculated = new ArrayList<>();
        Map<String, TestParameter> calcByCode = new HashMap<>();

        for (TestParameter p : parameters) {
            if (p == null) continue;
            CalculationType effectiveType = ClinicalParameterAliases.resolveCalculationType(
                    p.getCode(),
                    p.getCalculationType()
            );
            if ((p.getInputType() == ParameterInputType.CALCULATED || effectiveType != CalculationType.NONE)
                    && effectiveType != CalculationType.NONE) {
                calculated.add(p);
                calcByCode.put(p.getCode().trim().toUpperCase(), p);
                // Also index by canonical code
                String canon = ClinicalParameterAliases.getCanonical(p.getCode());
                calcByCode.putIfAbsent(canon, p);
            }
        }

        if (calculated.size() <= 1) {
            return calculated;
        }

        return topologicalSort(
                calculated,
                calcByCode,
                calculationEngine,
                p -> ClinicalParameterAliases.resolveCalculationType(p.getCode(), p.getCalculationType()),
                TestParameter::getCode
        );
    }

    /**
     * Sorts calculated TestParameterResults in topological execution order.
     */
    public static List<TestParameterResult> resolveResultOrder(
            List<TestParameterResult> results,
            CalculationEngine calculationEngine
    ) {
        if (results == null || results.isEmpty()) {
            return Collections.emptyList();
        }

        List<TestParameterResult> calculated = new ArrayList<>();
        Map<String, TestParameterResult> calcByCode = new HashMap<>();

        for (TestParameterResult r : results) {
            if (r == null) continue;
            CalculationType effectiveType = ClinicalParameterAliases.resolveCalculationType(
                    r.getParameterCode(),
                    r.getCalculationType()
            );
            if ((r.getInputType() == ParameterInputType.CALCULATED || effectiveType != CalculationType.NONE)
                    && effectiveType != CalculationType.NONE) {
                calculated.add(r);
                if (r.getParameterCode() != null) {
                    calcByCode.put(r.getParameterCode().trim().toUpperCase(), r);
                    String canon = ClinicalParameterAliases.getCanonical(r.getParameterCode());
                    calcByCode.putIfAbsent(canon, r);
                }
            }
        }

        if (calculated.size() <= 1) {
            return calculated;
        }

        return topologicalSort(
                calculated,
                calcByCode,
                calculationEngine,
                r -> ClinicalParameterAliases.resolveCalculationType(r.getParameterCode(), r.getCalculationType()),
                TestParameterResult::getParameterCode
        );
    }

    private static <T> List<T> topologicalSort(
            List<T> items,
            Map<String, T> calcByCode,
            CalculationEngine calculationEngine,
            java.util.function.Function<T, CalculationType> typeExtractor,
            java.util.function.Function<T, String> codeExtractor
    ) {
        Map<T, Set<T>> graph = new HashMap<>();
        Map<T, Integer> inDegree = new HashMap<>();

        for (T item : items) {
            graph.put(item, new HashSet<>());
            inDegree.put(item, 0);
        }

        for (T item : items) {
            CalculationType type = typeExtractor.apply(item);
            if (type == null || !calculationEngine.isSupported(type)) {
                continue;
            }

            Set<String> reqCodes = calculationEngine.getRequiredParameters(type);
            for (String reqCode : reqCodes) {
                // Check if this required parameter is itself one of the calculated items
                T upstream = calcByCode.get(reqCode.trim().toUpperCase());
                if (upstream == null) {
                    String canon = ClinicalParameterAliases.getCanonical(reqCode);
                    upstream = calcByCode.get(canon);
                }

                if (upstream != null && !upstream.equals(item)) {
                    // upstream must execute before item
                    if (graph.get(upstream).add(item)) {
                        inDegree.put(item, inDegree.get(item) + 1);
                    }
                }
            }
        }

        Queue<T> queue = new ArrayDeque<>();
        for (T item : items) {
            if (inDegree.get(item) == 0) {
                queue.add(item);
            }
        }

        List<T> sorted = new ArrayList<>();
        while (!queue.isEmpty()) {
            T current = queue.poll();
            sorted.add(current);

            for (T neighbor : graph.get(current)) {
                int newDegree = inDegree.get(neighbor) - 1;
                inDegree.put(neighbor, newDegree);
                if (newDegree == 0) {
                    queue.add(neighbor);
                }
            }
        }

        if (sorted.size() != items.size()) {
            throw new CalculationException(
                    "Circular calculation dependency detected among calculated test parameters."
            );
        }

        return sorted;
    }
}
