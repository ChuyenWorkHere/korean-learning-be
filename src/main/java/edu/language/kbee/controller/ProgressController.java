package edu.language.kbee.controller;

import edu.language.kbee.service.LessonService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class ProgressController {

    private final LessonService lessonService;

    @PutMapping("/lessons/{lessonId}/complete")
    public ResponseEntity<String> completeLesson(@PathVariable String lessonId) {
        String nextLessonId = lessonService.completeLessonByUser(lessonId);
        return ResponseEntity.ok(nextLessonId);
    }

}
