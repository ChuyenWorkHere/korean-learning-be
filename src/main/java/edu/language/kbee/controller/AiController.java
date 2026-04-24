package edu.language.kbee.controller;

import edu.language.kbee.service.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService openRouterAi;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/generate-writing")
    public ResponseEntity<?> generateWritingPrompt(@RequestBody Map<String, String> payload) {
        try {
            String topic = payload.getOrDefault("topic", "Korean Culture and Society");
            return ResponseEntity.ok(openRouterAi.generateWritingPrompt(topic));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/generate-translation")
    public ResponseEntity<?> generateTranslationPrompt(@RequestBody Map<String, String> payload) {
        try {
            String topic = payload.getOrDefault("topic", "Daily Life");
            return ResponseEntity.ok(openRouterAi.generateTranslationPrompt(topic));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/generate-quizzes")
    public ResponseEntity<?> generateQuizzes(@RequestBody Map<String, String> payload) {
        try {
            String context = payload.get("context");
            String skillType = payload.get("skillType");
            
            if (context == null || context.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Context is required"));
            }
            if (skillType == null || skillType.trim().isEmpty()) {
                skillType = "general";
            }
            
            return ResponseEntity.ok(openRouterAi.generateQuizzes(context, skillType));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}