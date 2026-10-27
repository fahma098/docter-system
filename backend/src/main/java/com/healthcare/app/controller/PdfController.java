package com.healthcare.app.controller;

import com.healthcare.app.dto.AppointmentDto;
import com.healthcare.app.exception.UnauthorizedException;
import com.healthcare.app.pdf.PdfReceiptService;
import com.healthcare.app.security.RequiresRole;
import com.healthcare.app.security.SecurityContextHelper;
import com.healthcare.app.security.SessionTokenManager;
import com.healthcare.app.service.AppointmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/appointments")
public class PdfController {

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private PdfReceiptService pdfReceiptService;

    @GetMapping("/{id}/receipt")
    @RequiresRole({"PATIENT", "DOCTOR", "ADMIN"})
    public ResponseEntity<byte[]> downloadReceipt(@PathVariable Long id) {
        SessionTokenManager.UserSession session = SecurityContextHelper.getCurrentSession();
        AppointmentDto appointment = appointmentService.getAppointmentDtoById(id);

        verifyPdfAccess(session, appointment);

        byte[] pdfBytes = pdfReceiptService.generateAppointmentReceipt(appointment);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "receipt-" + appointment.getAppointmentReference() + ".pdf");
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }

    @GetMapping("/{id}/receipt/preview")
    @RequiresRole({"PATIENT", "DOCTOR", "ADMIN"})
    public ResponseEntity<byte[]> previewReceipt(@PathVariable Long id) {
        SessionTokenManager.UserSession session = SecurityContextHelper.getCurrentSession();
        AppointmentDto appointment = appointmentService.getAppointmentDtoById(id);

        verifyPdfAccess(session, appointment);

        byte[] pdfBytes = pdfReceiptService.generateAppointmentReceipt(appointment);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.add("Content-Disposition", "inline; filename=\"receipt-" + appointment.getAppointmentReference() + ".pdf\"");

        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }

    private void verifyPdfAccess(SessionTokenManager.UserSession session, AppointmentDto appointment) {
        if ("PATIENT".equals(session.getRole()) && !session.getPatientId().equals(appointment.getPatientId())) {
            throw new UnauthorizedException("You are not authorized to download this receipt");
        }
        if ("DOCTOR".equals(session.getRole()) && !session.getDoctorId().equals(appointment.getDoctorId())) {
            throw new UnauthorizedException("You are not authorized to download this receipt");
        }
    }
}
