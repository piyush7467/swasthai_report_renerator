package com.swasthai.report_generator.test.calculation;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

/**
 * Clinical validator and calculator for Complete Blood Count (CBC) Differential Leukocyte Count (DLC).
 *
 * <p>Approved Business Rules:
 * <ol>
 *   <li><b>Differential Parameters:</b>
 *       <ul>
 *         <li>{@code NEUT} - Neutrophils (%) [Manual entry]</li>
 *         <li>{@code LYMPH} - Lymphocytes (%) [Manual entry]</li>
 *         <li>{@code EOS} - Eosinophils (%) [Manual entry]</li>
 *         <li>{@code MONO} - Monocytes (%) [Automatically calculated: 100 - NEUT - LYMPH - EOS]</li>
 *         <li>{@code BASO} - Basophils (%) [Automatically set to 0]</li>
 *       </ul>
 *   </li>
 *   <li><b>Approved differential entry workflow:</b> Accepts the three required manual percentages
 *       (NEUT, LYMPH, EOS). Each must be numeric, finite, and between 0.0% and 100.0%.</li>
 *   <li><b>Calculated Monocytes:</b> MONO is calculated as {@code 100 - NEUT - LYMPH - EOS}.
 *       If the calculated MONO is negative (sum of manual inputs exceeds 100%), the request is strictly
 *       rejected with a validation error; it is NEVER silently clamped to zero.</li>
 *   <li><b>Basophils:</b> Automatically set to {@code 0.0%}. Users are not required to enter either calculated value.</li>
 *   <li><b>Conflict Rejection:</b> If client requests supply conflicting MONO or BASO values that bypass
 *       or contradict the approved calculation, the request is strictly rejected.</li>
 *   <li><b>Exact Total Sum:</b> The resulting NEUT, LYMPH, MONO, EOS, and BASO percentages must total
 *       exactly 100.0%.</li>
 *   <li><b>CBC without Differential:</b> If zero (0) differential parameters are submitted, the basic CBC panel
 *       without differential count is permitted.</li>
 *   <li><b>Scope Restriction:</b> Applied only to the CBC test differential workflow. Non-CBC tests are never affected.</li>
 * </ol>
 */
@Component
public class CbcDifferentialValidator {

    public static final String PARAM_NEUT = "NEUT";
    public static final String PARAM_LYMPH = "LYMPH";
    public static final String PARAM_MONO = "MONO";
    public static final String PARAM_EOS = "EOS";
    public static final String PARAM_BASO = "BASO";

    public static final List<String> MANUAL_DIFFERENTIAL_PARAMETERS = List.of(
            PARAM_NEUT,
            PARAM_LYMPH,
            PARAM_EOS
    );

    public static final List<String> DIFFERENTIAL_PARAMETERS = List.of(
            PARAM_NEUT,
            PARAM_LYMPH,
            PARAM_MONO,
            PARAM_EOS,
            PARAM_BASO
    );

    public static final BigDecimal HUNDRED = new BigDecimal("100.0");
    public static final BigDecimal DEFAULT_TOLERANCE = new BigDecimal("0.0");

    private final CbcDifferentialValidationProperties properties;

    public CbcDifferentialValidator() {
        this.properties = new CbcDifferentialValidationProperties();
    }

    public CbcDifferentialValidator(CbcDifferentialValidationProperties properties) {
        this.properties = properties != null ? properties : new CbcDifferentialValidationProperties();
    }

    public CbcDifferentialValidator(BigDecimal tolerance) {
        this.properties = new CbcDifferentialValidationProperties();
        if (tolerance != null) {
            this.properties.setTolerance(tolerance);
        }
    }

    /**
     * Calculation & validation result for CBC differential leukocyte count.
     */
    public record DifferentialCalculationResult(
            boolean applied,
            Map<String, BigDecimal> differentialValues,
            BigDecimal neut,
            BigDecimal lymph,
            BigDecimal mono,
            BigDecimal eos,
            BigDecimal baso,
            BigDecimal totalSum
    ) {
        public static DifferentialCalculationResult notApplied() {
            return new DifferentialCalculationResult(
                    false,
                    Collections.emptyMap(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
        }
    }

    public BigDecimal getTolerance() {
        return properties.getTolerance() != null ? properties.getTolerance() : DEFAULT_TOLERANCE;
    }

    public boolean isEnabled() {
        return properties.isEnabled();
    }

    public boolean isCbcTest(String testCode) {
        return testCode != null && "CBC".equalsIgnoreCase(testCode.trim());
    }

    public boolean hasAnyDifferentialParameter(Collection<String> parameterCodes) {
        if (parameterCodes == null || parameterCodes.isEmpty()) {
            return false;
        }
        for (String code : parameterCodes) {
            if (isDifferentialCode(code)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Calculates and validates CBC differential leukocyte counts according to the lab-owner-approved rule.
     *
     * @param inputValues Map containing parameter codes and their numeric values
     * @return DifferentialCalculationResult with calculated values and total sum
     * @throws CbcDifferentialValidationException if validation rules or formulas are violated
     */
    public DifferentialCalculationResult calculateAndValidateDifferential(Map<String, BigDecimal> inputValues) {
        if (!properties.isEnabled() || inputValues == null || inputValues.isEmpty()) {
            return DifferentialCalculationResult.notApplied();
        }

        Map<String, BigDecimal> diffValues = extractDifferentialValues(inputValues);

        if (diffValues.isEmpty()) {
            // CBC without differential: permitted
            return DifferentialCalculationResult.notApplied();
        }

        // Differential count was initiated: check for the 3 required manual percentages (NEUT, LYMPH, EOS)
        List<String> missingManual = MANUAL_DIFFERENTIAL_PARAMETERS.stream()
                .filter(code -> !diffValues.containsKey(code))
                .toList();

        if (!missingManual.isEmpty()) {
            throw new CbcDifferentialValidationException(
                    String.format(
                            "Differential leukocyte count requires manual entry of Neutrophils (NEUT), " +
                                    "Lymphocytes (LYMPH), and Eosinophils (EOS). Missing: %s.",
                            missingManual
                    ),
                    missingManual
            );
        }

        // Validate each manual percentage is numeric, finite, and within [0, 100]
        for (String code : MANUAL_DIFFERENTIAL_PARAMETERS) {
            BigDecimal val = diffValues.get(code);
            if (val == null) {
                throw new CbcDifferentialValidationException("Differential parameter " + code + " value cannot be null");
            }
            if (Double.isInfinite(val.doubleValue()) || Double.isNaN(val.doubleValue())) {
                throw new CbcDifferentialValidationException("Differential parameter " + code + " must be a finite number");
            }
            if (val.compareTo(BigDecimal.ZERO) < 0) {
                throw new CbcDifferentialValidationException(
                        String.format("Differential parameter %s cannot be negative: %s%%", code, val.stripTrailingZeros().toPlainString())
                );
            }
            if (val.compareTo(HUNDRED) > 0) {
                throw new CbcDifferentialValidationException(
                        String.format("Differential parameter %s cannot exceed 100%%: %s%%", code, val.stripTrailingZeros().toPlainString())
                );
            }
        }

        BigDecimal neut = diffValues.get(PARAM_NEUT);
        BigDecimal lymph = diffValues.get(PARAM_LYMPH);
        BigDecimal eos = diffValues.get(PARAM_EOS);

        BigDecimal manualSum = neut.add(lymph).add(eos);
        BigDecimal mono = HUNDRED.subtract(manualSum);

        // Reject if calculated MONO is negative (do not silently clamp to zero)
        if (mono.compareTo(BigDecimal.ZERO) < 0) {
            throw new CbcDifferentialValidationException(
                    String.format(
                            "Calculated Monocytes percentage is negative (%s%%). " +
                                    "Sum of manual differential percentages (%s%%) exceeds 100.0%%. " +
                                    "Values must not be silently adjusted.",
                            mono.stripTrailingZeros().toPlainString(),
                            manualSum.stripTrailingZeros().toPlainString()
                    ),
                    manualSum,
                    getTolerance()
            );
        }

        // Set BASO to 0 automatically
        BigDecimal baso = BigDecimal.ZERO;

        // Check for conflicting client-supplied MONO or BASO
        if (diffValues.containsKey(PARAM_MONO)) {
            BigDecimal clientMono = diffValues.get(PARAM_MONO);
            if (clientMono.compareTo(mono) != 0) {
                throw new CbcDifferentialValidationException(
                        String.format(
                                "Conflicting value supplied for calculated parameter MONO: " +
                                        "provided %s%% does not match calculated %s%%.",
                                clientMono.stripTrailingZeros().toPlainString(),
                                mono.stripTrailingZeros().toPlainString()
                        )
                );
            }
        }

        if (diffValues.containsKey(PARAM_BASO)) {
            BigDecimal clientBaso = diffValues.get(PARAM_BASO);
            if (clientBaso.compareTo(BigDecimal.ZERO) != 0) {
                throw new CbcDifferentialValidationException(
                        String.format(
                                "Conflicting value supplied for calculated parameter BASO: " +
                                        "expected 0.0%% but got %s%%.",
                                clientBaso.stripTrailingZeros().toPlainString()
                        )
                );
            }
        }

        BigDecimal total = neut.add(lymph).add(mono).add(eos).add(baso);

        Map<String, BigDecimal> fullMap = new LinkedHashMap<>();
        fullMap.put(PARAM_NEUT, neut);
        fullMap.put(PARAM_LYMPH, lymph);
        fullMap.put(PARAM_MONO, mono);
        fullMap.put(PARAM_EOS, eos);
        fullMap.put(PARAM_BASO, baso);

        return new DifferentialCalculationResult(
                true,
                Collections.unmodifiableMap(fullMap),
                neut,
                lymph,
                mono,
                eos,
                baso,
                total
        );
    }

    /**
     * Executes CBC differential workflow for a specific test code.
     * Applies ONLY when test is CBC.
     */
    public DifferentialCalculationResult processDifferentialForTest(String testCode, Map<String, BigDecimal> numericValues) {
        if (!isCbcTest(testCode)) {
            return DifferentialCalculationResult.notApplied();
        }
        return calculateAndValidateDifferential(numericValues);
    }

    public void validate(Map<String, BigDecimal> numericValues) {
        calculateAndValidateDifferential(numericValues);
    }

    public void validateForTest(String testCode, Map<String, BigDecimal> numericValues) {
        processDifferentialForTest(testCode, numericValues);
    }

    private Map<String, BigDecimal> extractDifferentialValues(Map<String, BigDecimal> inputValues) {
        Map<String, BigDecimal> diffValues = new LinkedHashMap<>();
        for (Map.Entry<String, BigDecimal> entry : inputValues.entrySet()) {
            String rawCode = entry.getKey();
            if (rawCode == null) continue;
            String canonicalCode = resolveCanonicalCode(rawCode);
            if (canonicalCode != null && entry.getValue() != null) {
                diffValues.put(canonicalCode, entry.getValue());
            }
        }
        return diffValues;
    }

    private String resolveCanonicalCode(String code) {
        String upper = code.trim().toUpperCase();
        if (DIFFERENTIAL_PARAMETERS.contains(upper)) {
            return upper;
        }
        String aliased = ClinicalParameterAliases.getCanonical(upper);
        if (DIFFERENTIAL_PARAMETERS.contains(aliased)) {
            return aliased;
        }
        return null;
    }

    private boolean isDifferentialCode(String code) {
        return resolveCanonicalCode(code) != null;
    }
}
