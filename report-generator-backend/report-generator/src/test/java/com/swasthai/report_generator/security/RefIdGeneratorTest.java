package com.swasthai.report_generator.security;

import com.swasthai.report_generator.common.util.RefIdGenerator;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RefIdGeneratorTest {

    @Test
    void testRefIdFormatAndEntropy() {
        String refId = RefIdGenerator.generate("PAT");

        assertNotNull(refId);
        assertTrue(refId.startsWith("PAT-"));
        assertEquals(16, refId.length()); // "PAT-" (4) + 12 chars = 16

        // Verify alphanumeric Base62 characters
        String suffix = refId.substring(4);
        assertTrue(suffix.matches("^[0-9A-Za-z]{12}$"));
    }

    @Test
    void testUniquenessAcrossThousandsOfGenerations() {
        Set<String> generated = new HashSet<>();
        int count = 5000;

        for (int i = 0; i < count; i++) {
            String refId = RefIdGenerator.generate("USR");
            assertTrue(generated.add(refId), "Duplicate refId generated: " + refId);
        }

        assertEquals(count, generated.size());
    }
}
