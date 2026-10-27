-- ====================================================================
-- DOCTOR AVAILABILITY & SCHEDULING SYSTEM
-- Sample Initial Data Script
-- Default password for all seed accounts is: Password123!
-- ====================================================================

USE doctor_scheduling_system;

-- 1. SYSTEM SETTINGS
INSERT INTO system_settings (setting_key, setting_value, description) VALUES
('CLINIC_NAME', 'Aura Health & Medical Specialist Center', 'Official clinic branding name'),
('CLINIC_ADDRESS', '450 Healthcare Blvd, Suite 800, Metro City', 'Clinic physical address'),
('CLINIC_PHONE', '+1 (800) 555-0199', 'Main patient helpline and booking desk'),
('CLINIC_EMAIL', 'support@aurahealth.org', 'Official healthcare email'),
('DEFAULT_APPOINTMENT_DURATION_MINUTES', '30', 'Standard appointment duration in minutes'),
('MIN_ADVANCE_BOOKING_HOURS', '2', 'Minimum advance booking time before appointment'),
('MAX_ADVANCE_BOOKING_DAYS', '60', 'Maximum scheduling calendar window in days'),
('CANCELLATION_DEADLINE_HOURS', '4', 'Minimum hours before appointment to cancel or reschedule'),
('AUTO_CONFIRM_BOOKINGS', 'true', 'Whether new bookings are automatically confirmed')
ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value);

-- 2. SPECIALIZATIONS
INSERT INTO specializations (specialization_id, specialization_name, description, icon_name, active_status) VALUES
(1, 'Cardiology', 'Diagnosis and treatment of congenital heart defects, coronary artery disease, heart failure, and valvular heart disease.', 'heart-pulse', TRUE),
(2, 'Dermatology', 'Expert medical care for skin conditions, pediatric dermatology, and cosmetic skin wellness.', 'sparkles', TRUE),
(3, 'Pediatrics', 'Comprehensive physical, behavioral, and mental healthcare for infants, children, and adolescents.', 'baby', TRUE),
(4, 'Neurology', 'Diagnosis and management of disorders of the nervous system, brain, and spinal cord.', 'brain', TRUE),
(5, 'Orthopedics', 'Treatment of musculoskeletal system conditions, bones, joints, ligaments, tendons, and muscles.', 'bone', TRUE),
(6, 'General Medicine', 'Primary care, preventive medicine, health risk assessments, and routine checkups.', 'activity', TRUE)
ON DUPLICATE KEY UPDATE specialization_name = VALUES(specialization_name);

-- 3. USERS (Seed Passwords: Password123!)
-- Hash corresponds to standard BCrypt of 'Password123!'
INSERT INTO users (user_id, full_name, email, phone, password_hash, role, account_status) VALUES
(1, 'System Administrator', 'admin@healthcare.com', '+1-800-555-0100', '$2a$10$wT8hWj4wP40c5f0vjV0Hne508jR3m9rXgG0aM1Jz6uJ1T6bY6H8hG', 'ADMIN', 'ACTIVE'),
(2, 'Dr. Sarah Jenkins, MD', 'doctor.sarah@healthcare.com', '+1-800-555-0111', '$2a$10$wT8hWj4wP40c5f0vjV0Hne508jR3m9rXgG0aM1Jz6uJ1T6bY6H8hG', 'DOCTOR', 'ACTIVE'),
(3, 'Dr. James Wilson, MD, FACC', 'doctor.james@healthcare.com', '+1-800-555-0112', '$2a$10$wT8hWj4wP40c5f0vjV0Hne508jR3m9rXgG0aM1Jz6uJ1T6bY6H8hG', 'DOCTOR', 'ACTIVE'),
(4, 'Dr. Emily Chen, MD', 'doctor.emily@healthcare.com', '+1-800-555-0113', '$2a$10$wT8hWj4wP40c5f0vjV0Hne508jR3m9rXgG0aM1Jz6uJ1T6bY6H8hG', 'DOCTOR', 'ACTIVE'),
(5, 'Dr. Marcus Vance, DO', 'doctor.marcus@healthcare.com', '+1-800-555-0114', '$2a$10$wT8hWj4wP40c5f0vjV0Hne508jR3m9rXgG0aM1Jz6uJ1T6bY6H8hG', 'DOCTOR', 'ACTIVE'),
(6, 'Johnathan Miller', 'patient.john@healthcare.com', '+1-800-555-0121', '$2a$10$wT8hWj4wP40c5f0vjV0Hne508jR3m9rXgG0aM1Jz6uJ1T6bY6H8hG', 'PATIENT', 'ACTIVE'),
(7, 'Emma Watson', 'patient.emma@healthcare.com', '+1-800-555-0122', '$2a$10$wT8hWj4wP40c5f0vjV0Hne508jR3m9rXgG0aM1Jz6uJ1T6bY6H8hG', 'PATIENT', 'ACTIVE'),
(8, 'Robert Davis', 'patient.robert@healthcare.com', '+1-800-555-0123', '$2a$10$wT8hWj4wP40c5f0vjV0Hne508jR3m9rXgG0aM1Jz6uJ1T6bY6H8hG', 'PATIENT', 'ACTIVE')
ON DUPLICATE KEY UPDATE full_name = VALUES(full_name);

-- 4. PATIENT PROFILES
INSERT INTO patients (patient_id, user_id, date_of_birth, gender, blood_group, address, emergency_contact) VALUES
(1, 6, '1988-06-14', 'Male', 'O+', '742 Evergreen Terrace, Springfield', 'Jane Miller (+1-800-555-0131)'),
(2, 7, '1994-03-22', 'Female', 'A+', '120 Oxford Road, Cambridge', 'Arthur Watson (+1-800-555-0132)'),
(3, 8, '1979-11-05', 'Male', 'B+', '88 Pinecrest Street, Seattle', 'Mary Davis (+1-800-555-0133)')
ON DUPLICATE KEY UPDATE user_id = VALUES(user_id);

-- 5. DOCTOR PROFILES
INSERT INTO doctors (doctor_id, user_id, specialization_id, license_number, qualifications, experience_years, consultation_fee, default_appointment_duration, professional_bio, profile_image_url, approval_status) VALUES
(1, 2, 1, 'MED-CA-99214', 'MD (Harvard), FACC, Board Certified Cardiologist', 14, 120.00, 30, 'Dr. Sarah Jenkins is an experienced cardiologist specializing in cardiovascular wellness, lipidology, hypertension, and non-invasive diagnostic evaluations.', 'https://images.unsplash.com/photo-1559839734-2b71ea197ec2?auto=format&fit=crop&q=80&w=400', 'APPROVED'),
(2, 3, 4, 'MED-NE-44182', 'MD (Johns Hopkins), PhD, Board Certified Neurologist', 18, 150.00, 30, 'Dr. James Wilson is a renowned neurologist with nearly two decades of clinical experience in headache management, neurodegenerative disorders, and neuropathy.', 'https://images.unsplash.com/photo-1622253692010-333f2da6031d?auto=format&fit=crop&q=80&w=400', 'APPROVED'),
(3, 4, 3, 'MED-PE-77319', 'MD (Columbia University), FAAP Pediatrics', 11, 95.00, 30, 'Dr. Emily Chen is dedicated to compassionate child healthcare, infant wellness development, routine immunizations, and pediatric adolescent medicine.', 'https://images.unsplash.com/photo-1594824813583-93d1838cf0db?auto=format&fit=crop&q=80&w=400', 'APPROVED'),
(4, 5, 2, 'MED-DE-33108', 'DO (Stanford), Fellow American Academy of Dermatology', 9, 110.00, 30, 'Dr. Marcus Vance provides cutting-edge dermatological consultations, specializing in acne therapy, eczema, skin cancer surveillance, and mole mapping.', 'https://images.unsplash.com/photo-1537368910025-700350fe46c7?auto=format&fit=crop&q=80&w=400', 'APPROVED')
ON DUPLICATE KEY UPDATE license_number = VALUES(license_number);

-- 6. DOCTOR SCHEDULES (Monday - Friday 09:00 - 17:00, lunch break 13:00 - 14:00)
INSERT INTO doctor_schedules (doctor_id, working_day, start_time, end_time, break_start_time, break_end_time, slot_duration_minutes, active_status) VALUES
-- Dr. Sarah Jenkins (Cardiology): Mon, Tue, Wed, Thu, Fri
(1, 'MONDAY', '09:00:00', '17:00:00', '13:00:00', '14:00:00', 30, TRUE),
(1, 'TUESDAY', '09:00:00', '17:00:00', '13:00:00', '14:00:00', 30, TRUE),
(1, 'WEDNESDAY', '09:00:00', '17:00:00', '13:00:00', '14:00:00', 30, TRUE),
(1, 'THURSDAY', '09:00:00', '17:00:00', '13:00:00', '14:00:00', 30, TRUE),
(1, 'FRIDAY', '09:00:00', '16:00:00', '12:30:00', '13:30:00', 30, TRUE),

-- Dr. James Wilson (Neurology): Mon, Wed, Fri, Sat
(2, 'MONDAY', '10:00:00', '18:00:00', '13:30:00', '14:30:00', 30, TRUE),
(2, 'WEDNESDAY', '10:00:00', '18:00:00', '13:30:00', '14:30:00', 30, TRUE),
(2, 'FRIDAY', '10:00:00', '17:00:00', '13:00:00', '14:00:00', 30, TRUE),
(2, 'SATURDAY', '09:30:00', '14:30:00', NULL, NULL, 30, TRUE),

-- Dr. Emily Chen (Pediatrics): Tue, Wed, Thu, Fri, Sat
(3, 'TUESDAY', '08:30:00', '16:30:00', '12:00:00', '13:00:00', 30, TRUE),
(3, 'WEDNESDAY', '08:30:00', '16:30:00', '12:00:00', '13:00:00', 30, TRUE),
(3, 'THURSDAY', '08:30:00', '16:30:00', '12:00:00', '13:00:00', 30, TRUE),
(3, 'FRIDAY', '08:30:00', '16:30:00', '12:00:00', '13:00:00', 30, TRUE),
(3, 'SATURDAY', '09:00:00', '13:00:00', NULL, NULL, 30, TRUE),

-- Dr. Marcus Vance (Dermatology): Mon, Tue, Thu, Fri
(4, 'MONDAY', '09:00:00', '17:00:00', '13:00:00', '14:00:00', 30, TRUE),
(4, 'TUESDAY', '09:00:00', '17:00:00', '13:00:00', '14:00:00', 30, TRUE),
(4, 'THURSDAY', '09:00:00', '17:00:00', '13:00:00', '14:00:00', 30, TRUE),
(4, 'FRIDAY', '09:00:00', '17:00:00', '13:00:00', '14:00:00', 30, TRUE)
ON DUPLICATE KEY UPDATE start_time = VALUES(start_time), end_time = VALUES(end_time);

-- 7. SAMPLE APPOINTMENTS
INSERT INTO appointments (appointment_id, appointment_reference, patient_id, doctor_id, appointment_date, start_time, end_time, appointment_status, consultation_fee_snapshot, appointment_type, patient_notes, doctor_instructions) VALUES
(1, 'DOC-20261028-A101', 1, 1, '2026-10-28', '10:00:00', '10:30:00', 'CONFIRMED', 120.00, 'IN_PERSON', 'Routine annual cardiac checkup and blood pressure review.', 'Please bring previous ECG records and fast for 4 hours before your visit.'),
(2, 'DOC-20261028-B202', 2, 2, '2026-10-28', '11:00:00', '11:30:00', 'CHECKED_IN', 150.00, 'IN_PERSON', 'Persistent tension headaches for 3 weeks.', 'Keep a headache trigger journal for the consultation.'),
(3, 'DOC-20261029-C303', 3, 3, '2026-10-29', '09:30:00', '10:00:00', 'PENDING_CONFIRMATION', 95.00, 'IN_PERSON', 'Seasonal allergy follow-up.', NULL),
(4, 'DOC-20261025-D404', 1, 4, '2026-10-25', '14:00:00', '14:30:00', 'COMPLETED', 110.00, 'IN_PERSON', 'Skin allergy patch test evaluation.', 'Prescription moisturizer advised; return in 4 weeks if irritation persists.'),
(5, 'DOC-20261024-E505', 2, 1, '2026-10-24', '15:00:00', '15:30:00', 'CANCELLED', 120.00, 'IN_PERSON', 'Schedule conflict with work travel.', NULL)
ON DUPLICATE KEY UPDATE appointment_reference = VALUES(appointment_reference);

-- 8. APPOINTMENT HISTORY
INSERT INTO appointment_history (appointment_id, actor_user_id, action, old_status, new_status, reason) VALUES
(1, 6, 'BOOKED', NULL, 'CONFIRMED', 'Initial booking confirmed automatically by system rules'),
(2, 7, 'BOOKED', NULL, 'CONFIRMED', 'Initial booking confirmed'),
(2, 2, 'CHECKED_IN', 'CONFIRMED', 'CHECKED_IN', 'Patient arrived at clinic front desk reception'),
(3, 8, 'BOOKED', NULL, 'PENDING_CONFIRMATION', 'New booking pending doctor schedule validation'),
(4, 6, 'BOOKED', NULL, 'CONFIRMED', 'Initial booking'),
(4, 5, 'COMPLETED', 'IN_PROGRESS', 'COMPLETED', 'Consultation finished; medical notes filed'),
(5, 7, 'CANCELLED', 'CONFIRMED', 'CANCELLED', 'Patient requested cancellation prior to deadline')
ON DUPLICATE KEY UPDATE action = VALUES(action);

-- 9. PAYMENTS
INSERT INTO payments (payment_id, appointment_id, amount, payment_method, payment_status, transaction_reference) VALUES
(1, 1, 120.00, 'CREDIT_CARD', 'PAID', 'TXN-AURA-99201'),
(2, 2, 150.00, 'INSURANCE', 'PAID', 'TXN-AURA-88312'),
(3, 3, 95.00, 'CREDIT_CARD', 'PENDING', 'TXN-AURA-PENDING-03'),
(4, 4, 110.00, 'UPI', 'PAID', 'TXN-AURA-77190'),
(5, 5, 120.00, 'CREDIT_CARD', 'REFUNDED', 'TXN-AURA-REF-05')
ON DUPLICATE KEY UPDATE transaction_reference = VALUES(transaction_reference);

-- 10. NOTIFICATIONS
INSERT INTO notifications (user_id, title, message, type, read_status) VALUES
(6, 'Appointment Confirmed', 'Your appointment with Dr. Sarah Jenkins on Oct 28, 2026 at 10:00 AM has been confirmed.', 'APPOINTMENT', FALSE),
(7, 'Patient Checked In', 'Patient Emma Watson has checked in for the 11:00 AM consultation.', 'APPOINTMENT', TRUE),
(8, 'Booking Received', 'Your booking request with Dr. Emily Chen is awaiting confirmation.', 'APPOINTMENT', FALSE),
(1, 'System Audit Event', 'Weekly automated database integrity check executed with zero warnings.', 'SYSTEM', TRUE);
