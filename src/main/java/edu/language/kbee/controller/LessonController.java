package edu.language.kbee.controller;

import edu.language.kbee.payload.CourseDto;
import edu.language.kbee.payload.LessonDto;
import edu.language.kbee.service.LessonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class LessonController {

    private final LessonService lessonService;

    @GetMapping("/units/{unitId}/lessons")
    public ResponseEntity<List<LessonDto>> getLessonsByUnit(@PathVariable String unitId) {
        return ResponseEntity.ok(lessonService.getLessonsByUnitId(unitId));
    }


    @GetMapping("/lessons/{lessonId}")
    public ResponseEntity<LessonDto> getLessonDetail(@PathVariable String lessonId) {
        return ResponseEntity.ok(lessonService.getLessonById(lessonId));
    }




    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/lessons")
    public ResponseEntity<LessonDto> createLesson(@Valid @RequestBody LessonDto lessonDto) {
        LessonDto createdLesson = lessonService.createLesson(lessonDto);
        return new ResponseEntity<>(createdLesson, HttpStatus.CREATED);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/lessons/{lessonId}")
    public ResponseEntity<LessonDto> updateLesson(@PathVariable String lessonId,
                                                  @Valid @RequestBody LessonDto lessonDto) {
        return ResponseEntity.ok(lessonService.updateLesson(lessonId, lessonDto));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/lessons/{lessonId}")
    public ResponseEntity<String> deleteLesson(@PathVariable String lessonId) {
        lessonService.deleteLesson(lessonId);
        return ResponseEntity.ok("Lesson deleted successfully!");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/units/{unitId}/lessons/reorder")
    public ResponseEntity<String> reorderLessons(@PathVariable String unitId,
                                               @RequestBody List<String> lessonIds) {
        lessonService.reorderLessonsInUnit(unitId, lessonIds);
        return ResponseEntity.ok("Lessons reordered successfully!");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/lessons/{lessonId}/status")
    public ResponseEntity<?> updateLessonStatus(@PathVariable(name = "lessonId") String lessonId) {
        LessonDto updatedLesson = lessonService.toggleLessonStatus(lessonId);
        return ResponseEntity.ok(updatedLesson);
    }
}
