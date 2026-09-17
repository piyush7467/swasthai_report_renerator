package com.swasthai.report_generator.report.pdf;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import org.openpdf.text.Image;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.EnumMap;
import java.util.Map;

/**
 * Generates QR codes for finalized report verification.
 *
 * Security:
 * - Only the supplied verification URL is encoded.
 * - No patient information is encoded.
 * - No test results are encoded.
 * - No internal database UUID is encoded.
 * - QR image is generated entirely in memory.
 * - No temporary files are created.
 */
@Component
public class VerificationQrCodeGenerator {

    private static final int DEFAULT_QR_SIZE = 256;

    private static final int DEFAULT_MARGIN = 1;

    private static final float DEFAULT_DISPLAY_WIDTH = 78f;

    private static final float DEFAULT_DISPLAY_HEIGHT = 78f;

    /**
     * Generates an OpenPDF Image containing a QR code.
     *
     * @param verificationUrl public/non-sensitive report verification URL
     * @return OpenPDF Image
     */
    public Image generate(String verificationUrl) {

        byte[] pngBytes = generatePngBytes(verificationUrl);

        try {

            Image qrImage =
                    Image.getInstance(pngBytes);

            qrImage.scaleToFit(
                    DEFAULT_DISPLAY_WIDTH,
                    DEFAULT_DISPLAY_HEIGHT
            );

            qrImage.setAlignment(
                    org.openpdf.text.Element.ALIGN_CENTER
            );

            return qrImage;

        } catch (Exception exception) {

            /*
             * Do not silently generate a PDF containing
             * a broken verification QR code.
             */
            throw new IllegalStateException(
                    "Failed to generate report verification QR code",
                    exception
            );
        }
    }

    /**
     * Generates PNG bytes for the QR code encoding the verification URL.
     *
     * @param verificationUrl public/non-sensitive report verification URL
     * @return PNG encoded byte array
     */
    public byte[] generatePngBytes(String verificationUrl) {

        String normalizedUrl =
                validateVerificationUrl(verificationUrl);

        try {

            BitMatrix matrix =
                    createQrMatrix(
                            normalizedUrl,
                            DEFAULT_QR_SIZE
                    );

            BufferedImage bufferedImage =
                    convertToBufferedImage(matrix);

            return convertToPngBytes(bufferedImage);

        } catch (Exception exception) {

            throw new IllegalStateException(
                    "Failed to generate report verification QR code",
                    exception
            );
        }
    }

    /**
     * Creates the ZXing QR matrix.
     */
    private BitMatrix createQrMatrix(
            String verificationUrl,
            int size
    ) throws Exception {

        Map<EncodeHintType, Object> hints =
                new EnumMap<>(EncodeHintType.class);

        /*
         * Quiet zone around the QR code.
         *
         * This helps phone cameras identify the QR boundary.
         */
        hints.put(
                EncodeHintType.MARGIN,
                DEFAULT_MARGIN
        );

        return new MultiFormatWriter().encode(
                verificationUrl,
                BarcodeFormat.QR_CODE,
                size,
                size,
                hints
        );
    }

    /**
     * Converts ZXing BitMatrix into a standard RGB image.
     */
    private BufferedImage convertToBufferedImage(
            BitMatrix matrix
    ) {

        int width = matrix.getWidth();

        int height = matrix.getHeight();

        BufferedImage image =
                new BufferedImage(
                        width,
                        height,
                        BufferedImage.TYPE_INT_RGB
                );

        for (int x = 0; x < width; x++) {

            for (int y = 0; y < height; y++) {

                image.setRGB(
                        x,
                        y,
                        matrix.get(x, y)
                                ? Color.BLACK.getRGB()
                                : Color.WHITE.getRGB()
                );
            }
        }

        return image;
    }

    /**
     * Converts the generated image to PNG bytes.
     */
    private byte[] convertToPngBytes(
            BufferedImage image
    ) throws Exception {

        ByteArrayOutputStream outputStream =
                new ByteArrayOutputStream();

        boolean written =
                ImageIO.write(
                        image,
                        "PNG",
                        outputStream
                );

        if (!written) {

            throw new IllegalStateException(
                    "Unable to encode QR code as PNG"
            );
        }

        return outputStream.toByteArray();
    }

    /**
     * Validates and normalizes the verification URL.
     *
     * The QR is intended to open a web verification page,
     * therefore only HTTP/HTTPS URLs are accepted.
     */
    private String validateVerificationUrl(
            String verificationUrl
    ) {

        if (verificationUrl == null) {

            throw new IllegalArgumentException(
                    "Verification URL cannot be null"
            );
        }

        String normalizedUrl =
                verificationUrl.trim();

        if (normalizedUrl.isBlank()) {

            throw new IllegalArgumentException(
                    "Verification URL cannot be blank"
            );
        }

        /*
         * Prevent arbitrary non-web QR payloads.
         */
        if (!normalizedUrl.startsWith("https://")
                && !normalizedUrl.startsWith("http://")) {

            throw new IllegalArgumentException(
                    "Verification URL must use HTTP or HTTPS"
            );
        }

        return normalizedUrl;
    }
}