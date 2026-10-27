package com.healthcare.app.service;

import com.healthcare.app.dto.CreateDoctorRequest;
import com.healthcare.app.dto.DoctorDto;
import com.healthcare.app.dto.ScheduleDto;
import com.healthcare.app.dto.UpdateDoctorRequest;
import com.healthcare.app.entity.Doctor;
import com.healthcare.app.entity.DoctorSchedule;
import com.healthcare.app.entity.User;
import com.healthcare.app.exception.BusinessValidationException;
import com.healthcare.app.exception.ResourceNotFoundException;
import com.healthcare.app.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DoctorService {

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private SpecializationRepository specializationRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public List<DoctorDto> searchDoctors(Long specializationId, String query, Boolean approvedOnly) {
        List<Doctor> doctors = doctorRepository.searchDoctors(specializationId, query, approvedOnly);
        return doctors.stream().map(this::toDto).collect(Collectors.toList());
    }

    public DoctorDto getDoctorById(Long doctorId) {
        Doctor doc = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with ID: " + doctorId));
        return toDto(doc);
    }

    public DoctorDto getDoctorByUserId(Long userId) {
        Doctor doc = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found for user ID: " + userId));
        return toDto(doc);
    }

    @Transactional
    public DoctorDto createDoctor(CreateDoctorRequest req, Long actorUserId) {
        String email = req.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new BusinessValidationException("A user with email " + email + " already exists");
        }

        specializationRepository.findById(req.getSpecializationId())
                .orElseThrow(() -> new BusinessValidationException("Invalid specialization ID: " + req.getSpecializationId()));

        String hashed = passwordEncoder.encode(req.getPassword());
        User user = new User(null, req.getFullName().trim(), email, req.getPhone().trim(), hashed, "DOCTOR", "ACTIVE");
        Long userId = userRepository.save(user);

        Doctor doc = new Doctor();
        doc.setUserId(userId);
        doc.setSpecializationId(req.getSpecializationId());
        doc.setLicenseNumber(req.getLicenseNumber().trim());
        doc.setQualifications(req.getQualifications().trim());
        doc.setExperienceYears(req.getExperienceYears());
        doc.setConsultationFee(req.getConsultationFee());
        doc.setDefaultAppointmentDuration(req.getDefaultAppointmentDuration() > 0 ? req.getDefaultAppointmentDuration() : 30);
        doc.setProfessionalBio(req.getProfessionalBio());
        doc.setProfileImageUrl(req.getProfileImageUrl());
        doc.setApprovalStatus("APPROVED");
        Long doctorId = doctorRepository.save(doc);

        // Add standard Mon-Fri schedule
        String[] days = {"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"};
        for (String day : days) {
            DoctorSchedule s = new DoctorSchedule();
            s.setDoctorId(doctorId);
            s.setWorkingDay(day);
            s.setStartTime(LocalTime.of(9, 0));
            s.setEndTime(LocalTime.of(17, 0));
            s.setBreakStartTime(LocalTime.of(13, 0));
            s.setBreakEndTime(LocalTime.of(14, 0));
            s.setSlotDurationMinutes(doc.getDefaultAppointmentDuration());
            s.setActiveStatus(true);
            scheduleRepository.save(s);
        }

        auditLogRepository.log(actorUserId, "CREATE_DOCTOR", "DOCTOR", String.valueOf(doctorId), "Admin added doctor: " + req.getFullName());

        return getDoctorById(doctorId);
    }

    @Transactional
    public DoctorDto updateDoctor(Long doctorId, UpdateDoctorRequest req, Long actorUserId) {
        Doctor doc = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with ID: " + doctorId));

        if (req.getSpecializationId() != null) {
            specializationRepository.findById(req.getSpecializationId())
                    .orElseThrow(() -> new BusinessValidationException("Invalid specialization ID: " + req.getSpecializationId()));
            doc.setSpecializationId(req.getSpecializationId());
        }
        if (req.getQualifications() != null) doc.setQualifications(req.getQualifications().trim());
        if (req.getExperienceYears() != null) doc.setExperienceYears(req.getExperienceYears());
        if (req.getConsultationFee() != null) doc.setConsultationFee(req.getConsultationFee());
        if (req.getDefaultAppointmentDuration() != null) doc.setDefaultAppointmentDuration(req.getDefaultAppointmentDuration());
        if (req.getProfessionalBio() != null) doc.setProfessionalBio(req.getProfessionalBio());
        if (req.getProfileImageUrl() != null) doc.setProfileImageUrl(req.getProfileImageUrl());

        doctorRepository.update(doc);

        if (req.getFullName() != null || req.getPhone() != null) {
            User user = userRepository.findById(doc.getUserId()).orElse(null);
            if (user != null) {
                String newName = req.getFullName() != null ? req.getFullName().trim() : user.getFullName();
                String newPhone = req.getPhone() != null ? req.getPhone().trim() : user.getPhone();
                userRepository.updateContactInfo(user.getUserId(), newName, newPhone);
            }
        }

        auditLogRepository.log(actorUserId, "UPDATE_DOCTOR", "DOCTOR", String.valueOf(doctorId), "Updated doctor profile for ID: " + doctorId);

        return getDoctorById(doctorId);
    }

    @Transactional
    public void updateApprovalStatus(Long doctorId, String status, Long actorUserId) {
        doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with ID: " + doctorId));
        doctorRepository.updateApprovalStatus(doctorId, status);
        auditLogRepository.log(actorUserId, "UPDATE_APPROVAL", "DOCTOR", String.valueOf(doctorId), "Updated doctor approval status to: " + status);
    }

    public DoctorDto toDto(Doctor d) {
        DoctorDto dto = new DoctorDto();
        dto.setDoctorId(d.getDoctorId());
        dto.setUserId(d.getUserId());
        dto.setFullName(d.getFullName());
        dto.setEmail(d.getEmail());
        dto.setPhone(d.getPhone());
        dto.setSpecializationId(d.getSpecializationId());
        dto.setSpecializationName(d.getSpecializationName());
        dto.setLicenseNumber(d.getLicenseNumber());
        dto.setQualifications(d.getQualifications());
        dto.setExperienceYears(d.getExperienceYears());
        dto.setConsultationFee(d.getConsultationFee());
        dto.setDefaultAppointmentDuration(d.getDefaultAppointmentDuration());
        dto.setProfessionalBio(d.getProfessionalBio());
        dto.setProfileImageUrl(d.getProfileImageUrl());
        dto.setApprovalStatus(d.getApprovalStatus());

        List<DoctorSchedule> schedules = scheduleRepository.findByDoctorId(d.getDoctorId());
        List<ScheduleDto> schedDtos = new ArrayList<>();
        for (DoctorSchedule s : schedules) {
            ScheduleDto sd = new ScheduleDto();
            sd.setScheduleId(s.getScheduleId());
            sd.setDoctorId(s.getDoctorId());
            sd.setWorkingDay(s.getWorkingDay());
            sd.setStartTime(s.getStartTime());
            sd.setEndTime(s.getEndTime());
            sd.setBreakStartTime(s.getBreakStartTime());
            sd.setBreakEndTime(s.getBreakEndTime());
            sd.setSlotDurationMinutes(s.getSlotDurationMinutes());
            sd.setActiveStatus(s.isActiveStatus());
            schedDtos.add(sd);
        }
        dto.setSchedules(schedDtos);
        return dto;
    }
}
