package com.swasthai.report_generator.report.pdf;

public interface PdfRenderer {
    byte[] render(ReportPdfData reportPdfData);
}