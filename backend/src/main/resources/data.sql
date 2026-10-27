-- ====================================================================
-- Initial Seed Data (Standard ANSI SQL)
-- ====================================================================

-- 1. System Settings
INSERT INTO system_settings (setting_key, setting_value, description)
SELECT 'CLINIC_NAME', 'Aura Health & Medical Specialist Center', 'Official clinic branding name'
WHERE NOT EXISTS (SELECT 1 FROM system_settings WHERE setting_key = 'CLINIC_NAME');

INSERT INTO system_settings (setting_key, setting_value, description)
SELECT 'CLINIC_ADDRESS', '450 Healthcare Blvd, Suite 800, Metro City', 'Clinic physical address'
WHERE NOT EXISTS (SELECT 1 FROM system_settings WHERE setting_key = 'CLINIC_ADDRESS');

INSERT INTO system_settings (setting_key, setting_value, description)
SELECT 'CLINIC_PHONE', '+1 (800) 555-0199', 'Main patient helpline and booking desk'
WHERE NOT EXISTS (SELECT 1 FROM system_settings WHERE setting_key = 'CLINIC_PHONE');

INSERT INTO system_settings (setting_key, setting_value, description)
SELECT 'CLINIC_EMAIL', 'appointments@aurahealth.org', 'Official healthcare email'
WHERE NOT EXISTS (SELECT 1 FROM system_settings WHERE setting_key = 'CLINIC_EMAIL');

INSERT INTO system_settings (setting_key, setting_value, description)
SELECT 'DEFAULT_APPOINTMENT_DURATION_MINUTES', '30', 'Standard appointment duration in minutes'
WHERE NOT EXISTS (SELECT 1 FROM system_settings WHERE setting_key = 'DEFAULT_APPOINTMENT_DURATION_MINUTES');

INSERT INTO system_settings (setting_key, setting_value, description)
SELECT 'MIN_ADVANCE_BOOKING_HOURS', '0', 'Minimum advance booking time before appointment'
WHERE NOT EXISTS (SELECT 1 FROM system_settings WHERE setting_key = 'MIN_ADVANCE_BOOKING_HOURS');

INSERT INTO system_settings (setting_key, setting_value, description)
SELECT 'MAX_ADVANCE_BOOKING_DAYS', '60', 'Maximum scheduling calendar window in days'
WHERE NOT EXISTS (SELECT 1 FROM system_settings WHERE setting_key = 'MAX_ADVANCE_BOOKING_DAYS');

INSERT INTO system_settings (setting_key, setting_value, description)
SELECT 'CANCELLATION_DEADLINE_HOURS', '4', 'Minimum hours before appointment to cancel or reschedule'
WHERE NOT EXISTS (SELECT 1 FROM system_settings WHERE setting_key = 'CANCELLATION_DEADLINE_HOURS');

-- 2. Specializations
INSERT INTO specializations (specialization_id, specialization_name, description, icon_name, active_status)
SELECT 1, 'Cardiology', 'Diagnosis and treatment of congenital heart defects, coronary artery disease, heart failure, and valvular heart disease.', 'heart-pulse', TRUE
WHERE NOT EXISTS (SELECT 1 FROM specializations WHERE specialization_id = 1);

INSERT INTO specializations (specialization_id, specialization_name, description, icon_name, active_status)
SELECT 2, 'Dermatology', 'Expert medical care for skin conditions, pediatric dermatology, and cosmetic skin wellness.', 'sparkles', TRUE
WHERE NOT EXISTS (SELECT 1 FROM specializations WHERE specialization_id = 2);

INSERT INTO specializations (specialization_id, specialization_name, description, icon_name, active_status)
SELECT 3, 'Pediatrics', 'Comprehensive physical, behavioral, and mental healthcare for infants, children, and adolescents.', 'baby', TRUE
WHERE NOT EXISTS (SELECT 1 FROM specializations WHERE specialization_id = 3);

INSERT INTO specializations (specialization_id, specialization_name, description, icon_name, active_status)
SELECT 4, 'Neurology', 'Diagnosis and management of disorders of the nervous system, brain, and spinal cord.', 'brain', TRUE
WHERE NOT EXISTS (SELECT 1 FROM specializations WHERE specialization_id = 4);

INSERT INTO specializations (specialization_id, specialization_name, description, icon_name, active_status)
SELECT 5, 'Orthopedics', 'Treatment of musculoskeletal system conditions, bones, joints, ligaments, tendons, and muscles.', 'bone', TRUE
WHERE NOT EXISTS (SELECT 1 FROM specializations WHERE specialization_id = 5);

INSERT INTO specializations (specialization_id, specialization_name, description, icon_name, active_status)
SELECT 6, 'General Medicine', 'Primary care, preventive medicine, health risk assessments, and routine checkups.', 'activity', TRUE
WHERE NOT EXISTS (SELECT 1 FROM specializations WHERE specialization_id = 6);
