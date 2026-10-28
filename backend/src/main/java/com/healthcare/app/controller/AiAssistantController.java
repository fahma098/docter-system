package com.healthcare.app.controller;

import com.healthcare.app.dto.ApiResponse;
import com.healthcare.app.service.AiAssistantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiAssistantController {

    @Autowired
    private AiAssistantService aiAssistantService;

    public static class ChatRequest {
        private String message;

        public ChatRequest() {}

        public ChatRequest(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<Map<String, Object>>> chatWithAi(@RequestBody(required = false) ChatRequest req) {
        String msg = (req != null && req.getMessage() != null) ? req.getMessage() : "";
        Map<String, Object> result = aiAssistantService.processHealthQuery(msg);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
