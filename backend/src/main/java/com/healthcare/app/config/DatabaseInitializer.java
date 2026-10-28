package com.healthcare.app.config;

import com.healthcare.app.entity.*;
import com.healthcare.app.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

@Component
public class DatabaseInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseInitializer.class);

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SpecializationRepository specializationRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private SystemSettingRepository systemSettingRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Initializing Kerala Healthcare System security, clinic settings, and baseline accounts...");

        // Auto-apply PostgreSQL schema if running against PostgreSQL
        try (Connection conn = dataSource.getConnection()) {
            String dbProduct = conn.getMetaData().getDatabaseProductName().toLowerCase();
            if (dbProduct.contains("postgres")) {
                log.info("Detected PostgreSQL database. Ensuring PostgreSQL schema is applied...");
                ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
                populator.setContinueOnError(true);
                populator.addScript(new ClassPathResource("schema_postgres.sql"));
                populator.execute(dataSource);
                log.info("PostgreSQL schema verified and ready.");
            }
        } catch (Exception e) {
            log.warn("PostgreSQL schema auto-detection check: {}", e.getMessage());
        }

        String defaultPasswordHash = passwordEncoder.encode("Password123!");

        // Ensure Kerala Hospital & INR System Settings
        try {
            systemSettingRepository.setSetting("CLINIC_NAME", "Aura Multispeciality Hospital & Medical Research Centre, Kochi", "Official clinic branding name");
            systemSettingRepository.setSetting("CLINIC_ADDRESS", "Marine Drive, Ernakulam, Kochi, Kerala 682031, India", "Clinic physical address");
            systemSettingRepository.setSetting("CLINIC_PHONE", "+91 484 288 9000", "Main patient helpline and booking desk");
            systemSettingRepository.setSetting("CLINIC_EMAIL", "appointments@aurakerala.health", "Official healthcare email");
            systemSettingRepository.setSetting("CURRENCY_SYMBOL", "₹", "Currency display symbol");
            systemSettingRepository.setSetting("CURRENCY_CODE", "INR", "Official billing currency");
            systemSettingRepository.setSetting("MIN_ADVANCE_BOOKING_HOURS", "0", "Minimum advance booking notice in hours");
        } catch (Exception e) {
            log.warn("Could not update clinic settings: {}", e.getMessage());
        }

        // 1. Ensure Specializations
        ensureSpecialization(1L, "Cardiology", "Heart health, hypertension, coronary care, and cardiovascular diagnostic management.", "heart-pulse");
        ensureSpecialization(2L, "Dermatology", "Skin disorders, clinical acne therapy, allergy management, and eczema care.", "sparkles");
        ensureSpecialization(3L, "Pediatrics", "Complete medical care, developmental assessment, and immunizations for infants & children.", "baby");
        ensureSpecialization(4L, "Neurology", "Disorders of the nervous system, chronic migraines, stroke recovery, and nerve health.", "brain");
        ensureSpecialization(5L, "Orthopedics", "Bone, joint, spine, sports injury, arthroscopy, and musculoskeletal care.", "bone");
        ensureSpecialization(6L, "General Medicine", "Routine wellness physicals, diabetes care, viral fevers, and primary diagnosis.", "activity");

        // 2. Ensure Administrator
        Long adminUserId = null;
        Optional<User> adminOpt = userRepository.findByEmail("admin@healthcare.com");
        if (adminOpt.isEmpty()) {
            User admin = new User(null, "System Administrator (Kochi)", "admin@healthcare.com", "+91 484 288 9001", defaultPasswordHash, "ADMIN", "ACTIVE");
            adminUserId = userRepository.save(admin);
            log.info("Created default Administrator account: admin@healthcare.com / Password123!");
        } else {
            adminUserId = adminOpt.get().getUserId();
        }

        // 3. Ensure Kerala Seed Doctors (with verified portraits & INR fees)
        Long doc1Id = ensureDoctor(
                "Dr. Ananya Menon, MD, DM",
                "doctor.sarah@healthcare.com", // Kept email compatible with 1-click login
                "+91 98470 12001",
                defaultPasswordHash,
                1L, // Cardiology
                "KMC-CARD-88124",
                "MBBS (GMC Kochi), MD (GMC Trivandrum), DM Cardiology (Amrita)",
                14,
                new BigDecimal("850.00"),
                30,
                "Dr. Ananya Menon is a leading Senior Interventional Cardiologist in Kochi, specializing in cardiovascular evaluation, hypertension, non-invasive imaging, and preventive cardiology.",
                "https://images.unsplash.com/photo-1559839734-2b71ea197ec2?auto=format&fit=crop&q=80&w=400"
        );

        Long doc2Id = ensureDoctor(
                "Dr. Priya Nambiar, MD, DDVL",
                "doctor.marcus@healthcare.com",
                "+91 98470 12002",
                defaultPasswordHash,
                2L, // Dermatology
                "KMC-DERM-44109",
                "MBBS (Calicut Medical College), MD, DDVL Dermatology",
                10,
                new BigDecimal("650.00"),
                30,
                "Dr. Priya Nambiar provides advanced dermatological care, clinical acne therapy, eczema management, allergy patch testing, and laser skin restoration in Ernakulam.",
                "https://images.unsplash.com/photo-1594824813689-ff8288593be7?auto=format&fit=crop&q=80&w=400"
        );

        Long doc3Id = ensureDoctor(
                "Dr. Fathima Nabeel, MBBS, DNB",
                "doctor.emily@healthcare.com",
                "+91 98470 12003",
                defaultPasswordHash,
                3L, // Pediatrics
                "KMC-PED-66231",
                "MBBS, DCH, DNB Pediatrics (KMC Manipal)",
                12,
                new BigDecimal("600.00"),
                30,
                "Dr. Fathima Nabeel is dedicated to infant healthcare, routine developmental assessments, pediatric vaccinations, and childhood asthma care across central Kerala.",
                "https://images.unsplash.com/photo-1651008376811-b90baee60c1f?auto=format&fit=crop&q=80&w=400"
        );

        Long doc4Id = ensureDoctor(
                "Dr. Rajesh Varma, MD, DM",
                "doctor.james@healthcare.com",
                "+91 98470 12004",
                defaultPasswordHash,
                4L, // Neurology
                "KMC-NEUR-77312",
                "MBBS (Trivandrum), MD Medicine, DM Neurology (SCTIMST)",
                18,
                new BigDecimal("1100.00"),
                30,
                "Dr. Rajesh Varma is a distinguished Neurologist trained at Sree Chitra Tirunal Institute, specializing in chronic migraines, stroke rehabilitation, epilepsy, and neuropathy.",
                "https://images.unsplash.com/photo-1622253692010-333f2da6031d?auto=format&fit=crop&q=80&w=400"
        );

        Long doc5Id = ensureDoctor(
                "Dr. Mathew Joseph, MS, MCh",
                "doctor.robert@healthcare.com",
                "+91 98470 12005",
                defaultPasswordHash,
                5L, // Orthopedics
                "KMC-ORTH-55102",
                "MBBS (Kottayam), MS Orthopaedics, Fellowship in Joint Replacement",
                16,
                new BigDecimal("950.00"),
                30,
                "Dr. Mathew Joseph has performed over 2,000 successful knee and hip arthroplasties, sports injury ligament reconstructions, and spine alignments in central Kerala.",
                "https://images.unsplash.com/photo-1612349317150-e413f6a5b16d?auto=format&fit=crop&q=80&w=400"
        );

        Long doc6Id = ensureDoctor(
                "Dr. Suresh Kumar, MBBS, MD",
                "doctor.linda@healthcare.com",
                "+91 98470 12006",
                defaultPasswordHash,
                6L, // General Medicine
                "KMC-GM-33290",
                "MBBS, MD General Medicine (GMC Ernakulam)",
                15,
                new BigDecimal("500.00"),
                30,
                "Dr. Suresh Kumar provides comprehensive primary healthcare, diabetes management, fever diagnostics, lifestyle consultations, and regular wellness reviews in Kochi.",
                "https://images.unsplash.com/photo-1537368910025-700350fe46c7?auto=format&fit=crop&q=80&w=400"
        );

        // Also ensure Kerala Doctor accounts under direct Kerala emails as well
        ensureDoctor("Dr. Ananya Menon, MD, DM", "doctor.ananya@healthcare.com", "+91 98470 12001", defaultPasswordHash, 1L, "KMC-CARD-88124-A", "MBBS, MD, DM Cardiology", 14, new BigDecimal("850.00"), 30, "Dr. Ananya Menon specializes in clinical cardiology in Kochi.", "https://images.unsplash.com/photo-1559839734-2b71ea197ec2?auto=format&fit=crop&q=80&w=400");
        ensureDoctor("Dr. Priya Nambiar, MD, DDVL", "doctor.priya@healthcare.com", "+91 98470 12002", defaultPasswordHash, 2L, "KMC-DERM-44109-A", "MBBS, MD, DDVL Dermatology", 10, new BigDecimal("650.00"), 30, "Dr. Priya Nambiar provides skin and hair consultations in Ernakulam.", "https://images.unsplash.com/photo-1594824813689-ff8288593be7?auto=format&fit=crop&q=80&w=400");

        // 4. Ensure Kerala Patients
        Long pat1Id = ensurePatient(
                "Rahul Nair",
                "patient.john@healthcare.com", // Kept email compatible with 1-click login
                "+91 98470 12021",
                defaultPasswordHash,
                LocalDate.of(1988, 6, 14),
                "Male",
                "O+",
                "Panampilly Nagar, Ernakulam, Kochi, Kerala",
                "Lakshmi Nair (+91 98470 12031)"
        );

        Long pat2Id = ensurePatient(
                "Sneha Kurian",
                "patient.emma@healthcare.com",
                "+91 98470 12022",
                defaultPasswordHash,
                LocalDate.of(1994, 3, 22),
                "Female",
                "A+",
                "Kowdiar, Thiruvananthapuram, Kerala",
                "Thomas Kurian (+91 98470 12032)"
        );

        Long pat3Id = ensurePatient(
                "Mohammed Fayis",
                "patient.michael@healthcare.com",
                "+91 98470 12023",
                defaultPasswordHash,
                LocalDate.of(1982, 11, 5),
                "Male",
                "B+",
                "Beach Road, Kozhikode, Kerala",
                "Amina Fayis (+91 98470 12033)"
        );

        // Find user IDs for notifications and audit logging
        Long pat1UserId = userRepository.findByEmail("patient.john@healthcare.com").map(User::getUserId).orElse(null);
        Long pat2UserId = userRepository.findByEmail("patient.emma@healthcare.com").map(User::getUserId).orElse(null);
        Long pat3UserId = userRepository.findByEmail("patient.michael@healthcare.com").map(User::getUserId).orElse(null);
        Long doc1UserId = userRepository.findByEmail("doctor.sarah@healthcare.com").map(User::getUserId).orElse(null);

        // 5. Ensure Seed Appointments, Payments, Notifications & Audit Logs (INR Based)
        ensureSeedAppointments(doc1Id, doc2Id, doc3Id, doc4Id, doc5Id,
                pat1Id, pat2Id, pat3Id,
                pat1UserId, pat2UserId, pat3UserId,
                doc1UserId, adminUserId);

        log.info("Baseline security accounts, Kerala doctor profiles, and clinical records are ready.");
    }

    private void ensureSpecialization(Long id, String name, String desc, String icon) {
        if (specializationRepository.findById(id).isEmpty()) {
            Specialization s = new Specialization(id, name, desc, icon, true);
            specializationRepository.save(s);
        }
    }

    private Long ensureDoctor(String name, String email, String phone, String passwordHash, Long specId,
                              String license, String qual, int exp, BigDecimal fee, int duration, String bio, String imgUrl) {
        Optional<User> existing = userRepository.findByEmail(email);
        if (existing.isPresent()) {
            Long userId = existing.get().getUserId();
            // Upgrade existing record to Kerala profile & working image
            userRepository.updateContactInfo(userId, name, phone);
            Optional<Doctor> docOpt = doctorRepository.findByUserId(userId);
            if (docOpt.isPresent()) {
                Doctor d = docOpt.get();
                d.setQualifications(qual);
                d.setExperienceYears(exp);
                d.setConsultationFee(fee);
                d.setProfessionalBio(bio);
                d.setProfileImageUrl(imgUrl);
                doctorRepository.update(d);
                return d.getDoctorId();
            }
            return null;
        }

        User user = new User(null, name, email, phone, passwordHash, "DOCTOR", "ACTIVE");
        Long userId = userRepository.save(user);

        Doctor doc = new Doctor();
        doc.setUserId(userId);
        doc.setSpecializationId(specId);
        doc.setLicenseNumber(license);
        doc.setQualifications(qual);
        doc.setExperienceYears(exp);
        doc.setConsultationFee(fee);
        doc.setDefaultAppointmentDuration(duration);
        doc.setProfessionalBio(bio);
        doc.setProfileImageUrl(imgUrl);
        doc.setApprovalStatus("APPROVED");
        Long docId = doctorRepository.save(doc);

        // Create default Mon-Sat working schedule (09:00 - 17:00, lunch break 13:00 - 14:00)
        String[] days = {"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"};
        for (String day : days) {
            DoctorSchedule s = new DoctorSchedule();
            s.setDoctorId(docId);
            s.setWorkingDay(day);
            s.setStartTime(LocalTime.of(9, 0));
            s.setEndTime(LocalTime.of(17, 0));
            s.setBreakStartTime(LocalTime.of(13, 0));
            s.setBreakEndTime(LocalTime.of(14, 0));
            s.setSlotDurationMinutes(30);
            s.setActiveStatus(true);
            scheduleRepository.save(s);
        }
        return docId;
    }

    private Long ensurePatient(String name, String email, String phone, String passwordHash,
                               LocalDate dob, String gender, String bloodGroup, String address, String emergencyContact) {
        Optional<User> existing = userRepository.findByEmail(email);
        if (existing.isPresent()) {
            Long userId = existing.get().getUserId();
            userRepository.updateContactInfo(userId, name, phone);
            Optional<Patient> pOpt = patientRepository.findByUserId(userId);
            if (pOpt.isPresent()) {
                Patient p = pOpt.get();
                p.setAddress(address);
                p.setEmergencyContact(emergencyContact);
                patientRepository.update(p);
                return p.getPatientId();
            }
            return null;
        }

        User user = new User(null, name, email, phone, passwordHash, "PATIENT", "ACTIVE");
        Long userId = userRepository.save(user);

        Patient p = new Patient();
        p.setUserId(userId);
        p.setDateOfBirth(dob);
        p.setGender(gender);
        p.setBloodGroup(bloodGroup);
        p.setAddress(address);
        p.setEmergencyContact(emergencyContact);
        return patientRepository.save(p);
    }

    private void ensureSeedAppointments(Long doc1Id, Long doc2Id, Long doc3Id, Long doc4Id, Long doc5Id,
                                        Long pat1Id, Long pat2Id, Long pat3Id,
                                        Long pat1UserId, Long pat2UserId, Long pat3UserId,
                                        Long doc1UserId, Long adminUserId) {
        if (pat1Id == null || doc1Id == null) {
            return;
        }

        if (!appointmentRepository.findByPatientId(pat1Id).isEmpty()) {
            return; // Appointments already seeded
        }

        LocalDate today = LocalDate.now();

        // 1. Completed Appointment with Dr. Ananya Menon (Cardiology) - 3 days ago
        Appointment a1 = new Appointment();
        a1.setAppointmentReference("APT-2026-1001");
        a1.setPatientId(pat1Id);
        a1.setDoctorId(doc1Id);
        a1.setAppointmentDate(today.minusDays(3));
        a1.setStartTime(LocalTime.of(10, 0));
        a1.setEndTime(LocalTime.of(10, 30));
        a1.setAppointmentStatus("COMPLETED");
        a1.setConsultationFeeSnapshot(new BigDecimal("850.00"));
        a1.setAppointmentType("IN_PERSON");
        a1.setPatientNotes("Cardiovascular wellness evaluation and baseline ECG screening at Kochi Centre.");
        a1.setDoctorInstructions("Resting ECG is completely normal. Blood pressure measured 120/78 mmHg. Continue low-sodium diet and 30 minutes daily moderate cardio exercise. Routine annual follow-up scheduled.");
        Long a1Id = appointmentRepository.save(a1);

        Payment pay1 = new Payment();
        pay1.setAppointmentId(a1Id);
        pay1.setAmount(new BigDecimal("850.00"));
        pay1.setPaymentMethod("UPI");
        pay1.setPaymentStatus("PAID");
        pay1.setTransactionReference("UPI-AURA-882101");
        paymentRepository.save(pay1);

        // 2. Confirmed Upcoming Appointment with Dr. Ananya Menon (Cardiology) - Tomorrow at 10:00 AM
        Appointment a2 = new Appointment();
        a2.setAppointmentReference("APT-2026-1002");
        a2.setPatientId(pat1Id);
        a2.setDoctorId(doc1Id);
        a2.setAppointmentDate(today.plusDays(1));
        a2.setStartTime(LocalTime.of(10, 0));
        a2.setEndTime(LocalTime.of(10, 30));
        a2.setAppointmentStatus("CONFIRMED");
        a2.setConsultationFeeSnapshot(new BigDecimal("850.00"));
        a2.setAppointmentType("IN_PERSON");
        a2.setPatientNotes("Review recent lipid cholesterol profile report and follow-up.");
        Long a2Id = appointmentRepository.save(a2);

        Payment pay2 = new Payment();
        pay2.setAppointmentId(a2Id);
        pay2.setAmount(new BigDecimal("850.00"));
        pay2.setPaymentMethod("UPI");
        pay2.setPaymentStatus("PAID");
        pay2.setTransactionReference("UPI-AURA-882102");
        paymentRepository.save(pay2);

        // 3. Completed Video Consultation with Dr. Priya Nambiar (Dermatology) - 6 days ago
        if (doc2Id != null && pat2Id != null) {
            Appointment a3 = new Appointment();
            a3.setAppointmentReference("APT-2026-1003");
            a3.setPatientId(pat2Id);
            a3.setDoctorId(doc2Id);
            a3.setAppointmentDate(today.minusDays(6));
            a3.setStartTime(LocalTime.of(14, 0));
            a3.setEndTime(LocalTime.of(14, 30));
            a3.setAppointmentStatus("COMPLETED");
            a3.setConsultationFeeSnapshot(new BigDecimal("650.00"));
            a3.setAppointmentType("VIDEO_CONSULTATION");
            a3.setPatientNotes("Skin allergy rash after outdoor monsoon trek.");
            a3.setDoctorInstructions("Contact dermatitis confirmed. Prescribed mild topical soothing lotion twice daily for 7 days. Avoid chemical soaps.");
            Long a3Id = appointmentRepository.save(a3);

            Payment pay3 = new Payment();
            pay3.setAppointmentId(a3Id);
            pay3.setAmount(new BigDecimal("650.00"));
            pay3.setPaymentMethod("CREDIT_CARD");
            pay3.setPaymentStatus("PAID");
            pay3.setTransactionReference("TXN-AURA-882103");
            paymentRepository.save(pay3);
        }

        // 4. Confirmed Appointment Today for Sneha with Dr. Fathima Nabeel (Pediatrics)
        if (doc3Id != null && pat2Id != null) {
            Appointment a4 = new Appointment();
            a4.setAppointmentReference("APT-2026-1004");
            a4.setPatientId(pat2Id);
            a4.setDoctorId(doc3Id);
            a4.setAppointmentDate(today);
            a4.setStartTime(LocalTime.of(11, 0));
            a4.setEndTime(LocalTime.of(11, 30));
            a4.setAppointmentStatus("CONFIRMED");
            a4.setConsultationFeeSnapshot(new BigDecimal("600.00"));
            a4.setAppointmentType("IN_PERSON");
            a4.setPatientNotes("Child developmental wellness checkup and MMR immunization booster.");
            Long a4Id = appointmentRepository.save(a4);

            Payment pay4 = new Payment();
            pay4.setAppointmentId(a4Id);
            pay4.setAmount(new BigDecimal("600.00"));
            pay4.setPaymentMethod("UPI");
            pay4.setPaymentStatus("PAID");
            pay4.setTransactionReference("UPI-AURA-882104");
            paymentRepository.save(pay4);
        }

        // 5. Cancelled Appointment for Mohammed Fayis with Dr. Rajesh Varma (Neurology)
        if (doc4Id != null && pat3Id != null) {
            Appointment a5 = new Appointment();
            a5.setAppointmentReference("APT-2026-1005");
            a5.setPatientId(pat3Id);
            a5.setDoctorId(doc4Id);
            a5.setAppointmentDate(today.minusDays(1));
            a5.setStartTime(LocalTime.of(15, 0));
            a5.setEndTime(LocalTime.of(15, 30));
            a5.setAppointmentStatus("CANCELLED");
            a5.setConsultationFeeSnapshot(new BigDecimal("1100.00"));
            a5.setAppointmentType("IN_PERSON");
            a5.setPatientNotes("Recurring migraine headache evaluation.");
            Long a5Id = appointmentRepository.save(a5);

            appointmentRepository.updateStatus(a5Id, "CANCELLED", "Patient had emergency travel to Kozhikode. Rescheduling next week.", null);

            Payment pay5 = new Payment();
            pay5.setAppointmentId(a5Id);
            pay5.setAmount(new BigDecimal("1100.00"));
            pay5.setPaymentMethod("NET_BANKING");
            pay5.setPaymentStatus("REFUNDED");
            pay5.setTransactionReference("REF-AURA-882105");
            paymentRepository.save(pay5);
        }

        // 6. Confirmed Upcoming Appointment for Mohammed Fayis with Dr. Mathew Joseph (Orthopedics)
        if (doc5Id != null && pat3Id != null) {
            Appointment a6 = new Appointment();
            a6.setAppointmentReference("APT-2026-1006");
            a6.setPatientId(pat3Id);
            a6.setDoctorId(doc5Id);
            a6.setAppointmentDate(today.plusDays(3));
            a6.setStartTime(LocalTime.of(14, 0));
            a6.setEndTime(LocalTime.of(14, 30));
            a6.setAppointmentStatus("CONFIRMED");
            a6.setConsultationFeeSnapshot(new BigDecimal("950.00"));
            a6.setAppointmentType("IN_PERSON");
            a6.setPatientNotes("Right knee pain assessment after sports tournament in Calicut.");
            Long a6Id = appointmentRepository.save(a6);

            Payment pay6 = new Payment();
            pay6.setAppointmentId(a6Id);
            pay6.setAmount(new BigDecimal("950.00"));
            pay6.setPaymentMethod("UPI");
            pay6.setPaymentStatus("PAID");
            pay6.setTransactionReference("UPI-AURA-882106");
            paymentRepository.save(pay6);
        }

        // Seed Sample Notifications
        if (pat1UserId != null) {
            createNotification(pat1UserId, "Appointment Confirmed", "Your cardiology consultation with Dr. Ananya Menon is confirmed for tomorrow at 10:00 AM at Kochi Centre.", "APPOINTMENT");
            createNotification(pat1UserId, "Doctor Instructions Ready", "Dr. Ananya Menon recorded consultation notes & care plan for appointment APT-2026-1001.", "SCHEDULE");
            createNotification(pat1UserId, "Payment Receipt Available", "Your official PDF receipt for payment UPI-AURA-882101 (₹850.00) is ready for download.", "SYSTEM");
        }

        if (doc1UserId != null) {
            createNotification(doc1UserId, "New Consultation Booked", "Rahul Nair booked an appointment for tomorrow at 10:00 AM.", "APPOINTMENT");
            createNotification(doc1UserId, "Schedule Update", "Your weekly clinic availability is active for Mon - Sat from 09:00 to 17:00 at Kochi Centre.", "SCHEDULE");
        }

        if (adminUserId != null) {
            createNotification(adminUserId, "System Health OK", "Database connected and daily automated table integrity verification passed.", "SYSTEM");
            createNotification(adminUserId, "Revenue Milestone", "Aura Hospital Kochi received patient consultation receipts totaling ₹3,900.00 this cycle.", "SYSTEM");
        }

        // Seed Sample Audit Logs
        if (adminUserId != null) {
            auditLogRepository.log(adminUserId, "SYSTEM_INIT", "CONFIG", "0", "Aura Multispeciality Hospital Kochi system baseline accounts and clinical schedules initialized");
        }
        if (pat1UserId != null) {
            auditLogRepository.log(pat1UserId, "BOOK_APPOINTMENT", "APPOINTMENT", "APT-2026-1002", "Patient booked Cardiology appointment with Dr. Ananya Menon");
        }
    }

    private void createNotification(Long userId, String title, String msg, String type) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setTitle(title);
        n.setMessage(msg);
        n.setType(type);
        n.setReadStatus(false);
        notificationRepository.save(n);
    }
}
