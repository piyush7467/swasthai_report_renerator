package com.swasthai.report_generator.test.calculation;

import com.swasthai.report_generator.patient.entity.Gender;

import java.math.BigDecimal;
import java.util.*;

/**
 * Contextual data provided to clinical parameter calculators.
 * Encapsulates source parameter values, source parameter units, and patient demographics.
 */
public class CalculationContext {

    private final Map<String, BigDecimal> values;
    private final Map<String, String> units;
    private final Integer patientAgeInYears;
    private final Gender patientGender;

    public CalculationContext(
            Map<String, BigDecimal> values,
            Map<String, String> units,
            Integer patientAgeInYears,
            Gender patientGender
    ) {
        this.values = values != null ? new HashMap<>(values) : new HashMap<>();
        this.units = units != null ? new HashMap<>(units) : new HashMap<>();
        this.patientAgeInYears = patientAgeInYears;
        this.patientGender = patientGender;
    }

    public static CalculationContext of(Map<String, BigDecimal> values) {
        return new CalculationContext(values, Collections.emptyMap(), null, null);
    }

    public static CalculationContext of(
            Map<String, BigDecimal> values,
            Map<String, String> units,
            Integer patientAgeInYears,
            Gender patientGender
    ) {
        return new CalculationContext(values, units, patientAgeInYears, patientGender);
    }

    public Map<String, BigDecimal> getValues() {
        return Collections.unmodifiableMap(values);
    }

    public Map<String, String> getUnits() {
        return Collections.unmodifiableMap(units);
    }

    public Integer getPatientAgeInYears() {
        return patientAgeInYears;
    }

    public Gender getPatientGender() {
        return patientGender;
    }

    /**
     * Resolves a numeric value by checking the canonical code and all registered aliases.
     */
    public BigDecimal getValue(String canonicalCode, String... additionalAliases) {
        if (canonicalCode == null) return null;

        // 1. Direct match
        BigDecimal val = values.get(canonicalCode);
        if (val != null) return val;

        // Case-insensitive direct match
        for (Map.Entry<String, BigDecimal> entry : values.entrySet()) {
            if (entry.getKey() != null && entry.getKey().trim().equalsIgnoreCase(canonicalCode.trim())) {
                return entry.getValue();
            }
        }

        // 2. Check registered clinical aliases
        Set<String> aliases = ClinicalParameterAliases.getAliases(canonicalCode);
        for (String alias : aliases) {
            for (Map.Entry<String, BigDecimal> entry : values.entrySet()) {
                if (entry.getKey() != null && entry.getKey().trim().equalsIgnoreCase(alias)) {
                    return entry.getValue();
                }
            }
        }

        // 3. Additional caller-supplied aliases
        if (additionalAliases != null) {
            for (String alias : additionalAliases) {
                if (alias == null) continue;
                for (Map.Entry<String, BigDecimal> entry : values.entrySet()) {
                    if (entry.getKey() != null && entry.getKey().trim().equalsIgnoreCase(alias.trim())) {
                        return entry.getValue();
                    }
                }
            }
        }

        return null;
    }

    /**
     * Resolves the unit for a given parameter code.
     */
    public String getUnit(String canonicalCode, String... additionalAliases) {
        if (canonicalCode == null) return null;

        String unit = units.get(canonicalCode);
        if (unit != null) return unit;

        for (Map.Entry<String, String> entry : units.entrySet()) {
            if (entry.getKey() != null && entry.getKey().trim().equalsIgnoreCase(canonicalCode.trim())) {
                return entry.getValue();
            }
        }

        Set<String> aliases = ClinicalParameterAliases.getAliases(canonicalCode);
        for (String alias : aliases) {
            for (Map.Entry<String, String> entry : units.entrySet()) {
                if (entry.getKey() != null && entry.getKey().trim().equalsIgnoreCase(alias)) {
                    return entry.getValue();
                }
            }
        }

        if (additionalAliases != null) {
            for (String alias : additionalAliases) {
                if (alias == null) continue;
                for (Map.Entry<String, String> entry : units.entrySet()) {
                    if (entry.getKey() != null && entry.getKey().trim().equalsIgnoreCase(alias.trim())) {
                        return entry.getValue();
                    }
                }
            }
        }

        return null;
    }

    public boolean containsKey(String canonicalCode, String... additionalAliases) {
        if (canonicalCode == null) return false;
        if (values.containsKey(canonicalCode)) return true;

        for (String k : values.keySet()) {
            if (k != null && k.trim().equalsIgnoreCase(canonicalCode.trim())) {
                return true;
            }
        }

        Set<String> aliases = ClinicalParameterAliases.getAliases(canonicalCode);
        for (String alias : aliases) {
            for (String k : values.keySet()) {
                if (k != null && k.trim().equalsIgnoreCase(alias)) {
                    return true;
                }
            }
        }

        if (additionalAliases != null) {
            for (String alias : additionalAliases) {
                if (alias == null) continue;
                for (String k : values.keySet()) {
                    if (k != null && k.trim().equalsIgnoreCase(alias.trim())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public boolean hasValue(String canonicalCode, String... additionalAliases) {
        return getValue(canonicalCode, additionalAliases) != null;
    }

    public void putValue(String code, BigDecimal value) {
        if (code != null) {
            this.values.put(code, value);
        }
    }
}
