package edu.language.kbee.controller;

import edu.language.kbee.service.GeminiAiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {

    private final GeminiAiService geminiAiService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/generate-writing")
    public ResponseEntity<?> generateWritingPrompt(@RequestBody Map<String, String> payload) {
        try {
            String topic = payload.getOrDefault("topic", "Korean Culture and Society");
            return ResponseEntity.ok(geminiAiService.generateWritingPrompt(topic));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}