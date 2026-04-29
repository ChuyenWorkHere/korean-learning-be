package edu.language.kbee.controller;

import edu.language.kbee.payload.DictationLessonDto;
import edu.language.kbee.payload.request.DictationLessonRequest;
import edu.language.kbee.service.DictationLessonService;
import edu.language.kbee.service.AssemblyAiService;
import edu.language.kbee.service.YoutubeSubtitleService;
import edu.language.kbee.utils.TranscriptSplitter;
import edu.language.kbee.payload.DictationSentenceDto;
import tools.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/dictation")
@RequiredArgsConstructor
public class DictationLessonController {

    private final DictationLessonService dictationLessonService;
    private final AssemblyAiService assemblyAiService;
    private final YoutubeSubtitleService youtubeSubtitleService;

    @GetMapping("/topics/{topicId}/lessons")
    public ResponseEntity<List<DictationLessonDto>> getLessonsByTopic(@PathVariable String topicId) {
        return ResponseEntity.ok(dictationLessonService.getLessonsByTopic(topicId));
    }

    @GetMapping("/lessons/{lessonId}")
    public ResponseEntity<DictationLessonDto> getLessonById(@PathVariable String lessonId) {
        return ResponseEntity.ok(dictationLessonService.getLessonById(lessonId));
    }

    @PostMapping("/topics/{topicId}/lessons")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DictationLessonDto> createLesson(
            @PathVariable String topicId,
            @Valid @RequestBody DictationLessonRequest request) {
        return new ResponseEntity<>(dictationLessonService.createLesson(topicId, request), HttpStatus.CREATED);
    }

    @PutMapping("/lessons/{lessonId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DictationLessonDto> updateLesson(
            @PathVariable String lessonId,
            @Valid @RequestBody DictationLessonRequest request) {
        return ResponseEntity.ok(dictationLessonService.updateLesson(lessonId, request));
    }

    @DeleteMapping("/lessons/{lessonId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteLesson(@PathVariable String lessonId) {
        dictationLessonService.deleteLesson(lessonId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/lessons/{lessonId}/toggle-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DictationLessonDto> toggleStatus(@PathVariable String lessonId) {
        return ResponseEntity.ok(dictationLessonService.toggleStatus(lessonId));
    }

    @PostMapping("/lessons/generate-transcript")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> generateTranscript(@RequestBody Map<String, String> payload) {
        try {
            String audioUrl = payload.get("audioUrl");
            String languageCode = payload.get("languageCode");
            if (audioUrl == null || audioUrl.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Audio URL is required"));
            }
            JsonNode result = assemblyAiService.generateTranscript(audioUrl, languageCode);
            List<DictationSentenceDto> sentences = TranscriptSplitter.split(result);
            return ResponseEntity.ok(sentences);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "AI Error: " + e.getMessage()));
        }
    }

    @PostMapping("/lessons/youtube-transcript")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getYoutubeTranscript(@RequestBody Map<String, String> payload) {
        try {
            String youtubeId = payload.get("youtubeId");
            if (youtubeId == null || youtubeId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "YouTube ID is required"));
            }
            List<DictationSentenceDto> sentences = youtubeSubtitleService.extractSubtitles(youtubeId);
            return ResponseEntity.ok(sentences);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
