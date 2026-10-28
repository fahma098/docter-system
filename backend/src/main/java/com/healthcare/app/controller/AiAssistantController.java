package com.healthcare.app.controller;

import com.healthcare.app.dto.ApiResponse;
import com.healthcare.app.service.AiAssistantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiAssistantController {

    @Autowired
    private AiAssistantService aiAssistantService;

    public static class ChatRequest {
        private String message;

        public ChatRequest() {}

        public ChatRequest(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<Map<String, Object>>> chatWithAi(@RequestBody(required = false) ChatRequest req) {
        String msg = (req != null && req.getMessage() != null) ? req.getMessage() : "";
        Map<String, Object> result = aiAssistantService.processHealthQuery(msg);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    public static class PrescriptionRequest {
        private String condition;
        private String patientNotes;
        private String doctorImpression;
        private String doctorName;
        private String specialization;

        public PrescriptionRequest() {}

        public String getCondition() { return condition; }
        public void setCondition(String condition) { this.condition = condition; }

        public String getPatientNotes() { return patientNotes; }
        public void setPatientNotes(String patientNotes) { this.patientNotes = patientNotes; }

        public String getDoctorImpression() { return doctorImpression; }
        public void setDoctorImpression(String doctorImpression) { this.doctorImpression = doctorImpression; }

        public String getDoctorName() { return doctorName; }
        public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

        public String getSpecialization() { return specialization; }
        public void setSpecialization(String specialization) { this.specialization = specialization; }
    }

    @PostMapping("/generate-prescription")
    public ResponseEntity<ApiResponse<Map<String, Object>>> generatePrescription(@RequestBody(required = false) PrescriptionRequest req) {
        String cond = req != null ? req.getCondition() : "";
        String pNotes = req != null ? req.getPatientNotes() : "";
        String docImp = req != null ? req.getDoctorImpression() : "";
        String docName = req != null ? req.getDoctorName() : "Doctor";
        String spec = req != null ? req.getSpecialization() : "Consultant";

        Map<String, Object> rx = aiAssistantService.generateSmartPrescription(cond, pNotes, docImp, docName, spec);
        return ResponseEntity.ok(ApiResponse.ok(rx));
    }
}
