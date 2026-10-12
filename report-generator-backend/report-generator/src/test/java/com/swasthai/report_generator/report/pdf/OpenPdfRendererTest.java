package com.swasthai.report_generator.report.pdf;

import com.swasthai.report_generator.report.entity.ReportStatus;
import com.swasthai.report_generator.storage.FileStorageService;
import com.swasthai.report_generator.test.entity.CalculationType;
import com.swasthai.report_generator.test.entity.ParameterInputType;
import com.swasthai.report_generator.test.entity.ResultFlag;
import com.swasthai.report_generator.test.entity.TestParameterDataType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openpdf.text.Image;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.parser.PdfTextExtractor;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpenPdfRendererTest {

    @Mock
    private VerificationQrCodeGenerator verificationQrCodeGenerator;

    @Mock
    private FileStorageService fileStorageService;

    private OpenPdfRenderer renderer;

    @BeforeEach
    void setUp() throws Exception {
        renderer = new OpenPdfRenderer(verificationQrCodeGenerator, fileStorageService);

        // Mock a dummy 1x1 QR code image
        BufferedImage qrImg = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream qrBaos = new ByteArrayOutputStream();
        ImageIO.write(qrImg, "png", qrBaos);
        Image mockQr = Image.getInstance(qrBaos.toByteArray());
        lenient().when(verificationQrCodeGenerator.generate(anyString())).thenReturn(mockQr);
    }

    private byte[] createSamplePngBytes() throws IOException {
        BufferedImage img = new BufferedImage(40, 40, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.BLUE);
        g.fillRect(0, 0, 40, 40);
        g.dispose();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        return baos.toByteArray();
    }

    private ReportPdfData createSampleReportData(Boolean includeOrganizationHeader, String logoStorageKey) {
        OrganizationPdfSnapshot organization = OrganizationPdfSnapshot.builder()
                .name("Apex Diagnostic Laboratory")
                .addressLine1("123 Health Ave")
                .city("Mumbai")
                .state("Maharashtra")
                .postalCode("400001")
                .country("India")
                .phone("+91 9876543210")
                .email("contact@apexlab.com")
                .website("www.apexlab.com")
                .logoStorageKey(logoStorageKey)
                .reportFooterText("Excellence in Diagnostics")
                .reportDisclaimer("Correlate clinically")
                .build();

        PatientPdfSnapshot patient = PatientPdfSnapshot.builder()
                .patientCode("PAT-1001")
                .name("Rahul Sharma")
                .salutation("Mr.")
                .gender("MALE")
                .ageValue(35)
                .ageUnit("YEARS")
                .dateOfBirth(LocalDate.of(1991, 1, 1))
                .phone("+91 9123456789")
                .build();

        UserPdfSnapshot doctor = UserPdfSnapshot.builder()
                .name("Dr. A. Verma")
                .email("averma@apexlab.com")
                .build();

        ParameterPdfItem param = ParameterPdfItem.builder()
                .parameterCode("HB")
                .parameterName("Haemoglobin")
                .dataType(TestParameterDataType.DECIMAL)
                .inputType(ParameterInputType.MANUAL)
                .calculationType(CalculationType.NONE)
                .unit("g/dL")
                .value("14.5")
                .numericValue(new BigDecimal("14.5"))
                .referenceMin(new BigDecimal("13.0"))
                .referenceMax(new BigDecimal("17.0"))
                .flag(ResultFlag.NORMAL)
                .displayOrder(1)
                .build();

        TestPdfItem test = TestPdfItem.builder()
                .testCode("CBC")
                .testName("Complete Blood Count")
                .reportSection("HAEMATOLOGY")
                .displayOrder(1)
                .parameters(List.of(param))
                .build();

        return ReportPdfData.builder()
                .reportRefId("REP-2026-TEST01")
                .status(ReportStatus.FINALIZED)
                .reportVersion(1)
                .createdAt(Instant.parse("2026-10-12T10:00:00Z"))
                .finalizedAt(Instant.parse("2026-10-12T10:30:00Z"))
                .organization(organization)
                .patient(patient)
                .createdBy(doctor)
                .finalizedBy(doctor)
                .tests(List.of(test))
                .verificationUrl("https://verify.swasthai.com/reports/REP-2026-TEST01")
                .includeOrganizationHeader(includeOrganizationHeader)
                .build();
    }

    @Test
    @DisplayName("Renders PDF with Organization Header ON and valid logo")
    void testRenderPdf_HeaderOn_WithValidLogo() throws Exception {
        String logoKey = "org/1/logo.png";
        byte[] logoBytes = createSamplePngBytes();
        when(fileStorageService.exists(logoKey)).thenReturn(true);
        when(fileStorageService.load(logoKey)).thenReturn(logoBytes);

        ReportPdfData data = createSampleReportData(true, logoKey);
        byte[] pdfBytes = renderer.render(data);

        assertThat(pdfBytes).isNotEmpty();
        PdfReader reader = new PdfReader(pdfBytes);
        assertThat(reader.getNumberOfPages()).isGreaterThanOrEqualTo(1);

        String text = new PdfTextExtractor(reader).getTextFromPage(1);
        assertThat(text).contains("Apex Diagnostic Laboratory");
        assertThat(text).contains("Rahul Sharma");
        assertThat(text).contains("Haemoglobin");
        reader.close();
    }

    @Test
    @DisplayName("Renders PDF with Organization Header ON when logo is missing (clean fallback)")
    void testRenderPdf_HeaderOn_NoLogoConfigured() throws Exception {
        ReportPdfData data = createSampleReportData(true, null);
        byte[] pdfBytes = renderer.render(data);

        assertThat(pdfBytes).isNotEmpty();
        PdfReader reader = new PdfReader(pdfBytes);
        String text = new PdfTextExtractor(reader).getTextFromPage(1);
        assertThat(text).contains("Apex Diagnostic Laboratory");
        assertThat(text).contains("Rahul Sharma");
        reader.close();
    }

    @Test
    @DisplayName("Renders PDF with Organization Header ON when logo file is corrupted (graceful fallback without throwing)")
    void testRenderPdf_HeaderOn_CorruptedLogoFile() throws Exception {
        String logoKey = "org/1/corrupted.png";
        byte[] corruptedBytes = new byte[]{ 0, 1, 2, 3, 4, 5 };
        when(fileStorageService.exists(logoKey)).thenReturn(true);
        when(fileStorageService.load(logoKey)).thenReturn(corruptedBytes);

        ReportPdfData data = createSampleReportData(true, logoKey);
        byte[] pdfBytes = renderer.render(data);

        assertThat(pdfBytes).isNotEmpty();
        PdfReader reader = new PdfReader(pdfBytes);
        String text = new PdfTextExtractor(reader).getTextFromPage(1);
        assertThat(text).contains("Apex Diagnostic Laboratory");
        reader.close();
    }

    @Test
    @DisplayName("Renders PDF with Organization Header OFF leaving blank space placeholder for letterhead paper")
    void testRenderPdf_HeaderOff_BlankSpacePlaceholder() throws Exception {
        ReportPdfData data = createSampleReportData(false, null);
        byte[] pdfBytes = renderer.render(data);

        assertThat(pdfBytes).isNotEmpty();
        PdfReader reader = new PdfReader(pdfBytes);
        String text = new PdfTextExtractor(reader).getTextFromPage(1);
        // Organization header details (tagline, address, email) are omitted from letterhead space
        assertThat(text).doesNotContain("Excellence in Diagnostics");
        assertThat(text).doesNotContain("123 Health Ave");
        assertThat(text).doesNotContain("contact@apexlab.com");
        // But patient demographics and test details must be present
        assertThat(text).contains("Rahul Sharma");
        assertThat(text).contains("Haemoglobin");
        reader.close();
    }

    @Test
    @DisplayName("Throws IllegalStateException when attempting to render non-finalized report")
    void testRenderPdf_NotFinalized_ThrowsException() {
        ReportPdfData data = ReportPdfData.builder()
                .reportRefId("REP-2026-DRAFT")
                .status(ReportStatus.DRAFT)
                .organization(OrganizationPdfSnapshot.builder().name("Lab").build())
                .patient(PatientPdfSnapshot.builder().name("Patient").patientCode("P1").build())
                .tests(List.of())
                .finalizedBy(UserPdfSnapshot.builder().name("Doc").build())
                .verificationUrl("https://verify.swasthai.com")
                .build();

        assertThatThrownBy(() -> renderer.render(data))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only finalized reports can be rendered as PDF");
    }
}
