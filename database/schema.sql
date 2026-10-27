-- ====================================================================
-- DOCTOR AVAILABILITY & SCHEDULING SYSTEM
-- Database Schema (MySQL 8.0+ / MariaDB)
-- Database Name: doctor_scheduling_system
-- ====================================================================

CREATE DATABASE IF NOT EXISTS doctor_scheduling_system
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE doctor_scheduling_system;

-- 1. USERS TABLE
CREATE TABLE IF NOT EXISTS users (
    user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    phone VARCHAR(30) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL, -- PATIENT, DOCTOR, ADMIN
    account_status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, INACTIVE, PENDING_APPROVAL, SUSPENDED
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_users_email (email),
    INDEX idx_users_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. SPECIALIZATIONS TABLE
CREATE TABLE IF NOT EXISTS specializations (
    specialization_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    specialization_name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    icon_name VARCHAR(50) DEFAULT 'stethoscope',
    active_status BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_spec_name (specialization_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. PATIENTS TABLE
CREATE TABLE IF NOT EXISTS patients (
    patient_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    date_of_birth DATE,
    gender VARCHAR(20),
    blood_group VARCHAR(10),
    address TEXT,
    emergency_contact VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_patients_users FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. DOCTORS TABLE
CREATE TABLE IF NOT EXISTS doctors (
    doctor_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    specialization_id BIGINT NOT NULL,
    license_number VARCHAR(100) NOT NULL UNIQUE,
    qualifications VARCHAR(255) NOT NULL,
    experience_years INT NOT NULL DEFAULT 0,
    consultation_fee DECIMAL(10,2) NOT NULL DEFAULT 50.00,
    default_appointment_duration INT NOT NULL DEFAULT 30, -- minutes
    professional_bio TEXT,
    profile_image_url VARCHAR(255),
    approval_status VARCHAR(30) NOT NULL DEFAULT 'APPROVED', -- APPROVED, PENDING, REJECTED
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_doctors_users FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
    CONSTRAINT fk_doctors_spec FOREIGN KEY (specialization_id) REFERENCES specializations (specialization_id),
    INDEX idx_doctors_spec (specialization_id),
    INDEX idx_doctors_approval (approval_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. DOCTOR SCHEDULES TABLE (Weekly working hours)
CREATE TABLE IF NOT EXISTS doctor_schedules (
    schedule_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    doctor_id BIGINT NOT NULL,
    working_day VARCHAR(20) NOT NULL, -- MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    break_start_time TIME NULL,
    break_end_time TIME NULL,
    slot_duration_minutes INT NOT NULL DEFAULT 30,
    active_status BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_schedules_doctor FOREIGN KEY (doctor_id) REFERENCES doctors (doctor_id) ON DELETE CASCADE,
    CONSTRAINT uk_doctor_day UNIQUE (doctor_id, working_day)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. DOCTOR UNAVAILABLE PERIODS / LEAVE TABLE
CREATE TABLE IF NOT EXISTS doctor_unavailable_periods (
    unavailable_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    doctor_id BIGINT NOT NULL,
    start_datetime DATETIME NOT NULL,
    end_datetime DATETIME NOT NULL,
    reason VARCHAR(255) NOT NULL,
    approval_status VARCHAR(30) NOT NULL DEFAULT 'APPROVED', -- PENDING, APPROVED, REJECTED
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_unavailable_doctor FOREIGN KEY (doctor_id) REFERENCES doctors (doctor_id) ON DELETE CASCADE,
    INDEX idx_unavailable_dates (doctor_id, start_datetime, end_datetime)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. APPOINTMENTS TABLE
CREATE TABLE IF NOT EXISTS appointments (
    appointment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    appointment_reference VARCHAR(64) NOT NULL UNIQUE,
    patient_id BIGINT NOT NULL,
    doctor_id BIGINT NOT NULL,
    appointment_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    appointment_status VARCHAR(30) NOT NULL DEFAULT 'CONFIRMED', -- PENDING_CONFIRMATION, CONFIRMED, CHECKED_IN, IN_PROGRESS, COMPLETED, CANCELLED, NO_SHOW
    consultation_fee_snapshot DECIMAL(10,2) NOT NULL,
    appointment_type VARCHAR(50) NOT NULL DEFAULT 'IN_PERSON', -- IN_PERSON, VIDEO_CONSULTATION, FOLLOW_UP
    patient_notes TEXT,
    cancellation_reason TEXT,
    doctor_instructions TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_appointments_patient FOREIGN KEY (patient_id) REFERENCES patients (patient_id),
    CONSTRAINT fk_appointments_doctor FOREIGN KEY (doctor_id) REFERENCES doctors (doctor_id),
    INDEX idx_app_doc_date (doctor_id, appointment_date),
    INDEX idx_app_pat_date (patient_id, appointment_date),
    INDEX idx_app_status (appointment_status),
    INDEX idx_app_ref (appointment_reference)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. APPOINTMENT HISTORY / AUDIT TRAIL TABLE
CREATE TABLE IF NOT EXISTS appointment_history (
    history_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    appointment_id BIGINT NOT NULL,
    actor_user_id BIGINT NULL,
    action VARCHAR(50) NOT NULL, -- BOOKED, CONFIRMED, RESCHEDULED, CANCELLED, CHECKED_IN, STARTED, COMPLETED, NO_SHOW
    old_status VARCHAR(50) NULL,
    new_status VARCHAR(50) NOT NULL,
    reason TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_history_appointment FOREIGN KEY (appointment_id) REFERENCES appointments (appointment_id) ON DELETE CASCADE,
    CONSTRAINT fk_history_actor FOREIGN KEY (actor_user_id) REFERENCES users (user_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 9. PAYMENTS TABLE
CREATE TABLE IF NOT EXISTS payments (
    payment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    appointment_id BIGINT NOT NULL UNIQUE,
    amount DECIMAL(10,2) NOT NULL,
    payment_method VARCHAR(50) NOT NULL DEFAULT 'CREDIT_CARD', -- CREDIT_CARD, CASH, UPI, INSURANCE
    payment_status VARCHAR(30) NOT NULL DEFAULT 'PENDING', -- PENDING, PAID, REFUNDED
    transaction_reference VARCHAR(100) UNIQUE NULL,
    recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_payments_appointment FOREIGN KEY (appointment_id) REFERENCES appointments (appointment_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 10. NOTIFICATIONS TABLE
CREATE TABLE IF NOT EXISTS notifications (
    notification_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(150) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(50) NOT NULL DEFAULT 'INFO', -- APPOINTMENT, REMINDER, SCHEDULE, SYSTEM
    read_status BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
    INDEX idx_notifications_user (user_id, read_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 11. SYSTEM SETTINGS TABLE
CREATE TABLE IF NOT EXISTS system_settings (
    setting_key VARCHAR(100) PRIMARY KEY,
    setting_value VARCHAR(255) NOT NULL,
    description VARCHAR(255) NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 12. AUDIT LOGS TABLE
CREATE TABLE IF NOT EXISTS audit_logs (
    audit_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    actor_user_id BIGINT NULL,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id VARCHAR(100) NULL,
    details TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_actor FOREIGN KEY (actor_user_id) REFERENCES users (user_id) ON DELETE SET NULL,
    INDEX idx_audit_action (action),
    INDEX idx_audit_time (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
