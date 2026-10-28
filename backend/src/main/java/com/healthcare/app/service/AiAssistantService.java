package com.healthcare.app.service;

import com.healthcare.app.dto.DoctorDto;
import com.healthcare.app.entity.Doctor;
import com.healthcare.app.entity.Specialization;
import com.healthcare.app.repository.DoctorRepository;
import com.healthcare.app.repository.SpecializationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AiAssistantService {

    private static final Logger log = LoggerFactory.getLogger(AiAssistantService.class);

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private SpecializationRepository specializationRepository;

    @Autowired
    private DoctorService doctorService;

    public Map<String, Object> processHealthQuery(String userQuery) {
        Map<String, Object> response = new HashMap<>();
        if (userQuery == null || userQuery.trim().isEmpty()) {
            response.put("reply", "Namaskaram! 🙏 Please describe your symptoms or medical concern (e.g., knee pain, skin rash, chest discomfort, child fever), and I will guide you to the right specialist at Aura Hospital Kochi.");
            response.put("recommendedDoctors", Collections.emptyList());
            return response;
        }

        String q = userQuery.toLowerCase().trim();

        // 1. Check for Emergency Red Flags
        if (q.contains("chest pain") && (q.contains("severe") || q.contains("left arm") || q.contains("sweat") || q.contains("breathless"))
                || q.contains("heart attack") || q.contains("paralysis") || q.contains("unconscious") || q.contains("heavy bleeding")) {
            response.put("reply", "🚨 **EMERGENCY WARNING**: Based on your symptoms, this could indicate a life-threatening medical emergency. Please **do not wait for an online appointment**. Call the Aura Kerala Emergency Casualty Helpline immediately at **+91 484 288 9108** or dial **108** for an ambulance to the nearest hospital casualty.");
            response.put("isEmergency", true);
            response.put("specializationId", 1L);
            response.put("specializationName", "Emergency / Cardiology");
            response.put("recommendedDoctors", getDoctorsBySpec(1L));
            return response;
        }

        // 2. Department Triage Keyword Matching (English + Malayalam transliteration)
        Long matchedSpecId = null;
        String triageReason = "";
        String careTips = "";

        // Cardiology (Spec 1)
        if (containsAny(q, "chest", "heart", "cardio", "bp", "blood pressure", "palpitation", "cholesterol", "ecg", "angina", "hypertension", "nenjil", "hridayam", "nenju vedhana")) {
            matchedSpecId = 1L;
            triageReason = "Your symptoms relate to cardiovascular health, blood pressure, or chest sensation.";
            careTips = "Avoid strenuous physical activity, rest in a comfortable seated position, avoid heavy salty meals, and monitor your blood pressure reading if an accurate cuff is available.";
        }
        // Dermatology (Spec 2)
        else if (containsAny(q, "skin", "rash", "acne", "pimple", "itch", "eczema", "allergy", "psoriasis", "hair", "dandruff", "fungal", "mole", "chirangu", "tholi", "choori", "dermat")) {
            matchedSpecId = 2L;
            triageReason = "Your symptoms indicate a dermatological skin, hair, or allergic concern.";
            careTips = "Keep the affected area clean and dry. Avoid scratching or applying unprescribed steroid creams or harsh scented soaps until evaluated by the doctor.";
        }
        // Pediatrics (Spec 3)
        else if (containsAny(q, "child", "baby", "infant", "kid", "toddler", "pediatric", "vaccin", "newborn", "teething", "kutti", "makan", "makal", "kunjinte", "paediatric")) {
            matchedSpecId = 3L;
            triageReason = "Your query pertains to pediatric care, child development, or infant health.";
            careTips = "Ensure the child stays well hydrated with oral fluids or breastmilk. Monitor body temperature with a digital thermometer and keep a vaccination record handy.";
        }
        // Neurology (Spec 4)
        else if (containsAny(q, "headache", "migraine", "dizzy", "vertigo", "seizure", "epilepsy", "stroke", "numb", "tingling", "nerve", "memory", "tremor", "thala", "thalavedhana", "karakkam", "neuro")) {
            matchedSpecId = 4L;
            triageReason = "Your symptoms point toward neurological or cranial nervous system health.";
            careTips = "Rest in a quiet, dark room, stay well hydrated, avoid excessive screen light, and note when the headache or numbness peaks to share with the neurologist.";
        }
        // Orthopedics (Spec 5)
        else if (containsAny(q, "bone", "joint", "knee", "back", "spine", "shoulder", "neck", "fracture", "arthritis", "ligament", "sprain", "ortho", "kaalu", "muttu", "kaal", "hip", "walking pain")) {
            matchedSpecId = 5L;
            triageReason = "Your symptoms are musculoskeletal, involving bones, joints, ligaments, or spine alignment.";
            careTips = "Practice the R.I.C.E protocol (Rest, Ice pack application for 15 mins, gentle Compression, and Elevation). Avoid weight-bearing strain or sudden twisting movements.";
        }
        // General Medicine (Spec 6)
        else if (containsAny(q, "fever", "cough", "cold", "flu", "viral", "throat", "fatigue", "tired", "weakness", "diabetes", "sugar", "infection", "stomach", "vomit", "motion", "pani", "chuma", "vayaru", "general")) {
            matchedSpecId = 6L;
            triageReason = "Your symptoms suggest an acute infection, metabolic condition, or primary medical concern.";
            careTips = "Drink plenty of boiled warm water, get adequate bed rest, eat easily digestible light meals, and track your temperature if running a fever.";
        }

        // 3. Formulate Clinical Recommendation
        if (matchedSpecId != null) {
            Optional<Specialization> specOpt = specializationRepository.findById(matchedSpecId);
            String specName = specOpt.map(Specialization::getSpecializationName).orElse("Specialist Doctor");

            List<DoctorDto> doctors = getDoctorsBySpec(matchedSpecId);
            String doctorName = !doctors.isEmpty() ? doctors.get(0).getFullName() : "our specialist";

            StringBuilder reply = new StringBuilder();
            reply.append("🏥 **Medical Triage Assessment**\n\n");
            reply.append(triageReason).append("\n\n");
            reply.append("👨‍⚕️ **Recommended Department:** **").append(specName).append("**\n");
            reply.append("We recommend booking a consultation with **").append(doctorName).append("** at our Kochi hospital.\n\n");
            reply.append("💡 **Initial Home Care Tip:**\n").append(careTips).append("\n\n");
            reply.append("👇 You can directly book an appointment with our recommended doctor below:");

            response.put("reply", reply.toString());
            response.put("specializationId", matchedSpecId);
            response.put("specializationName", specName);
            response.put("recommendedDoctors", doctors);
        } else {
            // General guidance
            List<DoctorDto> generalDocs = getDoctorsBySpec(6L); // General Medicine default
            response.put("reply", "Namaskaram! 🙏 Thank you for reaching out to Aura Multispeciality Hospital, Kochi. Based on your description, we recommend starting with a **General Medicine** consultation with **Dr. Suresh Kumar, MD** for a comprehensive evaluation, vitals screening, and clinical examination.\n\nAlternatively, you may browse our departments or describe your symptoms in more detail (e.g. location of pain, how many days, or related symptoms).");
            response.put("specializationId", 6L);
            response.put("specializationName", "General Medicine");
            response.put("recommendedDoctors", generalDocs);
        }

        return response;
    }

    private List<DoctorDto> getDoctorsBySpec(Long specId) {
        try {
            return doctorService.searchDoctors(specId, null, true);
        } catch (Exception e) {
            log.error("Failed to query doctors for spec {}: {}", specId, e.getMessage());
            return Collections.emptyList();
        }
    }

    private boolean containsAny(String text, String... keywords) {
        for (String k : keywords) {
            if (text.contains(k)) return true;
        }
        return false;
    }
}
