package com.swasthai.report_generator.report.pdf;

import lombok.Builder;

@Builder
public record UserPdfSnapshot(
        String name,
        String email
) {
}