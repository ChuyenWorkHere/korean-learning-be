package edu.language.kbee.controller;

import edu.language.kbee.payload.request.LessonCompletionRequest;
import edu.language.kbee.service.LessonService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class ProgressController {

    private final LessonService lessonService;

    @PutMapping("/lessons/{lessonId}/complete")
    public ResponseEntity<Map<String, String>> completeLesson(@PathVariable String lessonId, 
                                                            @RequestBody LessonCompletionRequest request) {
        Map<String, String> nextLessonData = lessonService.completeLessonByUser(lessonId, request);
        return ResponseEntity.ok(nextLessonData);
    }

}
