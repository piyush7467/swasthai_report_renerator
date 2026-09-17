package com.swasthai.report_generator.report.pdf;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openpdf.text.Image;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VerificationQrCodeGeneratorTest {

    private VerificationQrCodeGenerator qrCodeGenerator;

    @BeforeEach
    void setUp() {
        qrCodeGenerator = new VerificationQrCodeGenerator();
    }

    @Test
    @DisplayName("Successfully generates QR code for valid HTTPS verification URL and decodes exact payload")
    void testGenerate_ValidHttpsUrl() throws Exception {
        String testUrl = "https://verify.swasthai.com/reports/REP-2026-ABCD1234";

        Image qrImage = qrCodeGenerator.generate(testUrl);
        assertThat(qrImage).isNotNull();

        byte[] pngBytes = qrCodeGenerator.generatePngBytes(testUrl);
        assertThat(pngBytes).isNotEmpty();

        BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(pngBytes));
        assertThat(bufferedImage).isNotNull();

        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();
        int[] pixels = bufferedImage.getRGB(0, 0, width, height, null, 0, width);
        LuminanceSource source = new RGBLuminanceSource(width, height, pixels);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

        Result result = new MultiFormatReader().decode(bitmap);

        assertThat(result.getText()).isEqualTo(testUrl);
        // Security checks: Verify no PHI, patient info, or test results are in QR payload
        assertThat(result.getText()).doesNotContain("patient");
        assertThat(result.getText()).doesNotContain("result");
        assertThat(result.getText()).doesNotContain("UUID");
    }

    @Test
    @DisplayName("Successfully generates QR code for valid HTTP verification URL and decodes exact payload")
    void testGenerate_ValidHttpUrl() throws Exception {
        String testUrl = "http://localhost:8080/verify/REP-9999";

        Image qrImage = qrCodeGenerator.generate(testUrl);
        assertThat(qrImage).isNotNull();

        byte[] pngBytes = qrCodeGenerator.generatePngBytes(testUrl);
        assertThat(pngBytes).isNotEmpty();

        BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(pngBytes));
        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();
        int[] pixels = bufferedImage.getRGB(0, 0, width, height, null, 0, width);
        LuminanceSource source = new RGBLuminanceSource(width, height, pixels);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

        Result result = new MultiFormatReader().decode(bitmap);
        assertThat(result.getText()).isEqualTo(testUrl);
    }

    @Test
    @DisplayName("Throws exception when verification URL is null")
    void testGenerate_NullUrl_ThrowsException() {
        assertThatThrownBy(() -> qrCodeGenerator.generate(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null");

        assertThatThrownBy(() -> qrCodeGenerator.generatePngBytes(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null");
    }

    @Test
    @DisplayName("Throws exception when verification URL is blank or whitespace")
    void testGenerate_BlankUrl_ThrowsException() {
        assertThatThrownBy(() -> qrCodeGenerator.generate("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be blank");

        assertThatThrownBy(() -> qrCodeGenerator.generatePngBytes(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be blank");
    }

    @Test
    @DisplayName("Throws exception when verification URL does not use HTTP or HTTPS")
    void testGenerate_NonHttpUrl_ThrowsException() {
        assertThatThrownBy(() -> qrCodeGenerator.generate("ftp://example.com/test"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must use HTTP or HTTPS");

        assertThatThrownBy(() -> qrCodeGenerator.generate("javascript:alert(1)"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must use HTTP or HTTPS");

        assertThatThrownBy(() -> qrCodeGenerator.generatePngBytes("data:text/plain;base64;abc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must use HTTP or HTTPS");
    }
}
