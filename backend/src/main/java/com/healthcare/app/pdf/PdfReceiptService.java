package com.healthcare.app.pdf;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.healthcare.app.dto.AppointmentDto;
import com.healthcare.app.repository.SystemSettingRepository;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class PdfReceiptService {

    private static final Logger log = LoggerFactory.getLogger(PdfReceiptService.class);

    @Autowired
    private SystemSettingRepository systemSettingRepository;

    @Value("${server.port:8080}")
    private String serverPort;

    // Palette Constants
    private static final Color PRIMARY_BLUE = new Color(0x25, 0x63, 0xEB);
    private static final Color SECONDARY_BLUE = new Color(0x3B, 0x82, 0xF6);
    private static final Color HEALTHCARE_TEAL = new Color(0x0F, 0x76, 0x6E);
    private static final Color LIGHT_BG = new Color(0xF5, 0xF8, 0xFC);
    private static final Color LIGHT_TEAL = new Color(0xF0, 0xFD, 0xFA);
    private static final Color LIGHT_BLUE = new Color(0xEF, 0xF6, 0xFF);
    private static final Color TEXT_MAIN = new Color(0x17, 0x20, 0x33);
    private static final Color TEXT_MUTED = new Color(0x64, 0x74, 0x8B);
    private static final Color BORDER_COLOR = new Color(0xE2, 0xE8, 0xF0);
    private static final Color SUCCESS_COLOR = new Color(0x15, 0x80, 0x3D);

    public byte[] generateAppointmentReceipt(AppointmentDto app) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);

        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            document.open();

            // Clinic metadata
            String clinicName = systemSettingRepository.getValueOrDefault("CLINIC_NAME", "Aura Multispeciality Hospital & Medical Research Centre, Kochi");
            String clinicAddr = systemSettingRepository.getValueOrDefault("CLINIC_ADDRESS", "Marine Drive, Ernakulam, Kochi, Kerala 682031, India");
            String clinicPhone = systemSettingRepository.getValueOrDefault("CLINIC_PHONE", "+91 484 288 9000");
            String clinicEmail = systemSettingRepository.getValueOrDefault("CLINIC_EMAIL", "appointments@aurakerala.health");

            // Fonts
            Font brandFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, PRIMARY_BLUE);
            Font docTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, HEALTHCARE_TEAL);
            Font sectionTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, PRIMARY_BLUE);
            Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, TEXT_MUTED);
            Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 9, TEXT_MAIN);
            Font boldValueFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, TEXT_MAIN);
            Font smallMutedFont = FontFactory.getFont(FontFactory.HELVETICA, 8, TEXT_MUTED);
            Font statusFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, "PAID".equalsIgnoreCase(app.getPaymentStatus()) ? SUCCESS_COLOR : PRIMARY_BLUE);

            // ==========================================
            // 1. TOP HEADER & QR CODE ROW
            // ==========================================
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[]{75, 25});
            headerTable.getDefaultCell().setBorder(Rectangle.NO_BORDER);

            PdfPCell leftHeader = new PdfPCell();
            leftHeader.setBorder(Rectangle.NO_BORDER);

            Paragraph pClinic = new Paragraph(clinicName, brandFont);
            Paragraph pSub = new Paragraph("ADVANCED HEALTHCARE APPOINTMENT RECEIPT & PASS", docTitleFont);
            Paragraph pContact = new Paragraph(clinicAddr + "\nPhone: " + clinicPhone + " | Email: " + clinicEmail, smallMutedFont);
            pContact.setSpacingBefore(3);

            leftHeader.addElement(pClinic);
            leftHeader.addElement(pSub);
            leftHeader.addElement(pContact);
            headerTable.addCell(leftHeader);

            // Generate QR Code
            PdfPCell rightHeader = new PdfPCell();
            rightHeader.setBorder(Rectangle.NO_BORDER);
            rightHeader.setHorizontalAlignment(Element.ALIGN_RIGHT);

            try {
                String verificationUrl = "http://localhost:" + serverPort + "/verify-receipt.html?ref=" + app.getAppointmentReference();
                QRCodeWriter qrCodeWriter = new QRCodeWriter();
                BitMatrix bitMatrix = qrCodeWriter.encode(verificationUrl, BarcodeFormat.QR_CODE, 100, 100);
                BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(bitMatrix);
                ByteArrayOutputStream qrOut = new ByteArrayOutputStream();
                ImageIO.write(qrImage, "png", qrOut);
                Image pdfQrImage = Image.getInstance(qrOut.toByteArray());
                pdfQrImage.scaleToFit(70, 70);
                pdfQrImage.setAlignment(Element.ALIGN_RIGHT);
                rightHeader.addElement(pdfQrImage);
            } catch (Exception e) {
                log.warn("Could not generate QR code for receipt: {}", e.getMessage());
            }

            headerTable.addCell(rightHeader);
            document.add(headerTable);

            document.add(new Paragraph(" "));

            // ==========================================
            // 2. REFERENCE & STATUS BANNER
            // ==========================================
            PdfPTable banner = new PdfPTable(3);
            banner.setWidthPercentage(100);
            banner.setWidths(new float[]{35, 35, 30});

            PdfPCell c1 = createStyledCell("APPOINTMENT REFERENCE", app.getAppointmentReference(), labelFont, boldValueFont, LIGHT_BG);
            PdfPCell c2 = createStyledCell("APPOINTMENT STATUS", app.getAppointmentStatus(), labelFont, statusFont, LIGHT_BG);
            PdfPCell c3 = createStyledCell("BOOKING DATE", app.getCreatedAt() != null ? app.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")) : "N/A", labelFont, valueFont, LIGHT_BG);

            banner.addCell(c1);
            banner.addCell(c2);
            banner.addCell(c3);
            document.add(banner);

            document.add(new Paragraph(" "));

            // ==========================================
            // 3. PATIENT & DOCTOR TWO-COLUMN CARD
            // ==========================================
            PdfPTable infoGrid = new PdfPTable(2);
            infoGrid.setWidthPercentage(100);
            infoGrid.setWidths(new float[]{50, 50});

            // Patient Card Cell
            PdfPCell patientCell = new PdfPCell();
            patientCell.setPadding(10);
            patientCell.setBorderColor(BORDER_COLOR);
            patientCell.setBackgroundColor(Color.WHITE);

            Paragraph pHeading = new Paragraph("PATIENT DETAILS", sectionTitleFont);
            patientCell.addElement(pHeading);
            patientCell.addElement(new Paragraph("Name: " + app.getPatientName(), boldValueFont));
            patientCell.addElement(new Paragraph("Phone: " + (app.getPatientPhone() != null ? app.getPatientPhone() : "N/A"), valueFont));
            patientCell.addElement(new Paragraph("Email: " + (app.getPatientEmail() != null ? app.getPatientEmail() : "N/A"), valueFont));
            patientCell.addElement(new Paragraph("Patient ID: #" + app.getPatientId(), smallMutedFont));
            infoGrid.addCell(patientCell);

            // Doctor Card Cell
            PdfPCell doctorCell = new PdfPCell();
            doctorCell.setPadding(10);
            doctorCell.setBorderColor(BORDER_COLOR);
            doctorCell.setBackgroundColor(Color.WHITE);

            Paragraph dHeading = new Paragraph("DOCTOR DETAILS", sectionTitleFont);
            doctorCell.addElement(dHeading);
            doctorCell.addElement(new Paragraph("Doctor: " + app.getDoctorName(), boldValueFont));
            doctorCell.addElement(new Paragraph("Specialization: " + app.getSpecializationName(), valueFont));
            doctorCell.addElement(new Paragraph("Qualifications: " + (app.getDoctorQualifications() != null ? app.getDoctorQualifications() : "Certified Specialist"), valueFont));
            doctorCell.addElement(new Paragraph("Phone: " + (app.getDoctorPhone() != null ? app.getDoctorPhone() : "Clinic Desk"), smallMutedFont));
            infoGrid.addCell(doctorCell);

            document.add(infoGrid);
            document.add(new Paragraph(" "));

            // ==========================================
            // 4. APPOINTMENT SCHEDULE & TIMING
            // ==========================================
            PdfPTable schedTable = new PdfPTable(4);
            schedTable.setWidthPercentage(100);
            schedTable.setWidths(new float[]{25, 25, 25, 25});

            DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("EEEE, MMM dd, yyyy");
            String dateFormatted = app.getAppointmentDate() != null ? app.getAppointmentDate().format(dateFmt) : "";
            String timeFormatted = (app.getStartTime() != null ? app.getStartTime().toString() : "") + " - " + (app.getEndTime() != null ? app.getEndTime().toString() : "");

            schedTable.addCell(createStyledCell("CONSULTATION DATE", dateFormatted, labelFont, boldValueFont, LIGHT_BLUE));
            schedTable.addCell(createStyledCell("TIME SLOT", timeFormatted, labelFont, boldValueFont, LIGHT_BLUE));
            schedTable.addCell(createStyledCell("EST. DURATION", "30 Minutes", labelFont, valueFont, LIGHT_BLUE));
            schedTable.addCell(createStyledCell("CONSULTATION TYPE", app.getAppointmentType(), labelFont, valueFont, LIGHT_BLUE));
            document.add(schedTable);

            document.add(new Paragraph(" "));

            // ==========================================
            // 5. BILLING & PAYMENT BREAKDOWN
            // ==========================================
            PdfPTable billTable = new PdfPTable(2);
            billTable.setWidthPercentage(100);
            billTable.setWidths(new float[]{70, 30});

            PdfPCell billHeader1 = new PdfPCell(new Paragraph("FEE DESCRIPTION", sectionTitleFont));
            billHeader1.setBackgroundColor(LIGHT_BG);
            billHeader1.setPadding(6);
            billHeader1.setBorderColor(BORDER_COLOR);

            PdfPCell billHeader2 = new PdfPCell(new Paragraph("AMOUNT (INR)", sectionTitleFont));
            billHeader2.setBackgroundColor(LIGHT_BG);
            billHeader2.setHorizontalAlignment(Element.ALIGN_RIGHT);
            billHeader2.setPadding(6);
            billHeader2.setBorderColor(BORDER_COLOR);

            billTable.addCell(billHeader1);
            billTable.addCell(billHeader2);

            BigDecimal fee = app.getConsultationFee() != null ? app.getConsultationFee() : BigDecimal.ZERO;
            addBillRow(billTable, "Doctor Consultation Fee (" + app.getSpecializationName() + ")", "Rs. " + fee.setScale(2), valueFont, valueFont);
            addBillRow(billTable, "Clinic Facility & Electronic Health Records Charge", "Rs. 0.00 (Waived)", valueFont, valueFont);
            addBillRow(billTable, "Applicable Healthcare Taxes (Kerala GST Exempt)", "Rs. 0.00", valueFont, valueFont);

            // Total row
            PdfPCell totDesc = new PdfPCell(new Paragraph("TOTAL AMOUNT PAYABLE", boldValueFont));
            totDesc.setBackgroundColor(LIGHT_TEAL);
            totDesc.setPadding(8);
            totDesc.setBorderColor(BORDER_COLOR);

            PdfPCell totVal = new PdfPCell(new Paragraph("Rs. " + fee.setScale(2), boldValueFont));
            totVal.setBackgroundColor(LIGHT_TEAL);
            totVal.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totVal.setPadding(8);
            totVal.setBorderColor(BORDER_COLOR);

            billTable.addCell(totDesc);
            billTable.addCell(totVal);

            // Payment status note
            boolean isPaid = "PAID".equalsIgnoreCase(app.getPaymentStatus());
            String paymentNote = isPaid
                    ? "PAYMENT CONFIRMED: Paid via " + (app.getPaymentMethod() != null ? app.getPaymentMethod() : "Card") + " (Ref: " + (app.getTransactionReference() != null ? app.getTransactionReference() : "TXN-DIRECT") + ")"
                    : "PAYMENT STATUS: PENDING (Payable upon check-in at clinic front desk)";

            PdfPCell payNoteCell = new PdfPCell(new Paragraph(paymentNote, statusFont));
            payNoteCell.setColspan(2);
            payNoteCell.setPadding(8);
            payNoteCell.setBackgroundColor(isPaid ? LIGHT_TEAL : LIGHT_BG);
            payNoteCell.setBorderColor(BORDER_COLOR);
            billTable.addCell(payNoteCell);

            document.add(billTable);
            document.add(new Paragraph(" "));

            // ==========================================
            // 6. INSTRUCTIONS & CLINIC POLICY FOOTER
            // ==========================================
            PdfPTable footerTable = new PdfPTable(1);
            footerTable.setWidthPercentage(100);

            PdfPCell fCell = new PdfPCell();
            fCell.setPadding(10);
            fCell.setBorderColor(BORDER_COLOR);
            fCell.setBackgroundColor(LIGHT_BG);

            Paragraph pNotice = new Paragraph("CLINIC INSTRUCTIONS & PATIENT POLICY", sectionTitleFont);
            Paragraph pRules = new Paragraph(
                    "1. Please arrive at the clinic reception 10 minutes prior to your scheduled time slot.\n" +
                    "2. Present this document on your mobile device or physical printout with the QR verification code.\n" +
                    "3. Rescheduling or cancellation is permitted up to 4 hours prior to your consultation.\n" +
                    "4. For emergency care, please call 911 or visit your nearest emergency room immediately.",
                    smallMutedFont
            );
            pRules.setSpacingBefore(4);

            String genTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            Paragraph pTimestamp = new Paragraph("Document Generated: " + genTime + " | Official Electronic Receipt | Aura Health Cloud", smallMutedFont);
            pTimestamp.setSpacingBefore(8);

            fCell.addElement(pNotice);
            fCell.addElement(pRules);
            fCell.addElement(pTimestamp);
            footerTable.addCell(fCell);

            document.add(footerTable);

            document.close();
        } catch (Exception e) {
            log.error("Failed to generate PDF appointment receipt: ", e);
            throw new RuntimeException("Error rendering PDF receipt: " + e.getMessage(), e);
        }

        return out.toByteArray();
    }

    private PdfPCell createStyledCell(String title, String val, Font titleFont, Font valFont, Color bg) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(8);
        cell.setBackgroundColor(bg);
        cell.setBorderColor(BORDER_COLOR);
        cell.addElement(new Paragraph(title, titleFont));
        Paragraph valP = new Paragraph(val != null ? val : "N/A", valFont);
        valP.setSpacingBefore(2);
        cell.addElement(valP);
        return cell;
    }

    private void addBillRow(PdfPTable table, String desc, String amt, Font descFont, Font amtFont) {
        PdfPCell c1 = new PdfPCell(new Paragraph(desc, descFont));
        c1.setPadding(6);
        c1.setBorderColor(BORDER_COLOR);

        PdfPCell c2 = new PdfPCell(new Paragraph(amt, amtFont));
        c2.setHorizontalAlignment(Element.ALIGN_RIGHT);
        c2.setPadding(6);
        c2.setBorderColor(BORDER_COLOR);

        table.addCell(c1);
        table.addCell(c2);
    }
}
