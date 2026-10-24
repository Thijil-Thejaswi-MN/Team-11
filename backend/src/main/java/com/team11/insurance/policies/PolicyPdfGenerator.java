package com.team11.insurance.policies;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

/**
 * Generates official policy schedule PDF documents using OpenPDF (P3-05 / INS-F-009 / INS-F-024).
 */
@Component
public class PolicyPdfGenerator {

    private static final Logger log = LoggerFactory.getLogger(PolicyPdfGenerator.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MMM-yyyy");

    public byte[] generatePolicyPdf(Policy policy, String planName, String customerName) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 40, 40);
            PdfWriter.getInstance(document, baos);
            document.open();

            // Fonts
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new Color(30, 41, 59));
            Font subHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, new Color(59, 130, 246));
            Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, new Color(30, 41, 59));
            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(51, 65, 85));
            Font boldBody = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new Color(30, 41, 59));
            Font footerFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, new Color(148, 163, 184));

            // Header Banner
            Paragraph title = new Paragraph("INSURANCE CERTIFICATE & POLICY SCHEDULE", headerFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(4);
            document.add(title);

            Paragraph subtitle = new Paragraph("Team 11 Insurance Automation Platform — Digitally Issued Schedule", subHeaderFont);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            subtitle.setSpacingAfter(18);
            document.add(subtitle);

            // Policy Details Table
            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{35f, 65f});
            table.setSpacingBefore(10);
            table.setSpacingAfter(15);

            addTableRow(table, "Policy Number", policy.getPolicyNo(), boldBody, true);
            addTableRow(table, "Policy Status", String.valueOf(policy.getStatus()), boldBody, false);
            addTableRow(table, "Application ID", policy.getApplicationId() != null ? String.valueOf(policy.getApplicationId()) : "N/A", bodyFont, false);
            addTableRow(table, "Policyholder (Customer)", customerName != null ? customerName + " (ID: " + policy.getCustomerId() + ")" : "Customer ID #" + policy.getCustomerId(), bodyFont, false);
            addTableRow(table, "Insurer", "Insurer Entity #" + policy.getInsurerId(), bodyFont, false);
            addTableRow(table, "Insurance Plan", planName != null ? planName + " (Plan #" + policy.getPlanId() + ")" : "Plan #" + policy.getPlanId(), bodyFont, false);

            if (policy.getVehicleRegNo() != null && !policy.getVehicleRegNo().isBlank()) {
                addTableRow(table, "Insured Vehicle Reg No", policy.getVehicleRegNo(), boldBody, false);
            }

            String startStr = policy.getStartDate() != null ? policy.getStartDate().format(DATE_FORMAT) : "N/A";
            String endStr = policy.getEndDate() != null ? policy.getEndDate().format(DATE_FORMAT) : "N/A";
            addTableRow(table, "Policy Term", startStr + " to " + endStr + " (Midnight)", bodyFont, false);

            BigDecimal premium = policy.getPremiumPaid() != null ? policy.getPremiumPaid() : BigDecimal.ZERO;
            addTableRow(table, "Gross Premium Paid", "Rs. " + premium.toPlainString() + " (Incl. applicable GST)", boldBody, true);

            document.add(table);

            // Statutory Terms
            Paragraph termsHeader = new Paragraph("Statutory Terms & Policyholder Conditions", sectionFont);
            termsHeader.setSpacingBefore(10);
            termsHeader.setSpacingAfter(6);
            document.add(termsHeader);

            Paragraph p1 = new Paragraph(
                    "1. Free-Look Period: The insured is entitled to a free-look period of 15 days from the date of policy receipt. Cancellation within this window entitles the policyholder to a 100% refund of premium, less statutory stamp duty and medical examination expenses.",
                    bodyFont
            );
            p1.setSpacingAfter(4);
            document.add(p1);

            Paragraph p2 = new Paragraph(
                    "2. Cancellation & Pro-Rata Refund: In the event of cancellation outside the free-look period, unexpired premium shall be refunded on a pro-rata basis subject to a 5% cancellation administrative fee, provided no claim has been settled under this policy term.",
                    bodyFont
            );
            p2.setSpacingAfter(4);
            document.add(p2);

            Paragraph p3 = new Paragraph(
                    "3. Claims Intimation: Notice of any claim must be lodged through the policy portal within 30 days of the occurrence of the insured event.",
                    bodyFont
            );
            p3.setSpacingAfter(15);
            document.add(p3);

            // Footer
            Paragraph footer = new Paragraph(
                    "Digitally issued and cryptographically encrypted by Team 11 Insurance Automation Engine. No physical signature is required under IT Act 2000.",
                    footerFont
            );
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingBefore(20);
            document.add(footer);

            document.close();
            return baos.toByteArray();
        } catch (DocumentException e) {
            log.error("Failed to generate PDF for policy {}: {}", policy.getPolicyNo(), e.getMessage(), e);
            throw new RuntimeException("Error generating policy document PDF", e);
        } catch (Exception e) {
            log.error("Unexpected error generating PDF for policy {}: {}", policy.getPolicyNo(), e.getMessage(), e);
            throw new RuntimeException("Error generating policy document PDF", e);
        }
    }

    private void addTableRow(PdfPTable table, String label, String value, Font valueFont, boolean highlight) {
        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, new Color(71, 85, 105));

        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setPadding(6);
        labelCell.setBackgroundColor(highlight ? new Color(241, 245, 249) : new Color(248, 250, 252));
        labelCell.setBorderColor(new Color(226, 232, 240));

        PdfPCell valueCell = new PdfPCell(new Phrase(value != null ? value : "", valueFont));
        valueCell.setPadding(6);
        valueCell.setBackgroundColor(highlight ? new Color(241, 245, 249) : Color.WHITE);
        valueCell.setBorderColor(new Color(226, 232, 240));

        table.addCell(labelCell);
        table.addCell(valueCell);
    }
}
