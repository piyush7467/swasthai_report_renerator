package com.swasthai.report_generator.report.pdf;

import com.swasthai.report_generator.storage.FileStorageService;
import com.swasthai.report_generator.test.entity.ResultFlag;
import lombok.RequiredArgsConstructor;
import org.openpdf.text.Chunk;
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
import org.openpdf.text.pdf.Barcode128;
import org.openpdf.text.pdf.ColumnText;
import org.openpdf.text.pdf.PdfContentByte;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfPageEventHelper;
import org.openpdf.text.pdf.PdfTemplate;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OpenPdfRenderer implements PdfRenderer {

    private static final String REPORT_TITLE_PILL = "LABORATORY REPORT";
    private static final String VERIFICATION_MESSAGE = "This report is electronically generated and verified.";

    private static final ZoneId REPORT_ZONE = ZoneId.of("Asia/Kolkata");

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(REPORT_ZONE);

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(REPORT_ZONE);

    private final VerificationQrCodeGenerator verificationQrCodeGenerator;
    private final FileStorageService fileStorageService;

    /*
     * ============================================================
     * PAGE DIMENSIONS & MARGINS
     * ============================================================
     */
    private static final float MARGIN_LEFT = 30f;
    private static final float MARGIN_RIGHT = 30f;
    private static final float MARGIN_TOP = 28f;
    private static final float MARGIN_BOTTOM = 36f;

    /*
     * ============================================================
     * COLOR PALETTE (Matching Reference Medical Layout)
     * ============================================================
     */
    private static final Color COLOR_PRIMARY = new Color(24, 90, 157);        // #185A9D Deep Medical Blue
    private static final Color COLOR_PRIMARY_DARK = new Color(15, 44, 89);   // #0F2C59 Navy
    private static final Color COLOR_TEAL_ACCENT = new Color(13, 148, 136);  // #0D9488 Medical Teal
    private static final Color COLOR_HEADER_BG = new Color(220, 233, 246);   // #DCE9F6 Soft Clinical Header Blue
    private static final Color COLOR_CARD_BORDER = new Color(203, 213, 225); // #CBD5E1 Slate 300
    private static final Color COLOR_ROW_BORDER = new Color(241, 245, 249);  // #F1F5F9 Slate 100
    private static final Color COLOR_SUBHEADER_BG = new Color(241, 245, 249);// #F1F5F9 Soft Slate Tint
    private static final Color COLOR_TEXT = new Color(15, 23, 42);           // #0F172A Slate 900
    private static final Color COLOR_MUTED = new Color(100, 116, 139);       // #64748B Slate 500

    // Abnormal & Normal Flag Colors
    private static final Color COLOR_FLAG_NORMAL_BG = new Color(234, 247, 238);   // #EAF7EE Soft Green
    private static final Color COLOR_FLAG_NORMAL_TEXT = new Color(21, 128, 61);   // #15803D Forest Green
    private static final Color COLOR_FLAG_NORMAL_BORDER = new Color(194, 231, 203);// #C2E7CB
    private static final Color COLOR_FLAG_ABNORMAL_BG = new Color(253, 232, 232); // #FDE8E8 Soft Red/Pink
    private static final Color COLOR_FLAG_ABNORMAL_TEXT = new Color(220, 38, 38); // #DC2626 Red
    private static final Color COLOR_FLAG_ABNORMAL_BORDER = new Color(251, 208, 208);// #FBD0D0
    private static final Color COLOR_FLAG_CRITICAL_BG = new Color(254, 226, 226); // #FEE2E2
    private static final Color COLOR_FLAG_CRITICAL_TEXT = new Color(153, 27, 27); // #991B1B Deep Red

    // Category Section Ribbons
    private static final Color COLOR_BANNER_HAEMATOLOGY = new Color(24, 90, 157); // #185A9D
    private static final Color COLOR_BANNER_BIOCHEMISTRY = new Color(88, 44, 131); // #582C83 Royal Purple
    private static final Color COLOR_BANNER_MICROBIOLOGY = new Color(13, 148, 136); // #0D9488
    private static final Color COLOR_BANNER_DEFAULT = new Color(30, 58, 138);     // #1E3A8A

    /*
     * ============================================================
     * FONTS
     * ============================================================
     */
    private static final Font FONT_ORG_NAME = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15, COLOR_PRIMARY_DARK);
    private static final Font FONT_ORG_SUBTITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, COLOR_TEAL_ACCENT);
    private static final Font FONT_ORG_CONTACT = FontFactory.getFont(FontFactory.HELVETICA, 7.5f, COLOR_TEXT);
    private static final Font FONT_ORG_TAGLINE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, COLOR_PRIMARY);

    private static final Font FONT_CARD_LABEL = FontFactory.getFont(FontFactory.HELVETICA, 7.5f, COLOR_TEXT);
    private static final Font FONT_CARD_VALUE = FontFactory.getFont(FontFactory.HELVETICA, 7.5f, COLOR_TEXT);
    private static final Font FONT_CARD_VALUE_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.8f, COLOR_TEXT);

    private static final Font FONT_PILL_TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10.5f, COLOR_PRIMARY);

    private static final Font FONT_BANNER_TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9.5f, Color.WHITE);
    private static final Font FONT_BANNER_MOTTO = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8f, new Color(241, 245, 249));

    private static final Font FONT_TABLE_HEADER = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.8f, COLOR_PRIMARY_DARK);
    private static final Font FONT_SUBHEADER = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8f, COLOR_PRIMARY_DARK);

    private static final Font FONT_ROW = FontFactory.getFont(FontFactory.HELVETICA, 7.6f, COLOR_TEXT);
    private static final Font FONT_ROW_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8f, COLOR_TEXT);

    private static final Font FONT_FLAG_NORMAL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.2f, COLOR_FLAG_NORMAL_TEXT);
    private static final Font FONT_FLAG_ABNORMAL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.2f, COLOR_FLAG_ABNORMAL_TEXT);
    private static final Font FONT_FLAG_CRITICAL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.2f, COLOR_FLAG_CRITICAL_TEXT);

    private static final Font FONT_FOOTER_DOC_NAME = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9f, COLOR_PRIMARY_DARK);
    private static final Font FONT_FOOTER_DOC_TITLE = FontFactory.getFont(FontFactory.HELVETICA, 7.5f, COLOR_TEXT);
    private static final Font FONT_FOOTER_DOC_DESIG = FontFactory.getFont(FontFactory.HELVETICA, 7.5f, COLOR_MUTED);

    private static final Font FONT_FOOTER_VERIF_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8f, COLOR_TEXT);
    private static final Font FONT_FOOTER_VERIF_MUTED = FontFactory.getFont(FontFactory.HELVETICA, 6.8f, COLOR_MUTED);
    private static final Font FONT_PAGE_NUMBER = FontFactory.getFont(FontFactory.HELVETICA, 7.5f, COLOR_TEXT);

    @Override
    public byte[] render(ReportPdfData data) {
        validateInput(data);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Document document = new Document(
                PageSize.A4,
                MARGIN_LEFT,
                MARGIN_RIGHT,
                MARGIN_TOP,
                MARGIN_BOTTOM
        );

        try {
            PdfWriter writer = PdfWriter.getInstance(document, outputStream);

            Image qrImage = verificationQrCodeGenerator.generate(data.verificationUrl());
            if (qrImage == null) {
                throw new IllegalStateException("Failed to generate verification QR code image");
            }

            ReportPageEvent pageEvent = new ReportPageEvent();
            writer.setPageEvent(pageEvent);

            document.open();

            // 1. Organization Header (Optional: rendered or blank spacer)
            addOrganizationHeader(document, data);

            // 2. Patient Demographics Card (3-column bordered box with Barcode & QR)
            addPatientCard(document, data, writer, qrImage);

            // 3. Centered Pill: LABORATORY REPORT
            addReportTitlePill(document);

            // 4. Test Categories, Banners & Result Tables
            addCategorySectionsAndTables(document, data);

            // 5. Doctor Signature & Verification Footer
            addSignOffAndVerificationFooter(document, data, qrImage);

            document.close();
            return outputStream.toByteArray();

        } catch (DocumentException exception) {
            throw new IllegalStateException("Failed to generate report PDF", exception);
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }
    }

    private void validateInput(ReportPdfData data) {
        if (data == null) {
            throw new IllegalArgumentException("PDF report data cannot be null");
        }
        if (isBlank(data.reportRefId())) {
            throw new IllegalStateException("PDF report is missing report reference ID");
        }
        if (data.status() == null || !"FINALIZED".equals(data.status().name())) {
            throw new IllegalStateException("Only finalized reports can be rendered as PDF");
        }
        if (data.organization() == null || isBlank(data.organization().name())) {
            throw new IllegalStateException("PDF report is missing organization information");
        }
        if (data.patient() == null || isBlank(data.patient().name()) || isBlank(data.patient().patientCode())) {
            throw new IllegalStateException("PDF report is missing required patient information");
        }
        if (data.tests() == null || data.tests().isEmpty()) {
            throw new IllegalStateException("PDF report contains no tests");
        }
        if (data.finalizedBy() == null || isBlank(data.finalizedBy().name())) {
            throw new IllegalStateException("PDF report is missing finalizer information");
        }
        if (isBlank(data.verificationUrl())) {
            throw new IllegalStateException("PDF report is missing verification URL");
        }
    }

    /*
     * ============================================================
     * 1. TOP ORGANIZATION HEADER
     * ============================================================
     */
    private void addOrganizationHeader(Document document, ReportPdfData data) throws DocumentException {
        OrganizationPdfSnapshot organization = data.organization();

        PdfPTable header = new PdfPTable(3);
        header.setWidthPercentage(100);
        header.setWidths(new float[]{ 38f, 38f, 24f });
        header.setSpacingAfter(6f);

        // --- Left: Logo & Hospital Name & Subtitle ---
        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.NO_BORDER);
        leftCell.setPadding(0f);

        PdfPTable brandTable = new PdfPTable(2);
        brandTable.setWidthPercentage(100);
        brandTable.setWidths(new float[]{ 24f, 76f });

        PdfPCell logoCell = createLogoCell(organization.logoStorageKey());
        brandTable.addCell(logoCell);

        PdfPCell nameCell = new PdfPCell();
        nameCell.setBorder(Rectangle.NO_BORDER);
        nameCell.setPaddingLeft(5f);
        nameCell.setPaddingTop(2f);

        Paragraph orgName = new Paragraph(
                safeText(organization.name(), "LABORATORY"),
                FONT_ORG_NAME
        );
        orgName.setSpacingAfter(1f);
        nameCell.addElement(orgName);

        if (!isBlank(organization.reportFooterText())) {
            Paragraph subtitle = new Paragraph(
                    organization.reportFooterText(),
                    FONT_ORG_SUBTITLE
            );
            nameCell.addElement(subtitle);
        }

        brandTable.addCell(nameCell);
        leftCell.addElement(brandTable);
        header.addCell(leftCell);

        // --- Middle: Address, Phone, Email, Website ---
        PdfPCell midCell = new PdfPCell();
        midCell.setBorder(Rectangle.NO_BORDER);
        midCell.setPaddingLeft(6f);
        midCell.setPaddingTop(2f);

        String address = buildOrganizationAddress(organization);
        if (!address.isBlank()) {
            midCell.addElement(new Paragraph("• " + address, FONT_ORG_CONTACT));
        }
        if (!isBlank(organization.phone())) {
            midCell.addElement(new Paragraph("• " + organization.phone(), FONT_ORG_CONTACT));
        }
        if (!isBlank(organization.email())) {
            midCell.addElement(new Paragraph("• " + organization.email(), FONT_ORG_CONTACT));
        }
        if (!isBlank(organization.website())) {
            midCell.addElement(new Paragraph("• " + organization.website(), FONT_ORG_CONTACT));
        }

        header.addCell(midCell);

        // --- Right: Tagline / Disclaimer (if configured) ---
        PdfPCell rightCell = new PdfPCell();
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.setPaddingLeft(10f);
        rightCell.setPaddingTop(8f);

        if (!isBlank(organization.reportDisclaimer())) {
            PdfPTable taglineTable = new PdfPTable(2);
            taglineTable.setWidthPercentage(100);
            taglineTable.setWidths(new float[]{ 4f, 96f });

            PdfPCell dividerCell = new PdfPCell();
            dividerCell.setBorder(Rectangle.NO_BORDER);
            dividerCell.setBackgroundColor(COLOR_PRIMARY);
            dividerCell.setFixedHeight(24f);
            taglineTable.addCell(dividerCell);

            PdfPCell tagTextCell = new PdfPCell();
            tagTextCell.setBorder(Rectangle.NO_BORDER);
            tagTextCell.setPaddingLeft(6f);
            tagTextCell.setPaddingTop(2f);

            Paragraph line1 = new Paragraph(organization.reportDisclaimer(), FONT_ORG_TAGLINE);
            tagTextCell.addElement(line1);
            taglineTable.addCell(tagTextCell);
            rightCell.addElement(taglineTable);
        }
        header.addCell(rightCell);

        if (Boolean.TRUE.equals(data.includeOrganizationHeader())) {
            document.add(header);
        } else {
            // Preserve exact calculated space for pre-printed letterheads
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
        }
    }

    private PdfPCell createLogoCell(String logoStorageKey) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(0f);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        if (!isBlank(logoStorageKey)) {
            try {
                if (fileStorageService.exists(logoStorageKey)) {
                    byte[] imageBytes = fileStorageService.load(logoStorageKey);
                    Image image = Image.getInstance(imageBytes);
                    image.scaleToFit(50f, 45f);
                    image.setAlignment(Element.ALIGN_LEFT);
                    cell.addElement(image);
                    return cell;
                }
            } catch (Exception ignored) {
            }
        }

        return cell;
    }

    /*
     * ============================================================
     * 2. PATIENT DEMOGRAPHICS CARD
     * ============================================================
     */
    private void addPatientCard(Document document, ReportPdfData data, PdfWriter writer, Image qrImage) throws DocumentException {
        PatientPdfSnapshot patient = data.patient();

        PdfPTable card = new PdfPTable(3);
        card.setWidthPercentage(100);
        card.setWidths(new float[]{ 38f, 38f, 24f });
        card.setSpacingAfter(8f);

        // --- Column 1: Patient Details ---
        PdfPCell col1 = new PdfPCell();
        col1.setBorder(Rectangle.BOX);
        col1.setBorderColor(COLOR_CARD_BORDER);
        col1.setBorderWidth(0.8f);
        col1.setPadding(6f);

        addKeyValueLine(col1, "Patient Name", formatPatientName(patient), true);
        addKeyValueLine(col1, "Age / Sex", formatAgeGender(patient), false);
        addKeyValueLine(col1, "Patient ID", safeText(patient.patientCode(), "-"), false);
        addKeyValueLine(col1, "Referred By", safeText(data.organization().name(), "Self / Direct"), false);
        addKeyValueLine(col1, "Address", safeText(patient.address(), "-"), false);
        addKeyValueLine(col1, "Mobile No", safeText(patient.phone(), "-"), false);
        card.addCell(col1);

        // --- Column 2: Registration & Sample Dates ---
        PdfPCell col2 = new PdfPCell();
        col2.setBorder(Rectangle.BOX);
        col2.setBorderColor(COLOR_CARD_BORDER);
        col2.setBorderWidth(0.8f);
        col2.setPadding(6f);

        addKeyValueLine(col2, "Registered On", formatDate(data.createdAt()), false);
        addKeyValueLine(col2, "Sample Collected", formatDateTime(data.createdAt()), false);
        addKeyValueLine(col2, "Reported On", formatDateTime(data.finalizedAt()), false);
        addKeyValueLine(col2, "Printed On", formatDateTime(Instant.now()), false);
        card.addCell(col2);

        // --- Column 3: Barcode + Verification QR Code ---
        PdfPCell col3 = new PdfPCell();
        col3.setBorder(Rectangle.BOX);
        col3.setBorderColor(COLOR_CARD_BORDER);
        col3.setBorderWidth(0.8f);
        col3.setPadding(4f);
        col3.setHorizontalAlignment(Element.ALIGN_CENTER);

        // 1D Barcode 128
        try {
            Barcode128 barcode = new Barcode128();
            barcode.setCode(safeText(patient.patientCode(), data.reportRefId()));
            barcode.setCodeType(Barcode128.CODE128);
            barcode.setBarHeight(16f);
            barcode.setSize(6.5f);
            barcode.setTextAlignment(Element.ALIGN_CENTER);
            Image barcodeImage = barcode.createImageWithBarcode(writer.getDirectContent(), null, null);
            barcodeImage.setAlignment(Element.ALIGN_CENTER);
            barcodeImage.scalePercent(80f);
            col3.addElement(barcodeImage);
        } catch (Exception e) {
            Paragraph bcFallback = new Paragraph(safeText(patient.patientCode(), "-"), FONT_CARD_LABEL);
            bcFallback.setAlignment(Element.ALIGN_CENTER);
            col3.addElement(bcFallback);
        }

        // Small QR Code
        try {
            Image smallQr = Image.getInstance(qrImage);
            smallQr.scaleToFit(38f, 38f);
            smallQr.setAlignment(Element.ALIGN_CENTER);
            smallQr.setSpacingBefore(3f);
            col3.addElement(smallQr);

            Paragraph qrCaption = new Paragraph("Scan for Verification", FontFactory.getFont(FontFactory.HELVETICA, 6.2f, COLOR_MUTED));
            qrCaption.setAlignment(Element.ALIGN_CENTER);
            qrCaption.setSpacingBefore(1f);
            col3.addElement(qrCaption);
        } catch (Exception ignored) {
        }

        card.addCell(col3);
        document.add(card);
    }

    private void addKeyValueLine(PdfPCell cell, String key, String value, boolean isBold) {
        Paragraph p = new Paragraph();
        p.setLeading(11f);
        p.add(new Phrase(String.format("%-16s:  ", key), FONT_CARD_LABEL));
        p.add(new Phrase(value, isBold ? FONT_CARD_VALUE_BOLD : FONT_CARD_VALUE));
        cell.addElement(p);
    }

    /*
     * ============================================================
     * 3. CENTERED PILL: LABORATORY REPORT
     * ============================================================
     */
    private void addReportTitlePill(Document document) throws DocumentException {
        PdfPTable pillTable = new PdfPTable(1);
        pillTable.setWidthPercentage(44);
        pillTable.setSpacingAfter(7f);

        PdfPCell pillCell = new PdfPCell(new Phrase(REPORT_TITLE_PILL, FONT_PILL_TITLE));
        pillCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        pillCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        pillCell.setPaddingTop(3.5f);
        pillCell.setPaddingBottom(4.5f);
        pillCell.setBorderColor(COLOR_PRIMARY);
        pillCell.setBorderWidth(1.2f);
        pillCell.setBackgroundColor(new Color(248, 251, 255));

        pillTable.addCell(pillCell);
        document.add(pillTable);
    }

    /*
     * ============================================================
     * 4. CATEGORY SECTIONS, BANNERS & TABLES
     * ============================================================
     */
    private void addCategorySectionsAndTables(Document document, ReportPdfData data) throws DocumentException {
        List<TestPdfItem> tests = data.tests() == null ? Collections.emptyList() : data.tests();

        // Group tests by reportSection
        Map<String, List<TestPdfItem>> sections = new LinkedHashMap<>();
        for (TestPdfItem test : tests) {
            if (test == null) continue;
            String sectionName = safeText(test.reportSection(), "GENERAL INVESTIGATIONS").toUpperCase();
            sections.computeIfAbsent(sectionName, k -> new ArrayList<>()).add(test);
        }

        for (Map.Entry<String, List<TestPdfItem>> entry : sections.entrySet()) {
            String sectionName = entry.getKey();
            List<TestPdfItem> sectionTests = entry.getValue();

            // Render department colored banner
            addDepartmentBanner(document, sectionName);

            // Render table for this section
            addInvestigationTable(document, sectionTests);

            document.add(new Paragraph(" ", FontFactory.getFont(FontFactory.HELVETICA, 4f)));
        }
    }

    private void addDepartmentBanner(Document document, String sectionName) throws DocumentException {
        PdfPTable banner = new PdfPTable(2);
        banner.setWidthPercentage(100);
        banner.setWidths(new float[]{ 70f, 30f });
        banner.setSpacingBefore(3f);
        banner.setSpacingAfter(0f);

        Color bannerColor = getBannerColor(sectionName);
        String motto = getSectionMotto(sectionName);

        PdfPCell leftCell = new PdfPCell(new Phrase("  ⚗  " + sectionName, FONT_BANNER_TITLE));
        leftCell.setBorder(Rectangle.NO_BORDER);
        leftCell.setBackgroundColor(bannerColor);
        leftCell.setPaddingTop(4.5f);
        leftCell.setPaddingBottom(4.5f);
        leftCell.setPaddingLeft(6f);
        leftCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        banner.addCell(leftCell);

        PdfPCell rightCell = new PdfPCell(new Phrase(motto + "  ", FONT_BANNER_MOTTO));
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.setBackgroundColor(bannerColor);
        rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        rightCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        rightCell.setPaddingTop(4.5f);
        rightCell.setPaddingBottom(4.5f);
        banner.addCell(rightCell);

        document.add(banner);
    }

    private Color getBannerColor(String sectionName) {
        if (sectionName.contains("HAEMATOLOGY") || sectionName.contains("HEMATOLOGY")) {
            return COLOR_BANNER_HAEMATOLOGY;
        } else if (sectionName.contains("BIOCHEMISTRY")) {
            return COLOR_BANNER_BIOCHEMISTRY;
        } else if (sectionName.contains("MICROBIOLOGY")) {
            return COLOR_BANNER_MICROBIOLOGY;
        }
        return COLOR_BANNER_DEFAULT;
    }

    private String getSectionMotto(String sectionName) {
        if (sectionName.contains("HAEMATOLOGY") || sectionName.contains("HEMATOLOGY")) {
            return "Small Tests. Big Insights.";
        } else if (sectionName.contains("BIOCHEMISTRY")) {
            return "Accurate Results. Better Care.";
        } else if (sectionName.contains("MICROBIOLOGY")) {
            return "Precision in Every Culture.";
        }
        return "Clinical Diagnostics";
    }

    private void addInvestigationTable(Document document, List<TestPdfItem> sectionTests) throws DocumentException {
        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{ 33f, 17f, 15f, 21f, 14f });
        table.setHeaderRows(1);
        table.setSplitRows(true);
        table.setSplitLate(false);

        // Header Row
        addTableHeaderCell(table, "Test Name", Element.ALIGN_LEFT);
        addTableHeaderCell(table, "Result", Element.ALIGN_RIGHT);
        addTableHeaderCell(table, "Unit", Element.ALIGN_CENTER);
        addTableHeaderCell(table, "Reference Range", Element.ALIGN_CENTER);
        addTableHeaderCell(table, "Flag", Element.ALIGN_CENTER);

        for (TestPdfItem test : sectionTests) {
            List<ParameterPdfItem> params = test.parameters() == null ? Collections.emptyList() : test.parameters();

            // If test has a title and multiple parameters, show a subtle panel row
            boolean showPanelHeader = sectionTests.size() > 1 || !test.testName().equalsIgnoreCase(params.isEmpty() ? "" : params.get(0).parameterName());
            if (showPanelHeader && !isBlank(test.testName())) {
                PdfPCell panelCell = new PdfPCell(new Phrase("  " + test.testName(), FONT_SUBHEADER));
                panelCell.setColspan(5);
                panelCell.setBackgroundColor(COLOR_SUBHEADER_BG);
                panelCell.setBorderColor(COLOR_CARD_BORDER);
                panelCell.setBorderWidth(0.5f);
                panelCell.setPaddingTop(3.5f);
                panelCell.setPaddingBottom(3.5f);
                table.addCell(panelCell);
            }

            for (ParameterPdfItem param : params) {
                if (param == null) continue;
                addParameterRow(table, param);
            }
        }

        document.add(table);
    }

    private void addTableHeaderCell(PdfPTable table, String text, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FONT_TABLE_HEADER));
        cell.setBackgroundColor(COLOR_HEADER_BG);
        cell.setBorderColor(COLOR_CARD_BORDER);
        cell.setBorderWidth(0.6f);
        cell.setPaddingTop(4.5f);
        cell.setPaddingBottom(4.5f);
        cell.setPaddingLeft(5f);
        cell.setPaddingRight(5f);
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(cell);
    }

    private void addParameterRow(PdfPTable table, ParameterPdfItem param) {
        boolean isAbnormal = param.flag() != null && param.flag() != ResultFlag.NORMAL;

        // 1. Parameter Name
        PdfPCell nameCell = new PdfPCell(new Phrase(safeText(param.parameterName(), param.parameterCode()), FONT_ROW));
        nameCell.setPaddingTop(3.8f);
        nameCell.setPaddingBottom(3.8f);
        nameCell.setPaddingLeft(5f);
        nameCell.setBorderColor(COLOR_ROW_BORDER);
        nameCell.setBorderWidth(0.5f);
        nameCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(nameCell);

        // 2. Result Value (Bold if abnormal)
        String resStr = formatResult(param);
        PdfPCell resCell = new PdfPCell(new Phrase(resStr, isAbnormal ? FONT_ROW_BOLD : FONT_ROW));
        resCell.setPaddingTop(3.8f);
        resCell.setPaddingBottom(3.8f);
        resCell.setPaddingRight(6f);
        resCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        resCell.setBorderColor(COLOR_ROW_BORDER);
        resCell.setBorderWidth(0.5f);
        resCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(resCell);

        // 3. Unit (Standard compact scientific notation for million/µL and thousand/µL)
        PdfPCell unitCell = new PdfPCell(createUnitPhrase(param.unit(), FONT_ROW));
        unitCell.setPaddingTop(3.8f);
        unitCell.setPaddingBottom(3.8f);
        unitCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        unitCell.setBorderColor(COLOR_ROW_BORDER);
        unitCell.setBorderWidth(0.5f);
        unitCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(unitCell);

        // 4. Reference Range
        PdfPCell refCell = new PdfPCell(new Phrase(formatReferenceRange(param), FONT_ROW));
        refCell.setPaddingTop(3.8f);
        refCell.setPaddingBottom(3.8f);
        refCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        refCell.setBorderColor(COLOR_ROW_BORDER);
        refCell.setBorderWidth(0.5f);
        refCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(refCell);

        // 5. Flag Pill Badge
        PdfPCell flagContainerCell = new PdfPCell();
        flagContainerCell.setPaddingTop(2f);
        flagContainerCell.setPaddingBottom(2f);
        flagContainerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        flagContainerCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        flagContainerCell.setBorderColor(COLOR_ROW_BORDER);
        flagContainerCell.setBorderWidth(0.5f);

        PdfPTable pill = createFlagPill(param.flag());
        flagContainerCell.addElement(pill);
        table.addCell(flagContainerCell);
    }

    private PdfPTable createFlagPill(ResultFlag flag) {
        PdfPTable pillTable = new PdfPTable(1);
        pillTable.setWidthPercentage(86);

        String text = formatFlag(flag);
        Font font = getFlagFont(flag);
        Color bg = getFlagBg(flag);
        Color border = getFlagBorder(flag);

        PdfPCell pillCell = new PdfPCell(new Phrase(text, font));
        pillCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        pillCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        pillCell.setPaddingTop(1.8f);
        pillCell.setPaddingBottom(2.2f);
        pillCell.setBackgroundColor(bg);
        pillCell.setBorderColor(border);
        pillCell.setBorderWidth(0.5f);

        pillTable.addCell(pillCell);
        return pillTable;
    }

    private Color getFlagBg(ResultFlag flag) {
        if (flag == null || flag == ResultFlag.NORMAL) {
            return COLOR_FLAG_NORMAL_BG;
        } else if (flag == ResultFlag.CRITICAL_LOW || flag == ResultFlag.CRITICAL_HIGH) {
            return COLOR_FLAG_CRITICAL_BG;
        }
        return COLOR_FLAG_ABNORMAL_BG;
    }

    private Color getFlagBorder(ResultFlag flag) {
        if (flag == null || flag == ResultFlag.NORMAL) {
            return COLOR_FLAG_NORMAL_BORDER;
        }
        return COLOR_FLAG_ABNORMAL_BORDER;
    }

    private Font getFlagFont(ResultFlag flag) {
        if (flag == null || flag == ResultFlag.NORMAL) {
            return FONT_FLAG_NORMAL;
        } else if (flag == ResultFlag.CRITICAL_LOW || flag == ResultFlag.CRITICAL_HIGH) {
            return FONT_FLAG_CRITICAL;
        }
        return FONT_FLAG_ABNORMAL;
    }

    /*
     * ============================================================
     * 5. DOCTOR SIGN-OFF & VERIFICATION FOOTER
     * ============================================================
     */
    private void addSignOffAndVerificationFooter(Document document, ReportPdfData data, Image qrImage) throws DocumentException {
        document.add(new Paragraph(" ", FontFactory.getFont(FontFactory.HELVETICA, 6f)));

        PdfPTable footer = new PdfPTable(2);
        footer.setWidthPercentage(100);
        footer.setWidths(new float[]{ 55f, 45f });
        footer.setKeepTogether(true);

        // --- Left: QR code, Report ID & Electronic Verification Notice ---
        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.NO_BORDER);
        leftCell.setPadding(0f);

        PdfPTable qrTable = new PdfPTable(3);
        qrTable.setWidthPercentage(100);
        qrTable.setWidths(new float[]{ 24f, 3f, 73f });

        // QR Image
        PdfPCell qrCell = new PdfPCell();
        qrCell.setBorder(Rectangle.NO_BORDER);
        qrCell.setPadding(0f);
        try {
            Image footerQr = Image.getInstance(qrImage);
            footerQr.scaleToFit(44f, 44f);
            footerQr.setAlignment(Element.ALIGN_LEFT);
            qrCell.addElement(footerQr);
        } catch (Exception ignored) {
        }
        qrTable.addCell(qrCell);

        // Vertical divider
        PdfPCell divCell = new PdfPCell();
        divCell.setBorder(Rectangle.NO_BORDER);
        divCell.setBackgroundColor(COLOR_PRIMARY);
        divCell.setFixedHeight(38f);
        qrTable.addCell(divCell);

        // Verification Notice Text
        PdfPCell textCell = new PdfPCell();
        textCell.setBorder(Rectangle.NO_BORDER);
        textCell.setPaddingLeft(6f);

        Paragraph reportIdP = new Paragraph("Report ID: " + safeText(data.reportRefId(), ""), FONT_FOOTER_VERIF_BOLD);
        reportIdP.setSpacingAfter(1f);
        textCell.addElement(reportIdP);

        Paragraph scanP = new Paragraph("Scan this QR code to verify the authenticity of this report.", FONT_FOOTER_VERIF_MUTED);
        scanP.setSpacingAfter(1f);
        textCell.addElement(scanP);

        Paragraph elecP = new Paragraph(VERIFICATION_MESSAGE, FONT_FOOTER_VERIF_MUTED);
        textCell.addElement(elecP);

        qrTable.addCell(textCell);
        leftCell.addElement(qrTable);
        footer.addCell(leftCell);

        // --- Right: Doctor Signature & Credentials ---
        PdfPCell rightCell = new PdfPCell();
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        rightCell.setPaddingRight(10f);

        // Digital Signature Image
        if (!isBlank(data.organization().signatureStorageKey())) {
            try {
                if (fileStorageService.exists(data.organization().signatureStorageKey())) {
                    byte[] sigBytes = fileStorageService.load(data.organization().signatureStorageKey());
                    Image sigImage = Image.getInstance(sigBytes);
                    sigImage.scaleToFit(90f, 30f);
                    sigImage.setAlignment(Element.ALIGN_RIGHT);
                    rightCell.addElement(sigImage);
                }
            } catch (Exception ignored) {
            }
        }

        String docName = !isBlank(data.organization().signatureOwnerName())
                ? data.organization().signatureOwnerName()
                : (data.finalizedBy() != null ? data.finalizedBy().name() : null);

        if (!isBlank(docName)) {
            Paragraph docNameP = new Paragraph(docName, FONT_FOOTER_DOC_NAME);
            docNameP.setAlignment(Element.ALIGN_RIGHT);
            docNameP.setSpacingBefore(2f);
            rightCell.addElement(docNameP);

            Paragraph docDesigP = new Paragraph("Authorized Signatory", FONT_FOOTER_DOC_DESIG);
            docDesigP.setAlignment(Element.ALIGN_RIGHT);
            rightCell.addElement(docDesigP);
        }

        footer.addCell(rightCell);
        document.add(footer);
    }

    /*
     * ============================================================
     * PAGE EVENT: BOTTOM PAGE NUMBERING (Page X of Y)
     * ============================================================
     */
    private static final class ReportPageEvent extends PdfPageEventHelper {
        private PdfTemplate totalPages;

        @Override
        public void onOpenDocument(PdfWriter writer, Document document) {
            totalPages = writer.getDirectContent().createTemplate(30, 16);
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte canvas = writer.getDirectContent();

            // Subtle divider line at footer
            canvas.setColorStroke(new Color(226, 232, 240));
            canvas.setLineWidth(0.6f);
            canvas.moveTo(document.left(), document.bottom() - 6f);
            canvas.lineTo(document.right(), document.bottom() - 6f);
            canvas.stroke();

            // Right: Page X of [Template]
            String pageText = "Page " + writer.getPageNumber() + " of ";
            float textSize = 7.5f;
            float textBase = document.bottom() - 18f;
            float textWidth = FontFactory.getFont(FontFactory.HELVETICA, textSize).getBaseFont().getWidthPoint(pageText, textSize);

            ColumnText.showTextAligned(
                    canvas,
                    Element.ALIGN_RIGHT,
                    new Phrase(pageText, FONT_PAGE_NUMBER),
                    document.right() - 14f,
                    textBase,
                    0
            );

            canvas.addTemplate(totalPages, document.right() - 14f, textBase);
        }

        @Override
        public void onCloseDocument(PdfWriter writer, Document document) {
            ColumnText.showTextAligned(
                    totalPages,
                    Element.ALIGN_LEFT,
                    new Phrase(String.valueOf(writer.getPageNumber() - 1), FONT_PAGE_NUMBER),
                    2f,
                    0f,
                    0
            );
        }
    }

    /*
     * ============================================================
     * FORMATTING & TEXT HELPERS
     * ============================================================
     */
    private String formatPatientName(PatientPdfSnapshot patient) {
        String salutation = safeText(patient.salutation(), "");
        String name = safeText(patient.name(), "-");
        return salutation.isBlank() ? name : salutation + " " + name;
    }

    private String formatAgeGender(PatientPdfSnapshot patient) {
        String age = "-";
        if (patient.ageValue() != null) {
            String unit = safeText(patient.ageUnit(), "Years");
            age = patient.ageValue() + " " + unit;
        }
        String gender = safeText(patient.gender(), "-");
        return age + " / " + gender;
    }

    private String formatDate(Instant instant) {
        if (instant == null) return "-";
        return DATE_FORMATTER.format(instant);
    }

    private String formatDateTime(Instant instant) {
        if (instant == null) return "-";
        return DATE_TIME_FORMATTER.format(instant);
    }

    private String formatResult(ParameterPdfItem parameter) {
        if (!isBlank(parameter.value())) {
            return parameter.value();
        }
        if (parameter.numericValue() != null) {
            return formatDecimal(parameter.numericValue());
        }
        return "-";
    }

    private String formatReferenceRange(ParameterPdfItem parameter) {
        BigDecimal min = parameter.referenceMin();
        BigDecimal max = parameter.referenceMax();
        if (min != null && max != null) {
            return formatDecimal(min) + " - " + formatDecimal(max);
        }
        if (min != null) {
            return ">= " + formatDecimal(min);
        }
        if (max != null) {
            return "<= " + formatDecimal(max);
        }
        return "-";
    }

    private String formatFlag(ResultFlag flag) {
        if (flag == null) return "NORMAL";
        return switch (flag) {
            case NORMAL -> "NORMAL";
            case LOW -> "LOW";
            case HIGH -> "HIGH";
            case CRITICAL_LOW -> "CRITICAL LOW";
            case CRITICAL_HIGH -> "CRITICAL HIGH";
        };
    }

    private String formatDecimal(BigDecimal value) {
        if (value == null) return "-";
        return value.stripTrailingZeros().toPlainString();
    }

    private Phrase createUnitPhrase(String rawUnit, Font baseFont) {
        if (rawUnit == null || rawUnit.isBlank() || "-".equals(rawUnit)) {
            return new Phrase("-", baseFont);
        }
        String u = rawUnit.trim();
        String lower = u.toLowerCase();

        // Check for million/µL or 10^6/µL (RBC count etc.) -> 10⁶/µL
        if (lower.equals("million/µl") || lower.equals("million/ul")
                || lower.equals("million / µl") || lower.equals("million / ul")
                || lower.equals("10^6/µl") || lower.equals("10^6/ul")
                || lower.equals("10^6 / µl") || lower.equals("10^6 / ul")
                || lower.equals("106/µl") || lower.equals("106/ul")
                || lower.equals("m/µl") || lower.equals("m/ul")) {
            Phrase phrase = new Phrase();
            phrase.add(new Chunk("10", baseFont));
            Font supFont = new Font(baseFont.getFamily(), baseFont.getSize() * 0.75f, baseFont.getStyle(), baseFont.getColor());
            Chunk sup = new Chunk("6", supFont);
            sup.setTextRise(baseFont.getSize() * 0.35f);
            phrase.add(sup);
            phrase.add(new Chunk("/µL", baseFont));
            return phrase;
        }

        // Check for thousand/µL or 10^3/µL (WBC, Platelets etc.) -> 10³/µL
        if (lower.equals("thousand/µl") || lower.equals("thousand/ul")
                || lower.equals("thousand / µl") || lower.equals("thousand / ul")
                || lower.equals("10^3/µl") || lower.equals("10^3/ul")
                || lower.equals("10^3 / µl") || lower.equals("10^3 / ul")
                || lower.equals("103/µl") || lower.equals("103/ul")
                || lower.equals("k/µl") || lower.equals("k/ul")) {
            Phrase phrase = new Phrase();
            phrase.add(new Chunk("10", baseFont));
            Font supFont = new Font(baseFont.getFamily(), baseFont.getSize() * 0.75f, baseFont.getStyle(), baseFont.getColor());
            Chunk sup = new Chunk("3", supFont);
            sup.setTextRise(baseFont.getSize() * 0.35f);
            phrase.add(sup);
            phrase.add(new Chunk("/µL", baseFont));
            return phrase;
        }

        return new Phrase(u, baseFont);
    }

    private String buildOrganizationAddress(OrganizationPdfSnapshot organization) {
        StringBuilder builder = new StringBuilder();
        appendText(builder, organization.addressLine1());
        appendText(builder, organization.city());
        appendText(builder, organization.postalCode());
        if (builder.isEmpty()) {
            return "123 Health Street, New Delhi - 110001";
        }
        return builder.toString();
    }

    private void appendText(StringBuilder builder, String value) {
        if (isBlank(value)) return;
        if (!builder.isEmpty()) builder.append(", ");
        builder.append(value.trim());
    }

    private static String safeText(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}