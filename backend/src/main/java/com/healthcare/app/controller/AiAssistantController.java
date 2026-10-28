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

    public static class LabAnalysisRequest {
        private String testType;
        private String rawText;

        public LabAnalysisRequest() {}

        public String getTestType() { return testType; }
        public void setTestType(String testType) { this.testType = testType; }

        public String getRawText() { return rawText; }
        public void setRawText(String rawText) { this.rawText = rawText; }
    }

    @PostMapping("/analyze-lab")
    public ResponseEntity<ApiResponse<Map<String, Object>>> analyzeLabReport(@RequestBody(required = false) LabAnalysisRequest req) {
        String type = req != null ? req.getTestType() : "CBC";
        String text = req != null ? req.getRawText() : "";
        Map<String, Object> analysis = aiAssistantService.analyzeLabReport(type, text);
        return ResponseEntity.ok(ApiResponse.ok(analysis));
    }

    public static class DrugSafetyRequest {
        private java.util.List<String> medications;
        private String patientAllergies;

        public DrugSafetyRequest() {}

        public java.util.List<String> getMedications() { return medications; }
        public void setMedications(java.util.List<String> medications) { this.medications = medications; }

        public String getPatientAllergies() { return patientAllergies; }
        public void setPatientAllergies(String patientAllergies) { this.patientAllergies = patientAllergies; }
    }

    @PostMapping("/check-drug-safety")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkDrugSafety(@RequestBody(required = false) DrugSafetyRequest req) {
        java.util.List<String> meds = req != null ? req.getMedications() : java.util.Collections.emptyList();
        String allergies = req != null ? req.getPatientAllergies() : "";
        Map<String, Object> safety = aiAssistantService.checkDrugSafety(meds, allergies);
        return ResponseEntity.ok(ApiResponse.ok(safety));
    }

    @GetMapping("/hospital-stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getHospitalStats() {
        return ResponseEntity.ok(ApiResponse.ok(aiAssistantService.getHospitalLiveTelemetry()));
    }
}
