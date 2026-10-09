package com.swasthai.report_generator.test.masterdata.validation;

import lombok.Getter;

import java.util.Collections;
import java.util.List;

/**
 * Thrown when master data seeding validation fails.
 * Collects one or more explicit error reasons.
 */
@Getter
public class MasterDataValidationException extends RuntimeException {

    private final List<String> errors;

    public MasterDataValidationException(String message) {
        super(message);
        this.errors = List.of(message);
    }

    public MasterDataValidationException(String message, Throwable cause) {
        super(message, cause);
        this.errors = List.of(message);
    }

    public MasterDataValidationException(String message, List<String> errors) {
        super(formatMessage(message, errors));
        this.errors = errors != null ? Collections.unmodifiableList(errors) : Collections.emptyList();
    }

    private static String formatMessage(String message, List<String> errors) {
        if (errors == null || errors.isEmpty()) {
            return message;
        }
        StringBuilder sb = new StringBuilder(message).append(":\n");
        for (String err : errors) {
            sb.append("  - ").append(err).append("\n");
        }
        return sb.toString().trim();
    }
}
