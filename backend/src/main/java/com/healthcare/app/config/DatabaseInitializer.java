package com.healthcare.app.config;

import com.healthcare.app.entity.*;
import com.healthcare.app.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalTime;

@Component
public class DatabaseInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseInitializer.class);

    @Autowired
    private DataSource dataSource;

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
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Initializing Healthcare System security and baseline accounts...");

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

        // 1. Ensure Specializations
        ensureSpecialization(1L, "Cardiology", "Heart health, hypertension, and cardiovascular diagnostic management.", "heart-pulse");
        ensureSpecialization(2L, "Dermatology", "Skin disorders, acne therapy, mole mapping, and eczema care.", "sparkles");
        ensureSpecialization(3L, "Pediatrics", "Complete medical care and vaccinations for infants, children, and teens.", "baby");
        ensureSpecialization(4L, "Neurology", "Disorders of the nervous system, migraines, memory, and nerve health.", "brain");
        ensureSpecialization(5L, "Orthopedics", "Bone, joint, spine, sports injury, and musculoskeletal care.", "bone");
        ensureSpecialization(6L, "General Medicine", "Routine annual wellness physicals, preventive care, and primary diagnosis.", "activity");

        // 2. Ensure Administrator
        if (!userRepository.existsByEmail("admin@healthcare.com")) {
            User admin = new User(null, "System Administrator", "admin@healthcare.com", "+1-800-555-0100", defaultPasswordHash, "ADMIN", "ACTIVE");
            userRepository.save(admin);
            log.info("Created default Administrator account: admin@healthcare.com / Password123!");
        }

        // 3. Ensure Seed Doctors
        ensureDoctor(
                "Dr. Sarah Jenkins, MD",
                "doctor.sarah@healthcare.com",
                "+1-800-555-0111",
                defaultPasswordHash,
                1L, // Cardiology
                "MED-CA-99214",
                "MD (Harvard), FACC Board Certified",
                14,
                new BigDecimal("120.00"),
                30,
                "Dr. Sarah Jenkins specializes in cardiovascular evaluation, hypertension, non-invasive imaging, and preventive cardiology.",
                "https://images.unsplash.com/photo-1559839734-2b71ea197ec2?auto=format&fit=crop&q=80&w=400"
        );

        ensureDoctor(
                "Dr. James Wilson, MD, PhD",
                "doctor.james@healthcare.com",
                "+1-800-555-0112",
                defaultPasswordHash,
                4L, // Neurology
                "MED-NE-44182",
                "MD (Johns Hopkins), PhD Neurology",
                18,
                new BigDecimal("150.00"),
                30,
                "Dr. James Wilson is a renowned specialist in chronic headache therapy, neurodegenerative disorders, and peripheral neuropathy.",
                "https://images.unsplash.com/photo-1622253692010-333f2da6031d?auto=format&fit=crop&q=80&w=400"
        );

        ensureDoctor(
                "Dr. Emily Chen, MD",
                "doctor.emily@healthcare.com",
                "+1-800-555-0113",
                defaultPasswordHash,
                3L, // Pediatrics
                "MED-PE-77319",
                "MD (Columbia), FAAP Pediatrics",
                11,
                new BigDecimal("95.00"),
                30,
                "Dr. Emily Chen is passionate about child development, infant health wellness, routine childhood immunizations, and teen medicine.",
                "https://images.unsplash.com/photo-1594824813583-93d1838cf0db?auto=format&fit=crop&q=80&w=400"
        );

        ensureDoctor(
                "Dr. Marcus Vance, DO",
                "doctor.marcus@healthcare.com",
                "+1-800-555-0114",
                defaultPasswordHash,
                2L, // Dermatology
                "MED-DE-33108",
                "DO (Stanford), FAAD Dermatology",
                9,
                new BigDecimal("110.00"),
                30,
                "Dr. Marcus Vance offers comprehensive dermatological consultations focusing on eczema, psoriasis, acne, and skin checks.",
                "https://images.unsplash.com/photo-1537368910025-700350fe46c7?auto=format&fit=crop&q=80&w=400"
        );

        // 4. Ensure Seed Patients
        ensurePatient(
                "Johnathan Miller",
                "patient.john@healthcare.com",
                "+1-800-555-0121",
                defaultPasswordHash,
                LocalDate.of(1988, 6, 14),
                "Male",
                "O+",
                "742 Evergreen Terrace, Springfield",
                "Jane Miller (+1-800-555-0131)"
        );

        ensurePatient(
                "Emma Watson",
                "patient.emma@healthcare.com",
                "+1-800-555-0122",
                defaultPasswordHash,
                LocalDate.of(1994, 3, 22),
                "Female",
                "A+",
                "120 Oxford Road, Cambridge",
                "Arthur Watson (+1-800-555-0132)"
        );

        log.info("Baseline security accounts and doctor profiles ready.");
    }

    private void ensureSpecialization(Long id, String name, String desc, String icon) {
        if (specializationRepository.findById(id).isEmpty()) {
            Specialization s = new Specialization(id, name, desc, icon, true);
            specializationRepository.save(s);
        }
    }

    private void ensureDoctor(String name, String email, String phone, String passwordHash, Long specId,
                              String license, String qual, int exp, BigDecimal fee, int duration, String bio, String imgUrl) {
        if (!userRepository.existsByEmail(email)) {
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

            // Create default Mon-Fri working schedule (09:00 - 17:00, lunch break 13:00 - 14:00)
            String[] days = {"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"};
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
        }
    }

    private void ensurePatient(String name, String email, String phone, String passwordHash,
                               LocalDate dob, String gender, String bloodGroup, String address, String emergencyContact) {
        if (!userRepository.existsByEmail(email)) {
            User user = new User(null, name, email, phone, passwordHash, "PATIENT", "ACTIVE");
            Long userId = userRepository.save(user);

            Patient p = new Patient();
            p.setUserId(userId);
            p.setDateOfBirth(dob);
            p.setGender(gender);
            p.setBloodGroup(bloodGroup);
            p.setAddress(address);
            p.setEmergencyContact(emergencyContact);
            patientRepository.save(p);
        }
    }
}
