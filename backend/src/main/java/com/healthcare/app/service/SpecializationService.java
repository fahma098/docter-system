package com.healthcare.app.service;

import com.healthcare.app.entity.Specialization;
import com.healthcare.app.exception.ResourceNotFoundException;
import com.healthcare.app.repository.AuditLogRepository;
import com.healthcare.app.repository.SpecializationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SpecializationService {

    @Autowired
    private SpecializationRepository specializationRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    public List<Specialization> getAllActive() {
        return specializationRepository.findAllActive();
    }

    public List<Specialization> getAll() {
        return specializationRepository.findAll();
    }

    public Specialization getById(Long id) {
        return specializationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Specialization not found with id: " + id));
    }

    @Transactional
    public Specialization create(Specialization s, Long actorUserId) {
        Long id = specializationRepository.save(s);
        s.setSpecializationId(id);
        auditLogRepository.log(actorUserId, "CREATE_SPECIALIZATION", "SPECIALIZATION", String.valueOf(id), "Created specialization: " + s.getSpecializationName());
        return s;
    }

    @Transactional
    public Specialization update(Specialization s, Long actorUserId) {
        specializationRepository.findById(s.getSpecializationId())
                .orElseThrow(() -> new ResourceNotFoundException("Specialization not found with id: " + s.getSpecializationId()));
        specializationRepository.update(s);
        auditLogRepository.log(actorUserId, "UPDATE_SPECIALIZATION", "SPECIALIZATION", String.valueOf(s.getSpecializationId()), "Updated specialization: " + s.getSpecializationName());
        return s;
    }
}
