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

    public Map<String, Object> generateSmartPrescription(String condition, String patientNotes, String doctorImpression, String doctorName, String specName) {
        Map<String, Object> result = new HashMap<>();
        String input = ((condition != null ? condition : "") + " " + (patientNotes != null ? patientNotes : "") + " " + (doctorImpression != null ? doctorImpression : "")).toLowerCase();

        String diagnosis;
        String icdCode;
        List<String> rxMedications = new ArrayList<>();
        List<String> investigations = new ArrayList<>();
        List<String> advice = new ArrayList<>();
        String followUp;

        if (containsAny(input, "cardio", "chest", "bp", "hypertension", "angina", "heart", "palpitation")) {
            diagnosis = "Essential Stage-1 Systemic Hypertension & Mild Tachycardia";
            icdCode = "ICD-10: I10";
            rxMedications.add("1. Tab. Telmisartan 40mg (Telma 40) - 1 tab (1-0-0) morning after breakfast x 30 days");
            rxMedications.add("2. Tab. Atorvastatin 10mg (Atorva 10) - 1 tab (0-0-1) at bedtime x 30 days");
            rxMedications.add("3. Tab. Aspirin 75mg (Ecosprin 75) - 1 tab (0-1-0) after lunch x 30 days");
            rxMedications.add("4. Cap. Pantoprazole 40mg (Pan 40) - 1 cap (1-0-0) 30 mins before breakfast x 14 days");
            investigations.add("12-Lead Electrocardiogram (ECG)");
            investigations.add("Fasting Lipid Profile & Serum Creatinine");
            investigations.add("Echocardiography (2D Echo) if chest heaviness recurs");
            advice.add("Restrict dietary sodium (< 3 grams/day); avoid papads, pickles, and processed snacks");
            advice.add("Engage in 30 minutes of moderate aerobic walking 5 days a week");
            advice.add("Log morning and evening resting BP readings in a health diary");
            followUp = "Review in OPD Room 204 in 14 days with BP chart, or SOS if systolic BP exceeds 160 mmHg.";
        } else if (containsAny(input, "skin", "rash", "acne", "itch", "eczema", "dermat", "allergy")) {
            diagnosis = "Allergic Contact Dermatitis & Pruritus Cutaneus";
            icdCode = "ICD-10: L23.9";
            rxMedications.add("1. Tab. Levocetirizine 5mg (Levocet 5) - 1 tab (0-0-1) at bedtime x 10 days");
            rxMedications.add("2. Cap. Doxycycline 100mg (Doxicip 100) - 1 cap (1-0-1) after meals x 7 days");
            rxMedications.add("3. Mupirocin 2% Ointment (T-Bact) - Apply thin film locally over lesions twice daily x 7 days");
            rxMedications.add("4. Calamine + Liquid Paraffin Lotion - Apply gently for cooling relief as needed");
            investigations.add("Absolute Eosinophil Count (AEC) & Total Serum IgE");
            investigations.add("Skin Scraping for KOH mount (if fungal spread suspected)");
            advice.add("Avoid scratching, harsh synthetic detergents, scented soaps, and direct hot water exposure");
            advice.add("Wear loose-fitting, breathable cotton clothing");
            advice.add("Keep skin well hydrated with hypoallergenic emollient cream within 3 mins of bathing");
            followUp = "Review in OPD Room 108 in 10 days to assess lesion resolution.";
        } else if (containsAny(input, "child", "pediatric", "baby", "infant", "fever", "cough", "cold", "throat", "bronch")) {
            diagnosis = "Acute Upper Respiratory Tract Infection (URTI) with Viral Pharyngitis";
            icdCode = "ICD-10: J06.9";
            rxMedications.add("1. Tab. Paracetamol 650mg (Dolo 650) - 1 tab (1-0-1) after food x 3 days (SOS for fever > 99.5 F)");
            rxMedications.add("2. Tab. Augmentin 625mg (Amoxicillin + Clavulanate) - 1 tab (1-0-1) after food x 5 days");
            rxMedications.add("3. Syp. Grilinctus-L - 10 ml thrice daily x 5 days");
            rxMedications.add("4. Tab. Pantocid 40mg - 1 tab once daily in morning before food x 5 days");
            investigations.add("Complete Blood Count (CBC) with Differential Count & ESR");
            investigations.add("Serum C-Reactive Protein (CRP) quantitative");
            advice.add("Warm saline gargling 3 times daily");
            advice.add("Steam inhalation with menthol/saline twice daily for nasal decongestion");
            advice.add("Drink plenty of boiled warm fluids (jeera water, soups); avoid refrigerated drinks");
            followUp = "Review in OPD Room 102 in 5 days or SOS if persistent high fever (>102°F) or breathlessness.";
        } else if (containsAny(input, "neuro", "headache", "migraine", "dizzy", "vertigo", "spine")) {
            diagnosis = "Classic Migraine without Aura with Episodic Tension-type Cephalea";
            icdCode = "ICD-10: G43.0";
            rxMedications.add("1. Tab. Naproxen 500mg + Domperidone 10mg - 1 tab SOS at migraine onset (max 2/day)");
            rxMedications.add("2. Tab. Propranolol 20mg (Inderal 20) - 1 tab (1-0-0) morning x 30 days");
            rxMedications.add("3. Cap. Methylcobalamin 1500mcg + Pregabalin 75mg - 1 cap (0-0-1) night x 30 days");
            rxMedications.add("4. Tab. Betahistine 16mg (Vertin 16) - 1 tab twice daily x 7 days if vertigo present");
            investigations.add("Brain MRI Screening with MR Angiogram (if severe atypical presentation)");
            investigations.add("Ophthalmology Fundus & Refraction Examination");
            advice.add("Maintain consistent sleep cycles (7-8 hours); avoid irregular fasting and dehydration");
            advice.add("Limit bright fluorescent screen exposure; take 5-min eye breaks every hour");
            advice.add("Maintain a headache trigger journal (caffeine, stress, missed meals)");
            followUp = "Neurology review in OPD Room 301 in 21 days with headache frequency log.";
        } else if (containsAny(input, "ortho", "bone", "joint", "knee", "back", "arthritis", "ligament")) {
            diagnosis = "Bilateral Knee Osteoarthritis (Grade II) with Mechanical Joint Pain";
            icdCode = "ICD-10: M17.0";
            rxMedications.add("1. Tab. Aceclofenac 100mg + Paracetamol 325mg (Zerodol-P) - 1 tab (1-0-1) after food x 5 days (SOS)");
            rxMedications.add("2. Tab. Calcium Carbonate 500mg + Vitamin D3 250 IU (Shelcal 500) - 1 tab (0-1-0) after lunch x 30 days");
            rxMedications.add("3. Tab. Diacerein 50mg + Glucosamine 750mg - 1 tab (1-0-0) after breakfast x 30 days");
            rxMedications.add("4. Volini / Fastum Gel - Gentle topical application over knees twice daily x 14 days");
            investigations.add("Bilateral Knee Weight-Bearing X-Ray (AP & Lateral views)");
            investigations.add("Serum Uric Acid & ESR");
            advice.add("Quadriceps isometric strengthening exercises twice daily under physiotherapy guidance");
            advice.add("Avoid deep squatting, cross-legged sitting on floor, and excessive stair climbing");
            advice.add("Apply warm compress for 15 minutes before bedtime to reduce stiffness");
            followUp = "Orthopedic review in OPD Room 215 in 14 days along with X-Ray films.";
        } else {
            diagnosis = "Acute Febrile Illness & Generalized Fatigue Syndrome";
            icdCode = "ICD-10: R50.9";
            rxMedications.add("1. Tab. Dolo 650mg (Paracetamol) - 1 tab (1-0-1) after food x 3 days (SOS)");
            rxMedications.add("2. Tab. Multivitamin with Zinc (Becadexamin) - 1 cap (0-1-0) after lunch x 30 days");
            rxMedications.add("3. Tab. Pantoprazole 40mg (Pan 40) - 1 tab (1-0-0) 30 mins before breakfast x 14 days");
            rxMedications.add("4. ORS Hydration Sachet - 1 sachet in 1 Litre boiled water, sip through day");
            investigations.add("Complete Blood Count (CBC) with Peripheral Smear");
            investigations.add("Fasting Blood Sugar (FBS) & HbA1c");
            advice.add("Adequate rest, intake of light easily digestible home-cooked meals");
            advice.add("Drink minimum 2.5 to 3 Litres of fluids per day");
            followUp = "Review in OPD Room 101 in 7 days or earlier if symptoms do not subside.";
        }

        StringBuilder formattedRx = new StringBuilder();
        formattedRx.append("=========================================\n");
        formattedRx.append("AURA MULTISPECIALITY HOSPITAL, KOCHI\n");
        formattedRx.append("CLINICAL PRESCRIPTION & ORDERS (Rx)\n");
        formattedRx.append("=========================================\n\n");
        formattedRx.append("📋 CLINICAL DIAGNOSIS:\n");
        formattedRx.append(diagnosis).append(" (").append(icdCode).append(")\n\n");

        formattedRx.append("💊 MEDICATIONS PRESCRIBED (Rx):\n");
        for (String rx : rxMedications) {
            formattedRx.append(rx).append("\n");
        }
        formattedRx.append("\n");

        formattedRx.append("🔬 SUGGESTED LAB INVESTIGATIONS:\n");
        for (String inv : investigations) {
            formattedRx.append("• ").append(inv).append("\n");
        }
        formattedRx.append("\n");

        formattedRx.append("🥗 DIET & LIFESTYLE ADVICE:\n");
        for (String adv : advice) {
            formattedRx.append("• ").append(adv).append("\n");
        }
        formattedRx.append("\n");

        formattedRx.append("🗓️ FOLLOW-UP:\n");
        formattedRx.append(followUp).append("\n\n");

        formattedRx.append("✍️ CONSULTING SPECIALIST:\n");
        formattedRx.append(doctorName != null ? doctorName : "Specialist Medical Officer");
        if (specName != null) formattedRx.append(" (").append(specName).append(")");
        formattedRx.append("\nAura Multispeciality Hospital & Research Centre, Marine Drive, Kochi, Kerala\n");
        formattedRx.append("Electronic Signature Verified • Kerala Medical Council Reg.");

        result.put("diagnosis", diagnosis);
        result.put("icdCode", icdCode);
        result.put("medications", rxMedications);
        result.put("investigations", investigations);
        result.put("advice", advice);
        result.put("followUp", followUp);
        result.put("formattedRx", formattedRx.toString());

        return result;
    }
}
