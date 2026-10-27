package com.healthcare.app.controller;

import com.healthcare.app.dto.ApiResponse;
import com.healthcare.app.entity.Notification;
import com.healthcare.app.repository.NotificationRepository;
import com.healthcare.app.security.SecurityContextHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationRepository notificationRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Notification>>> getMyNotifications() {
        Long userId = SecurityContextHelper.getCurrentUserId();
        List<Notification> list = notificationRepository.findByUserId(userId);
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable Long id) {
        Long userId = SecurityContextHelper.getCurrentUserId();
        notificationRepository.markAsRead(id, userId);
        return ResponseEntity.ok(ApiResponse.okMessage("Marked notification as read"));
    }

    @PostMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead() {
        Long userId = SecurityContextHelper.getCurrentUserId();
        notificationRepository.markAllAsRead(userId);
        return ResponseEntity.ok(ApiResponse.okMessage("Marked all notifications as read"));
    }
}
