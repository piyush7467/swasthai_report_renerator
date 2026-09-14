package com.swasthai.report_generator.report.pdf;

import lombok.Builder;

import java.util.List;

@Builder
public record TestPdfItem(
        String testCode,
        String testName,
        String testShortName,
        String sampleType,
        String customSampleType,
        String specimenContainer,
        String reportSection,
        Integer displayOrder,
        Integer testVersion,
        List<ParameterPdfItem> parameters
) {
}