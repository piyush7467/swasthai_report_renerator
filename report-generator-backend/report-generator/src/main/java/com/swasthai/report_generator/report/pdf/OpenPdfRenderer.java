package com.swasthai.report_generator.report.pdf;

import com.swasthai.report_generator.storage.FileStorageService;
import com.swasthai.report_generator.test.entity.ResultFlag;
import lombok.RequiredArgsConstructor;
import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.FontFactory;
import org.openpdf.text.Image;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.ColumnText;
import org.openpdf.text.pdf.PdfContentByte;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfPageEventHelper;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OpenPdfRenderer implements PdfRenderer {

        private static final String DEFAULT_ORGANIZATION_NAME = "Diagnostic Laboratory";

        private static final String REPORT_TITLE = "CLINICAL LABORATORY REPORT";

        private static final String VERIFICATION_MESSAGE = "This report is electronically generated and verified.";

        private static final String DEFAULT_LOGO_TEXT = "LAB";

        private static final ZoneId REPORT_ZONE = ZoneId.of("Asia/Kolkata");

        private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")
                        .withZone(REPORT_ZONE);

        private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy");

        private final VerificationQrCodeGenerator verificationQrCodeGenerator;

        /*
         * ============================================================
         * PAGE
         * ============================================================
         */

        private static final float MARGIN_LEFT = 36f;
        private static final float MARGIN_RIGHT = 36f;
        private static final float MARGIN_TOP = 55f;
        private static final float MARGIN_BOTTOM = 58f;

        /*
         * ============================================================
         * COLORS
         * ============================================================
         *
         * The PDF deliberately avoids pure black for normal body text.
         * This gives the report a cleaner clinical/professional look.
         */

        private static final Color COLOR_PRIMARY = new Color(25, 75, 125);

        private static final Color COLOR_PRIMARY_DARK = new Color(18, 55, 95);

        private static final Color COLOR_PRIMARY_LIGHT = new Color(232, 240, 248);

        private static final Color COLOR_BORDER = new Color(205, 212, 220);

        private static final Color COLOR_LIGHT_BORDER = new Color(225, 230, 235);

        private static final Color COLOR_LIGHT_BACKGROUND = new Color(248, 249, 251);

        private static final Color COLOR_TEXT = new Color(35, 35, 35);

        private static final Color COLOR_MUTED = new Color(100, 105, 110);

        private static final Color COLOR_NORMAL = new Color(30, 125, 65);

        private static final Color COLOR_ABNORMAL = new Color(190, 45, 40);

        private static final Color COLOR_CRITICAL = new Color(145, 20, 20);

        private static final Color COLOR_FALLBACK_LOGO = new Color(235, 239, 244);

        /*
         * ============================================================
         * FONTS
         * ============================================================
         */

        private static final Font FONT_ORGANIZATION = FontFactory.getFont(
                        FontFactory.HELVETICA_BOLD,
                        18,
                        COLOR_PRIMARY_DARK);

        private static final Font FONT_REPORT_TITLE = FontFactory.getFont(
                        FontFactory.HELVETICA_BOLD,
                        10.5f,
                        COLOR_PRIMARY);

        private static final Font FONT_SECTION = FontFactory.getFont(
                        FontFactory.HELVETICA_BOLD,
                        10,
                        COLOR_PRIMARY_DARK);

        private static final Font FONT_SUBSECTION = FontFactory.getFont(
                        FontFactory.HELVETICA_BOLD,
                        9,
                        COLOR_PRIMARY_DARK);

        private static final Font FONT_LABEL = FontFactory.getFont(
                        FontFactory.HELVETICA_BOLD,
                        7.5f,
                        COLOR_MUTED);

        private static final Font FONT_VALUE = FontFactory.getFont(
                        FontFactory.HELVETICA,
                        8.2f,
                        COLOR_TEXT);

        private static final Font FONT_RESULT = FontFactory.getFont(
                        FontFactory.HELVETICA_BOLD,
                        8.5f,
                        COLOR_TEXT);

        private static final Font FONT_TABLE_HEADER = FontFactory.getFont(
                        FontFactory.HELVETICA_BOLD,
                        8,
                        Color.WHITE);

        private static final Font FONT_TABLE = FontFactory.getFont(
                        FontFactory.HELVETICA,
                        7.8f,
                        COLOR_TEXT);

        private static final Font FONT_TABLE_BOLD = FontFactory.getFont(
                        FontFactory.HELVETICA_BOLD,
                        7.8f,
                        COLOR_TEXT);

        private static final Font FONT_SMALL = FontFactory.getFont(
                        FontFactory.HELVETICA,
                        7,
                        COLOR_MUTED);

        private static final Font FONT_SMALL_BOLD = FontFactory.getFont(
                        FontFactory.HELVETICA_BOLD,
                        7,
                        COLOR_MUTED);

        private static final Font FONT_NORMAL_FLAG = FontFactory.getFont(
                        FontFactory.HELVETICA_BOLD,
                        7.8f,
                        COLOR_NORMAL);

        private static final Font FONT_ABNORMAL_FLAG = FontFactory.getFont(
                        FontFactory.HELVETICA_BOLD,
                        7.8f,
                        COLOR_ABNORMAL);

        private static final Font FONT_CRITICAL_FLAG = FontFactory.getFont(
                        FontFactory.HELVETICA_BOLD,
                        7.8f,
                        COLOR_CRITICAL);

        /*
         * ============================================================
         * STORAGE
         * ============================================================
         */

        private final FileStorageService fileStorageService;

        /*
         * ============================================================
         * RENDER
         * ============================================================
         */

        @Override
        public byte[] render(ReportPdfData data) {

                validateInput(data);

                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

                Document document = new Document(
                                PageSize.A4,
                                MARGIN_LEFT,
                                MARGIN_RIGHT,
                                MARGIN_TOP,
                                MARGIN_BOTTOM);

                try {

                        PdfWriter writer = PdfWriter.getInstance(
                                        document,
                                        outputStream);

                        Image qrImage = verificationQrCodeGenerator.generate(
                                        data.verificationUrl());

                        if (qrImage == null) {
                                throw new IllegalStateException(
                                                "Failed to generate verification QR code image");
                        }

                        writer.setPageEvent(
                                        new ReportPageEvent(data, qrImage));

                        document.open();

                        /*
                         * Main document content.
                         *
                         * Everything below is based exclusively on
                         * ReportPdfData historical snapshots.
                         */

                        addOrganizationHeader(document, data);

                        addPatientInformation(document, data);

                        addTests(document, data);

                        addSignOffSection(document, data);

                        addVerificationSection(document, data);

                        document.close();

                        return outputStream.toByteArray();

                } catch (DocumentException exception) {

                        /*
                         * Never log patient/report information here.
                         */

                        throw new IllegalStateException(
                                        "Failed to generate report PDF",
                                        exception);

                } finally {

                        if (document.isOpen()) {
                                document.close();
                        }
                }
        }

        /*
         * ============================================================
         * VALIDATION
         * ============================================================
         */

        private void validateInput(ReportPdfData data) {

                if (data == null) {
                        throw new IllegalArgumentException(
                                        "PDF report data cannot be null");
                }

                if (isBlank(data.reportRefId())) {
                        throw new IllegalStateException(
                                        "PDF report is missing report reference ID");
                }

                if (data.status() == null
                                || !"FINALIZED".equals(data.status().name())) {

                        throw new IllegalStateException(
                                        "Only finalized reports can be rendered as PDF");
                }

                if (data.organization() == null) {
                        throw new IllegalStateException(
                                        "PDF report is missing organization snapshot");
                }

                if (isBlank(data.organization().name())) {
                        throw new IllegalStateException(
                                        "PDF report is missing organization name");
                }

                if (data.patient() == null) {
                        throw new IllegalStateException(
                                        "PDF report is missing patient snapshot");
                }

                if (isBlank(data.patient().name())) {
                        throw new IllegalStateException(
                                        "PDF report is missing patient name");
                }

                if (isBlank(data.patient().patientCode())) {
                        throw new IllegalStateException(
                                        "PDF report is missing patient code");
                }

                if (isBlank(data.patient().gender())) {
                        throw new IllegalStateException(
                                        "PDF report is missing patient gender");
                }

                if (data.tests() == null || data.tests().isEmpty()) {
                        throw new IllegalStateException(
                                        "PDF report contains no tests");
                }

                if (data.finalizedBy() == null
                                || isBlank(data.finalizedBy().name())) {

                        throw new IllegalStateException(
                                        "PDF report is missing finalizer information");
                }

                /*
                 * If a signature exists, its owner must also exist.
                 *
                 * This protects historical signature attribution.
                 */

                if (!isBlank(data.organization().signatureStorageKey())
                                && isBlank(data.organization().signatureOwnerName())) {

                        throw new IllegalStateException(
                                        "PDF report signature owner is missing");
                }

                if (isBlank(data.verificationUrl())) {
                        throw new IllegalStateException(
                                        "PDF report is missing verification URL");
                }
        }

        /*
         * ============================================================
         * ORGANIZATION HEADER
         * ============================================================
         */

        private void addOrganizationHeader(
                        Document document,
                        ReportPdfData data) throws DocumentException {

                OrganizationPdfSnapshot organization = data.organization();

                PdfPTable header = new PdfPTable(2);

                header.setWidthPercentage(100);

                header.setWidths(
                                new float[] {
                                                18f,
                                                82f
                                });

                header.setSpacingAfter(4f);

                /*
                 * --------------------------------------------------------
                 * LOGO
                 * --------------------------------------------------------
                 */

                PdfPCell logoCell = createLogoCell(
                                organization.logoStorageKey());

                header.addCell(logoCell);

                /*
                 * --------------------------------------------------------
                 * ORGANIZATION DETAILS
                 * --------------------------------------------------------
                 */

                PdfPCell detailsCell = new PdfPCell();

                detailsCell.setBorder(
                                Rectangle.NO_BORDER);

                detailsCell.setPaddingLeft(8f);
                detailsCell.setPaddingRight(2f);
                detailsCell.setPaddingTop(2f);
                detailsCell.setPaddingBottom(2f);

                Paragraph organizationName = new Paragraph(
                                safeText(
                                                organization.name(),
                                                DEFAULT_ORGANIZATION_NAME),
                                FONT_ORGANIZATION);

                organizationName.setSpacingAfter(1.5f);

                detailsCell.addElement(
                                organizationName);

                Paragraph title = new Paragraph(
                                REPORT_TITLE,
                                FONT_REPORT_TITLE);

                title.setSpacingAfter(4f);

                detailsCell.addElement(title);

                addOrganizationContactLines(
                                detailsCell,
                                organization);

                header.addCell(detailsCell);

                if (Boolean.TRUE.equals(data.includeOrganizationHeader())) {
                        document.add(header);
                        addHorizontalRule(document);
                } else {
                        /*
                         * Preserves the exact reserved header space so the report layout
                         * remains consistent when printing on pre-printed letterhead.
                         */
                        float contentWidth = document.right() - document.left();
                        header.setTotalWidth(contentWidth);
                        float headerHeight = header.calculateHeights(true);

                        PdfPTable placeholder = new PdfPTable(1);
                        placeholder.setWidthPercentage(100);
                        placeholder.setSpacingAfter(header.spacingAfter());

                        PdfPCell blankCell = new PdfPCell();
                        blankCell.setBorder(Rectangle.NO_BORDER);
                        blankCell.setFixedHeight(headerHeight);
                        placeholder.addCell(blankCell);

                        document.add(placeholder);

                        addBlankRuleSpacer(document);
                }
        }

        private PdfPCell createLogoCell(
                        String logoStorageKey) {

                PdfPCell cell = new PdfPCell();

                cell.setBorder(
                                Rectangle.NO_BORDER);

                cell.setPadding(2f);

                cell.setVerticalAlignment(
                                Element.ALIGN_MIDDLE);

                /*
                 * Try to load the historical organization logo.
                 */
                if (!isBlank(logoStorageKey)) {

                        try {

                                if (fileStorageService.exists(
                                                logoStorageKey)) {

                                        byte[] imageBytes = fileStorageService.load(
                                                        logoStorageKey);

                                        Image image = Image.getInstance(
                                                        imageBytes);

                                        image.scaleToFit(
                                                        72f,
                                                        60f);

                                        cell.setHorizontalAlignment(
                                                        Element.ALIGN_LEFT);

                                        cell.addElement(image);

                                        return cell;
                                }

                        } catch (Exception exception) {

                                /*
                                 * Logo is optional.
                                 *
                                 * If the historical logo cannot be loaded or
                                 * decoded, use the neutral fallback instead.
                                 *
                                 * Do NOT log the storage key because it can reveal
                                 * organization information unnecessarily.
                                 */
                        }
                }

                /*
                 * ------------------------------------------------------------
                 * NEUTRAL FALLBACK
                 * ------------------------------------------------------------
                 *
                 * Never use SwasthAI branding here.
                 * Never use another organization's logo.
                 */

                PdfPTable fallback = new PdfPTable(1);

                fallback.setWidthPercentage(100);

                PdfPCell fallbackCell = new PdfPCell(
                                new Phrase(
                                                DEFAULT_LOGO_TEXT,
                                                FontFactory.getFont(
                                                                FontFactory.HELVETICA_BOLD,
                                                                14,
                                                                COLOR_PRIMARY_DARK)));

                fallbackCell.setFixedHeight(58f);

                fallbackCell.setHorizontalAlignment(
                                Element.ALIGN_CENTER);

                fallbackCell.setVerticalAlignment(
                                Element.ALIGN_MIDDLE);

                fallbackCell.setBackgroundColor(
                                COLOR_FALLBACK_LOGO);

                fallbackCell.setBorderColor(
                                COLOR_BORDER);

                fallback.addCell(
                                fallbackCell);

                cell.addElement(
                                fallback);

                return cell;
        }

        private void addOrganizationContactLines(
                        PdfPCell cell,
                        OrganizationPdfSnapshot organization) {

                String address = buildOrganizationAddress(
                                organization);

                if (!address.isBlank()) {

                        Paragraph paragraph = new Paragraph(
                                        address,
                                        FONT_SMALL);

                        paragraph.setSpacingAfter(2f);

                        cell.addElement(paragraph);
                }

                String contact = buildOrganizationContact(
                                organization);

                if (!contact.isBlank()) {

                        Paragraph paragraph = new Paragraph(
                                        contact,
                                        FONT_SMALL);

                        paragraph.setSpacingAfter(1f);

                        cell.addElement(paragraph);
                }
        }

        private String buildOrganizationAddress(
                        OrganizationPdfSnapshot organization) {

                StringBuilder builder = new StringBuilder();

                appendText(builder, organization.addressLine1());
                appendText(builder, organization.addressLine2());
                appendText(builder, organization.city());
                appendText(builder, organization.state());
                appendText(builder, organization.postalCode());
                appendText(builder, organization.country());

                return builder.toString();
        }

        private String buildOrganizationContact(
                        OrganizationPdfSnapshot organization) {

                StringBuilder builder = new StringBuilder();

                if (!isBlank(organization.phone())) {

                        builder.append("Phone: ")
                                        .append(
                                                        organization.phone().trim());
                }

                if (!isBlank(organization.alternatePhone())) {

                        appendSeparator(builder);

                        builder.append("Alt: ")
                                        .append(
                                                        organization.alternatePhone().trim());
                }

                if (!isBlank(organization.email())) {

                        appendSeparator(builder);

                        builder.append("Email: ")
                                        .append(
                                                        organization.email().trim());
                }

                if (!isBlank(organization.website())) {

                        appendSeparator(builder);

                        builder.append(
                                        organization.website().trim());
                }

                return builder.toString();
        }

        /*
         * ============================================================
         * PATIENT INFORMATION
         * ============================================================
         */

        private void addPatientInformation(
                        Document document,
                        ReportPdfData data) throws DocumentException {

                addSectionTitle(
                                document,
                                "PATIENT & REPORT INFORMATION");

                PatientPdfSnapshot patient = data.patient();

                PdfPTable table = new PdfPTable(4);

                table.setWidthPercentage(100);

                table.setWidths(
                                new float[] {
                                                18f,
                                                32f,
                                                18f,
                                                32f
                                });

                table.setSplitRows(true);
                table.setSplitLate(false);

                addInformationRow(
                                table,
                                "Patient Name",
                                formatPatientName(patient),
                                "Patient ID",
                                safeText(
                                                patient.patientCode(),
                                                "-"));

                addInformationRow(
                                table,
                                "Age / Gender",
                                formatAgeGender(patient),
                                "Report ID",
                                safeText(
                                                data.reportRefId(),
                                                "-"));

                addInformationRow(
                                table,
                                "Date of Birth",
                                formatDateOfBirth(patient),
                                "Report Status",
                                "FINALIZED");

                /*
                 * Optional fields are omitted rather than displaying
                 * meaningless "-" labels where possible.
                 */

                if (!isBlank(patient.phone())
                                || patient.weightKg() != null) {

                        addInformationRow(
                                        table,
                                        "Phone",
                                        safeText(
                                                        patient.phone(),
                                                        "-"),
                                        "Weight",
                                        formatWeight(patient));
                }

                if (!isBlank(patient.email())
                                || !isBlank(patient.address())) {

                        addInformationRow(
                                        table,
                                        "Email",
                                        safeText(
                                                        patient.email(),
                                                        "-"),
                                        "Address",
                                        safeText(
                                                        patient.address(),
                                                        "-"));
                }

                addInformationRow(
                                table,
                                "Created At",
                                formatInstant(data.createdAt()),
                                "Finalized At",
                                formatInstant(data.finalizedAt()));

                document.add(table);

                document.add(
                                createSpacer(8f));
        }

        private void addInformationRow(
                        PdfPTable table,
                        String label1,
                        String value1,
                        String label2,
                        String value2) {

                addLabelCell(
                                table,
                                label1);

                addValueCell(
                                table,
                                value1);

                addLabelCell(
                                table,
                                label2);

                addValueCell(
                                table,
                                value2);
        }

        private void addLabelCell(
                        PdfPTable table,
                        String text) {

                PdfPCell cell = new PdfPCell(
                                new Phrase(
                                                safeText(text, ""),
                                                FONT_LABEL));

                cell.setPadding(5f);

                cell.setBackgroundColor(
                                COLOR_LIGHT_BACKGROUND);

                cell.setBorderColor(
                                COLOR_LIGHT_BORDER);

                cell.setVerticalAlignment(
                                Element.ALIGN_MIDDLE);

                table.addCell(cell);
        }

        private void addValueCell(
                        PdfPTable table,
                        String text) {

                PdfPCell cell = new PdfPCell(
                                new Phrase(
                                                safeText(text, "-"),
                                                FONT_VALUE));

                cell.setPadding(5f);

                cell.setBorderColor(
                                COLOR_LIGHT_BORDER);

                cell.setVerticalAlignment(
                                Element.ALIGN_MIDDLE);

                table.addCell(cell);
        }

        /*
         * ============================================================
         * TESTS
         * ============================================================
         */

        private void addTests(
                        Document document,
                        ReportPdfData data) throws DocumentException {

                addSectionTitle(
                                document,
                                "LABORATORY INVESTIGATIONS");

                List<TestPdfItem> tests = data.tests() == null
                                ? Collections.emptyList()
                                : data.tests();

                for (int i = 0; i < tests.size(); i++) {

                        TestPdfItem test = tests.get(i);

                        if (test == null) {
                                continue;
                        }

                        addTest(
                                        document,
                                        test);

                        if (i < tests.size() - 1) {

                                document.add(
                                                createSpacer(7f));
                        }
                }
        }

        private void addTest(
                        Document document,
                        TestPdfItem test) throws DocumentException {

                String testTitle = buildTestTitle(test);

                PdfPTable testHeader = new PdfPTable(1);

                testHeader.setWidthPercentage(100);
                testHeader.setKeepTogether(false);

                PdfPCell titleCell = new PdfPCell(
                                new Phrase(
                                                testTitle,
                                                FONT_SECTION));

                titleCell.setPaddingTop(6f);
                titleCell.setPaddingBottom(6f);
                titleCell.setPaddingLeft(8f);
                titleCell.setPaddingRight(8f);

                titleCell.setBackgroundColor(
                                COLOR_PRIMARY_LIGHT);

                titleCell.setBorderColor(
                                COLOR_BORDER);

                titleCell.setBorderWidth(
                                0.8f);

                testHeader.addCell(titleCell);

                document.add(testHeader);

                addSampleInformation(
                                document,
                                test);

                addParameterTable(
                                document,
                                test);
        }

        private String buildTestTitle(
                        TestPdfItem test) {

                String name = safeText(
                                test.testName(),
                                "Laboratory Test");

                String code = safeText(
                                test.testCode(),
                                "");

                String shortName = safeText(
                                test.testShortName(),
                                "");

                if (!code.isBlank()) {
                        return name + " (" + code + ")";
                }

                if (!shortName.isBlank()) {
                        return name + " (" + shortName + ")";
                }

                return name;
        }

        private void addSampleInformation(
                        Document document,
                        TestPdfItem test) throws DocumentException {

                String sampleType = safeText(
                                test.sampleType(),
                                "");

                String customSampleType = safeText(
                                test.customSampleType(),
                                "");

                String container = safeText(
                                test.specimenContainer(),
                                "");

                String section = safeText(
                                test.reportSection(),
                                "");

                StringBuilder builder = new StringBuilder();

                if (!sampleType.isBlank()) {

                        builder.append(
                                        "Sample Type: ").append(sampleType);
                }

                if (!customSampleType.isBlank()) {

                        appendSeparator(builder);

                        builder.append(
                                        "Custom Sample: ").append(customSampleType);
                }

                if (!container.isBlank()) {

                        appendSeparator(builder);

                        builder.append(
                                        "Container: ").append(container);
                }

                if (!section.isBlank()) {

                        appendSeparator(builder);

                        builder.append(
                                        "Section: ").append(section);
                }

                if (builder.isEmpty()) {
                        return;
                }

                Paragraph sample = new Paragraph(
                                builder.toString(),
                                FONT_SMALL);

                sample.setSpacingBefore(3f);
                sample.setSpacingAfter(4f);

                document.add(sample);
        }

        /*
         * ============================================================
         * PARAMETER TABLE
         * ============================================================
         */

        private void addParameterTable(
                        Document document,
                        TestPdfItem test) throws DocumentException {

                PdfPTable table = new PdfPTable(5);

                table.setWidthPercentage(100);

                table.setWidths(
                                new float[] {
                                                29f,
                                                17f,
                                                15f,
                                                24f,
                                                15f
                                });

                /*
                 * Repeat table header when the table continues on
                 * another page.
                 */

                table.setHeaderRows(1);

                table.setSplitRows(true);
                table.setSplitLate(false);

                addTableHeader(
                                table,
                                "Parameter");

                addTableHeader(
                                table,
                                "Result");

                addTableHeader(
                                table,
                                "Unit");

                addTableHeader(
                                table,
                                "Reference Range");

                addTableHeader(
                                table,
                                "Flag");

                List<ParameterPdfItem> parameters = test.parameters() == null
                                ? Collections.emptyList()
                                : test.parameters();

                if (parameters.isEmpty()) {

                        PdfPCell emptyCell = new PdfPCell(
                                        new Phrase(
                                                        "No parameter results available.",
                                                        FONT_SMALL));

                        emptyCell.setColspan(5);

                        emptyCell.setPadding(6f);

                        emptyCell.setHorizontalAlignment(
                                        Element.ALIGN_CENTER);

                        emptyCell.setBorderColor(
                                        COLOR_LIGHT_BORDER);

                        table.addCell(emptyCell);

                } else {

                        for (ParameterPdfItem parameter : parameters) {

                                if (parameter == null) {
                                        continue;
                                }

                                addParameterRow(
                                                table,
                                                parameter);
                        }
                }

                document.add(table);
        }

        private void addParameterRow(
                        PdfPTable table,
                        ParameterPdfItem parameter) {

                /*
                 * Parameter name
                 */

                addParameterCell(
                                table,
                                safeText(
                                                parameter.parameterName(),
                                                parameter.parameterCode()),
                                FONT_TABLE,
                                Element.ALIGN_LEFT);

                /*
                 * IMPORTANT:
                 *
                 * The renderer does not calculate or modify the result.
                 * The backend value is authoritative.
                 */

                addParameterCell(
                                table,
                                formatResult(parameter),
                                getResultFont(parameter.flag()),
                                Element.ALIGN_RIGHT);

                /*
                 * Unit
                 */

                addParameterCell(
                                table,
                                safeText(
                                                parameter.unit(),
                                                "-"),
                                FONT_TABLE,
                                Element.ALIGN_CENTER);

                /*
                 * Reference range
                 */

                addParameterCell(
                                table,
                                formatReferenceRange(parameter),
                                FONT_TABLE,
                                Element.ALIGN_CENTER);

                /*
                 * Stored ResultFlag
                 */

                addFlagCell(
                                table,
                                parameter.flag());
        }

        private void addParameterCell(
                        PdfPTable table,
                        String text,
                        Font font,
                        int alignment) {

                PdfPCell cell = new PdfPCell(
                                new Phrase(
                                                safeText(text, "-"),
                                                font));

                cell.setPaddingTop(4f);
                cell.setPaddingBottom(4f);
                cell.setPaddingLeft(4f);
                cell.setPaddingRight(4f);

                cell.setBorderColor(
                                COLOR_LIGHT_BORDER);

                cell.setVerticalAlignment(
                                Element.ALIGN_MIDDLE);

                cell.setHorizontalAlignment(
                                alignment);

                table.addCell(cell);
        }

        private void addFlagCell(
                        PdfPTable table,
                        ResultFlag flag) {

                String text = formatFlag(flag);

                Font font = getFlagFont(flag);

                PdfPCell cell = new PdfPCell(
                                new Phrase(
                                                text,
                                                font));

                cell.setPaddingTop(4f);
                cell.setPaddingBottom(4f);
                cell.setPaddingLeft(3f);
                cell.setPaddingRight(3f);

                cell.setHorizontalAlignment(
                                Element.ALIGN_CENTER);

                cell.setVerticalAlignment(
                                Element.ALIGN_MIDDLE);

                cell.setBorderColor(
                                COLOR_LIGHT_BORDER);

                /*
                 * Very subtle background differentiation.
                 * Text remains the primary visual indicator.
                 */

                if (flag == ResultFlag.CRITICAL_LOW
                                || flag == ResultFlag.CRITICAL_HIGH) {

                        cell.setBackgroundColor(
                                        new Color(252, 238, 238));

                } else if (flag == ResultFlag.LOW
                                || flag == ResultFlag.HIGH) {

                        cell.setBackgroundColor(
                                        new Color(255, 246, 246));

                } else if (flag == ResultFlag.NORMAL) {

                        cell.setBackgroundColor(
                                        new Color(245, 250, 247));
                }

                table.addCell(cell);
        }

        private void addTableHeader(
                        PdfPTable table,
                        String text) {

                PdfPCell cell = new PdfPCell(
                                new Phrase(
                                                text,
                                                FONT_TABLE_HEADER));

                cell.setPaddingTop(5f);
                cell.setPaddingBottom(5f);
                cell.setPaddingLeft(4f);
                cell.setPaddingRight(4f);

                cell.setHorizontalAlignment(
                                Element.ALIGN_CENTER);

                cell.setVerticalAlignment(
                                Element.ALIGN_MIDDLE);

                cell.setBackgroundColor(
                                COLOR_PRIMARY);

                cell.setBorderColor(
                                COLOR_PRIMARY_DARK);

                table.addCell(cell);
        }

        /*
         * ============================================================
         * SIGN-OFF
         * ============================================================
         */

        private void addSignOffSection(
                        Document document,
                        ReportPdfData data) throws DocumentException {

                document.add(
                                createSpacer(12f));

                addSectionTitle(
                                document,
                                "REPORT AUTHENTICATION");

                PdfPTable table = new PdfPTable(2);

                table.setWidthPercentage(100);

                table.setWidths(
                                new float[] {
                                                50f,
                                                50f
                                });

                UserPdfSnapshot createdBy = data.createdBy();

                UserPdfSnapshot finalizedBy = data.finalizedBy();

                addSignOffCell(
                                table,
                                "REPORT PREPARED BY",
                                createdBy,
                                false,
                                data.organization());

                addSignOffCell(
                                table,
                                "VERIFIED & FINALIZED BY",
                                finalizedBy,
                                true,
                                data.organization());

                document.add(table);
        }

        private void addSignOffCell(
                        PdfPTable table,
                        String heading,
                        UserPdfSnapshot user,
                        boolean includeSignature,
                        OrganizationPdfSnapshot organization) {

                PdfPCell cell = new PdfPCell();

                cell.setPadding(7f);
                cell.setMinimumHeight(
                                includeSignature ? 105f : 65f);

                cell.setBorderColor(
                                COLOR_BORDER);

                Paragraph headingParagraph = new Paragraph(
                                heading,
                                FONT_SMALL_BOLD);

                headingParagraph.setSpacingAfter(5f);

                cell.addElement(
                                headingParagraph);

                if (includeSignature
                                && !isBlank(
                                                organization.signatureStorageKey())) {

                        addSignatureImage(
                                        cell,
                                        organization.signatureStorageKey());
                }

                String name = user != null
                                ? safeText(
                                                user.name(),
                                                "-")
                                : "-";

                Paragraph nameParagraph = new Paragraph(
                                name,
                                FONT_TABLE_BOLD);

                nameParagraph.setSpacingBefore(2f);

                cell.addElement(
                                nameParagraph);

                if (user != null
                                && !isBlank(user.email())) {

                        Paragraph emailParagraph = new Paragraph(
                                        user.email(),
                                        FONT_SMALL);

                        emailParagraph.setSpacingBefore(1f);

                        cell.addElement(
                                        emailParagraph);
                }

                /*
                 * For the finalizer, preserve the organization-level
                 * signature ownership information captured at finalization.
                 */

                if (includeSignature
                                && !isBlank(
                                                organization.signatureOwnerName())) {

                        Paragraph ownerParagraph = new Paragraph(
                                        "Authorized Signatory: "
                                                        + organization.signatureOwnerName(),
                                        FONT_SMALL);

                        ownerParagraph.setSpacingBefore(3f);

                        cell.addElement(
                                        ownerParagraph);
                }

                table.addCell(cell);
        }

        private void addSignatureImage(
                        PdfPCell cell,
                        String signatureStorageKey) {

                if (isBlank(signatureStorageKey)) {
                        return;
                }

                try {
                        if (!fileStorageService.exists(signatureStorageKey)) {
                                throw new IllegalStateException(
                                                "Historical signature asset does not exist");
                        }

                        byte[] signatureBytes = fileStorageService.load(signatureStorageKey);

                        if (signatureBytes == null || signatureBytes.length == 0) {
                                throw new IllegalStateException(
                                                "Historical signature asset is empty");
                        }

                        Image signature = Image.getInstance(signatureBytes);

                        /*
                         * Keep the signature inside a predictable
                         * report-signature area.
                         */
                        signature.scaleToFit(
                                        150f,
                                        45f);

                        signature.setAlignment(
                                        Element.ALIGN_LEFT);

                        Paragraph imageParagraph = new Paragraph();

                        imageParagraph.setSpacingBefore(1f);
                        imageParagraph.setSpacingAfter(2f);

                        imageParagraph.add(signature);

                        cell.addElement(
                                        imageParagraph);

                } catch (Exception exception) {

                        /*
                         * Do not expose storage keys or PHI.
                         * Throw a safe application exception so the
                         * actual storage/image problem is visible during
                         * development and testing.
                         */
                        throw new IllegalStateException(
                                        "Failed to render organization signature image",
                                        exception);
                }
        }
        /*
         * ============================================================
         * VERIFICATION
         * ============================================================
         */

        private void addVerificationSection(
                        Document document,
                        ReportPdfData data) throws DocumentException {

                document.add(createSpacer(10f));

                PdfPTable table = new PdfPTable(1);

                table.setWidthPercentage(100);

                PdfPCell cell = new PdfPCell();

                cell.setPadding(8f);

                cell.setBackgroundColor(COLOR_LIGHT_BACKGROUND);

                cell.setBorderColor(COLOR_LIGHT_BORDER);

                Paragraph heading = new Paragraph(
                                "REPORT VERIFICATION",
                                FONT_SMALL_BOLD);

                heading.setAlignment(
                                Element.ALIGN_CENTER);

                cell.addElement(heading);

                if (!isBlank(data.verificationUrl())) {

                        Paragraph url = new Paragraph(
                                        data.verificationUrl(),
                                        FONT_SMALL);

                        url.setAlignment(
                                        Element.ALIGN_CENTER);

                        url.setSpacingBefore(3f);

                        cell.addElement(url);
                }

                Paragraph notice = new Paragraph(
                                VERIFICATION_MESSAGE,
                                FONT_SMALL);

                notice.setAlignment(
                                Element.ALIGN_CENTER);

                notice.setSpacingBefore(3f);

                cell.addElement(notice);

                if (data.organization() != null
                                && !isBlank(
                                                data.organization().reportDisclaimer())) {

                        Paragraph disclaimer = new Paragraph(
                                        data.organization().reportDisclaimer(),
                                        FONT_SMALL);

                        disclaimer.setAlignment(
                                        Element.ALIGN_CENTER);

                        disclaimer.setSpacingBefore(3f);

                        cell.addElement(disclaimer);
                }

                table.addCell(cell);

                document.add(table);
        }

        /*
         * ============================================================
         * SECTION / SPACING
         * ============================================================
         */

        private void addSectionTitle(
                        Document document,
                        String title) throws DocumentException {

                Paragraph paragraph = new Paragraph(
                                title,
                                FONT_SUBSECTION);

                paragraph.setSpacingBefore(2f);
                paragraph.setSpacingAfter(5f);

                document.add(paragraph);
        }

        private void addHorizontalRule(
                        Document document) throws DocumentException {

                PdfPTable rule = new PdfPTable(1);

                rule.setWidthPercentage(100);

                PdfPCell cell = new PdfPCell(
                                new Phrase(""));

                cell.setFixedHeight(2f);

                cell.setBorder(
                                Rectangle.NO_BORDER);

                cell.setBackgroundColor(
                                COLOR_PRIMARY);

                rule.addCell(cell);

                document.add(rule);

                document.add(
                                createSpacer(7f));
        }

        private void addBlankRuleSpacer(
                        Document document) throws DocumentException {

                PdfPTable rule = new PdfPTable(1);

                rule.setWidthPercentage(100);

                PdfPCell cell = new PdfPCell(
                                new Phrase(""));

                cell.setFixedHeight(2f);

                cell.setBorder(
                                Rectangle.NO_BORDER);

                rule.addCell(cell);

                document.add(rule);

                document.add(
                                createSpacer(7f));
        }

        private Paragraph createSpacer(
                        float height) {

                Paragraph spacer = new Paragraph(
                                " ",
                                FONT_SMALL);

                spacer.setLeading(height);

                return spacer;
        }

        /*
         * ============================================================
         * PAGE EVENT
         * ============================================================
         */

        private static final class ReportPageEvent
                        extends PdfPageEventHelper {

                private final ReportPdfData data;
                private final Image qrImage;

                private ReportPageEvent(
                                ReportPdfData data,
                                Image qrImage) {
                        this.data = data;
                        this.qrImage = qrImage;
                }

                @Override
                public void onEndPage(
                                PdfWriter writer,
                                Document document) {

                        PdfContentByte canvas = writer.getDirectContent();

                        /*
                         * Footer separator.
                         */

                        canvas.setColorStroke(
                                        COLOR_LIGHT_BORDER);

                        canvas.setLineWidth(
                                        0.5f);

                        canvas.moveTo(
                                        document.left(),
                                        document.bottom() - 8f);

                        canvas.lineTo(
                                        document.right(),
                                        document.bottom() - 8f);

                        canvas.stroke();

                        /*
                         * Verification QR code.
                         *
                         * Rendered on every page using direct canvas coordinates.
                         * Placed in the left footer area below the separator line.
                         */

                        float qrWidth = 34f;
                        float qrHeight = 34f;
                        float qrX = document.left();
                        float qrY = document.bottom() - 44f;

                        if (qrImage == null) {
                                throw new IllegalStateException(
                                                "Verification QR code image is required on every PDF page");
                        }

                        try {
                                Image pageQr = Image.getInstance(qrImage);
                                pageQr.scaleAbsolute(qrWidth, qrHeight);
                                pageQr.setAbsolutePosition(qrX, qrY);
                                canvas.addImage(pageQr);
                        } catch (DocumentException exception) {
                                throw new IllegalStateException(
                                                "Failed to render verification QR code on PDF page",
                                                exception);
                        }

                        /*
                         * Left footer.
                         *
                         * Positioned to the right of the QR code so there is no overlap.
                         */

                        String reportId = data != null
                                        ? safeText(
                                                        data.reportRefId(),
                                                        "")
                                        : "";

                        Phrase leftFooter = new Phrase(
                                        reportId.isBlank()
                                                        ? ""
                                                        : "Report ID: " + reportId,
                                        FONT_SMALL);

                        ColumnText.showTextAligned(
                                        canvas,
                                        Element.ALIGN_LEFT,
                                        leftFooter,
                                        document.left() + qrWidth + 6f,
                                        document.bottom() - 22f,
                                        0);

                        /*
                         * Center footer.
                         *
                         * Organization-controlled footer only.
                         * No automatic SwasthAI branding.
                         */

                        String footerText = data != null
                                        && data.organization() != null
                                                        ? safeText(
                                                                        data.organization()
                                                                                        .reportFooterText(),
                                                                        "")
                                                        : "";

                        if (!footerText.isBlank()) {

                                Phrase centerFooter = new Phrase(
                                                footerText,
                                                FONT_SMALL);

                                ColumnText.showTextAligned(
                                                canvas,
                                                Element.ALIGN_CENTER,
                                                centerFooter,
                                                (document.left()
                                                                + document.right()) / 2f,
                                                document.bottom() - 22f,
                                                0);
                        }

                        /*
                         * Right footer.
                         */

                        Phrase pagePhrase = new Phrase(
                                        "Page "
                                                        + writer.getPageNumber(),
                                        FONT_SMALL);

                        ColumnText.showTextAligned(
                                        canvas,
                                        Element.ALIGN_RIGHT,
                                        pagePhrase,
                                        document.right(),
                                        document.bottom() - 22f,
                                        0);
                }
        }

        /*
         * ============================================================
         * FORMATTING
         * ============================================================
         */

        private String formatPatientName(
                        PatientPdfSnapshot patient) {

                String salutation = safeText(
                                patient.salutation(),
                                "");

                String name = safeText(
                                patient.name(),
                                "-");

                if (salutation.isBlank()) {
                        return name;
                }

                return salutation + " " + name;
        }

        private String formatAgeGender(
                        PatientPdfSnapshot patient) {

                String age = "-";

                if (patient.ageValue() != null) {

                        String unit = safeText(
                                        patient.ageUnit(),
                                        "");

                        age = patient.ageValue()
                                        + (unit.isBlank()
                                                        ? ""
                                                        : " " + unit);
                }

                String gender = safeText(
                                patient.gender(),
                                "-");

                return age + " / " + gender;
        }

        private String formatDateOfBirth(
                        PatientPdfSnapshot patient) {

                if (Boolean.TRUE.equals(
                                patient.dateOfBirthKnown())) {

                        if (patient.dateOfBirth() != null) {

                                return patient.dateOfBirth()
                                                .format(
                                                                DATE_FORMATTER);
                        }

                        return "-";
                }

                return "Not provided";
        }

        private String formatWeight(
                        PatientPdfSnapshot patient) {

                if (patient.weightKg() == null) {
                        return "-";
                }

                return formatDecimal(
                                patient.weightKg()) + " kg";
        }

        private String formatInstant(
                        Instant instant) {

                if (instant == null) {
                        return "-";
                }

                return DATE_TIME_FORMATTER.format(
                                instant);
        }

        private String formatResult(
                        ParameterPdfItem parameter) {

                /*
                 * Backend result string is authoritative.
                 */

                if (!isBlank(parameter.value())) {
                        return parameter.value();
                }

                /*
                 * Numeric fallback is only used if the display value
                 * itself is unavailable.
                 */

                if (parameter.numericValue() != null) {

                        return formatDecimal(
                                        parameter.numericValue());
                }

                return "-";
        }

        private String formatReferenceRange(
                        ParameterPdfItem parameter) {

                BigDecimal min = parameter.referenceMin();

                BigDecimal max = parameter.referenceMax();

                if (min != null && max != null) {

                        return formatDecimal(min)
                                        + " - "
                                        + formatDecimal(max);
                }

                if (min != null) {

                        return ">= "
                                        + formatDecimal(min);
                }

                if (max != null) {

                        return "<= "
                                        + formatDecimal(max);
                }

                return "-";
        }

        private String formatFlag(
                        ResultFlag flag) {

                if (flag == null) {
                        return "-";
                }

                return switch (flag) {

                        case NORMAL ->
                                "NORMAL";

                        case LOW ->
                                "LOW";

                        case HIGH ->
                                "HIGH";

                        case CRITICAL_LOW ->
                                "CRITICAL LOW";

                        case CRITICAL_HIGH ->
                                "CRITICAL HIGH";
                };
        }

        private Font getFlagFont(
                        ResultFlag flag) {

                if (flag == null) {
                        return FONT_TABLE;
                }

                return switch (flag) {

                        case NORMAL ->
                                FONT_NORMAL_FLAG;

                        case LOW, HIGH ->
                                FONT_ABNORMAL_FLAG;

                        case CRITICAL_LOW, CRITICAL_HIGH ->
                                FONT_CRITICAL_FLAG;
                };
        }

        private Font getResultFont(
                        ResultFlag flag) {

                if (flag == null
                                || flag == ResultFlag.NORMAL) {

                        return FONT_TABLE;
                }

                return FONT_TABLE_BOLD;
        }

        private String formatDecimal(
                        BigDecimal value) {

                if (value == null) {
                        return "-";
                }

                return value
                                .stripTrailingZeros()
                                .toPlainString();
        }

        /*
         * ============================================================
         * TEXT HELPERS
         * ============================================================
         */

        private void appendText(
                        StringBuilder builder,
                        String value) {

                if (isBlank(value)) {
                        return;
                }

                if (!builder.isEmpty()) {
                        builder.append(", ");
                }

                builder.append(
                                value.trim());
        }

        private void appendSeparator(
                        StringBuilder builder) {

                if (!builder.isEmpty()) {
                        builder.append(" | ");
                }
        }

        private static String safeText(
                        String value,
                        String fallback) {

                if (value == null
                                || value.isBlank()) {

                        return fallback;
                }

                return value.trim();
        }

        private boolean isBlank(
                        String value) {

                return value == null
                                || value.isBlank();
        }

}