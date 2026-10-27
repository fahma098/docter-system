package com.healthcare.app.controller;

import com.healthcare.app.dto.ApiResponse;
import com.healthcare.app.entity.Specialization;
import com.healthcare.app.security.RequiresRole;
import com.healthcare.app.security.SecurityContextHelper;
import com.healthcare.app.service.SpecializationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class SpecializationController {

    @Autowired
    private SpecializationService specializationService;

    @GetMapping("/specializations")
    public ResponseEntity<ApiResponse<List<Specialization>>> getSpecializations() {
        List<Specialization> list = specializationService.getAllActive();
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @GetMapping("/specializations/{id}")
    public ResponseEntity<ApiResponse<Specialization>> getSpecializationById(@PathVariable Long id) {
        Specialization s = specializationService.getById(id);
        return ResponseEntity.ok(ApiResponse.ok(s));
    }

    @PostMapping("/admin/specializations")
    @RequiresRole("ADMIN")
    public ResponseEntity<ApiResponse<Specialization>> createSpecialization(@RequestBody Specialization s) {
        Long actorId = SecurityContextHelper.getCurrentUserId();
        Specialization created = specializationService.create(s, actorId);
        return ResponseEntity.ok(ApiResponse.ok("Specialization created", created));
    }

    @PutMapping("/admin/specializations/{id}")
    @RequiresRole("ADMIN")
    public ResponseEntity<ApiResponse<Specialization>> updateSpecialization(@PathVariable Long id, @RequestBody Specialization s) {
        Long actorId = SecurityContextHelper.getCurrentUserId();
        s.setSpecializationId(id);
        Specialization updated = specializationService.update(s, actorId);
        return ResponseEntity.ok(ApiResponse.ok("Specialization updated", updated));
    }
}
