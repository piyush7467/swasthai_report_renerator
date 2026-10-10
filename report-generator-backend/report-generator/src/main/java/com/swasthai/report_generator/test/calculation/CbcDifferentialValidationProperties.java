package com.swasthai.report_generator.test.calculation;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Configuration properties for CBC Differential Leukocyte Count validation.
 *
 * Clinical note:
 * Summing five differential parameters each rounded to 1 decimal place can yield
 * rounding deviations from 100.0%. The default rounding tolerance is ±1.0% (sum range [99.0%, 101.0%]).
 * Laboratory medical directors / pathologists may configure this tolerance per standard operating procedure.
 */
@Component
@ConfigurationProperties(prefix = "clinical.validation.cbc-differential")
@Getter
@Setter
public class CbcDifferentialValidationProperties {

    /**
     * Whether CBC differential total sum validation against 100% is enabled.
     * Default is true.
     */
    private boolean enabled = true;

    /**
     * Rounding tolerance (±%) allowed when validating the sum of differential percentages against 100.0%.
     * Default is 1.0% (accepting totals between 99.0% and 101.0%).
     */
    private BigDecimal tolerance = new BigDecimal("1.0");

    /**
     * Whether all 5 differential parameters (NEUT, LYMPH, MONO, EOS, BASO) are required
     * when differential count is entered.
     * Default is true (partial differential counts like 3 or 4 parameters are rejected as incomplete).
     */
    private boolean requireAllFiveWhenStarted = true;
}
