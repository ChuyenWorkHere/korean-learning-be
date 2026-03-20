package edu.language.kbee.controller;

import edu.language.kbee.payload.FlashcardDto;
import edu.language.kbee.payload.request.StudyResultRequest;
import edu.language.kbee.service.StudyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/study")
@RequiredArgsConstructor
public class StudyController {

    private final StudyService studyService;

    @GetMapping("/{deckId}")
    public ResponseEntity<List<FlashcardDto>> getCardsToReview(@PathVariable String deckId) {
        return ResponseEntity.ok(studyService.getCardsToReview(deckId));
    }

    @PostMapping("/result")
    public ResponseEntity<String> recordStudyResult(@Valid @RequestBody StudyResultRequest request) {
        studyService.recordStudyResult(request);
        return ResponseEntity.ok("Study result recorded successfully");
    }
}
