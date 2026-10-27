package com.healthcare.app.pdf;

import com.healthcare.app.dto.AppointmentDto;
import com.healthcare.app.repository.SystemSettingRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PdfReceiptServiceTest {

    @Mock
    private SystemSettingRepository systemSettingRepository;

    @InjectMocks
    private PdfReceiptService pdfReceiptService;

    @Test
    @DisplayName("Should generate valid non-empty PDF document starting with %PDF- header")
    void shouldGenerateValidPdfBytes() {
        ReflectionTestUtils.setField(pdfReceiptService, "serverPort", "8080");
        when(systemSettingRepository.getValueOrDefault(anyString(), anyString())).thenAnswer(invocation -> invocation.getArgument(1));

        AppointmentDto app = new AppointmentDto();
        app.setAppointmentReference("DOC-20261028-TEST1");
        app.setPatientId(1L);
        app.setPatientName("Johnathan Miller");
        app.setPatientEmail("patient.john@healthcare.com");
        app.setPatientPhone("+1-800-555-0121");
        app.setDoctorId(2L);
        app.setDoctorName("Dr. Sarah Jenkins");
        app.setSpecializationName("Cardiology");
        app.setDoctorQualifications("MD, FACC");
        app.setAppointmentDate(LocalDate.now().plusDays(2));
        app.setStartTime(LocalTime.of(10, 0));
        app.setEndTime(LocalTime.of(10, 30));
        app.setAppointmentStatus("CONFIRMED");
        app.setConsultationFee(new BigDecimal("120.00"));
        app.setAppointmentType("IN_PERSON");
        app.setPaymentStatus("PAID");
        app.setPaymentMethod("CREDIT_CARD");
        app.setTransactionReference("TXN-TEST-99201");
        app.setCreatedAt(LocalDateTime.now());

        byte[] pdfBytes = pdfReceiptService.generateAppointmentReceipt(app);

        assertNotNull(pdfBytes, "PDF byte array must not be null");
        assertTrue(pdfBytes.length > 500, "PDF byte array must have reasonable length");

        // Verify standard PDF header magic bytes %PDF-
        String header = new String(pdfBytes, 0, 5);
        assertEquals("%PDF-", header, "Generated document must start with valid PDF magic bytes %PDF-");
    }
}
