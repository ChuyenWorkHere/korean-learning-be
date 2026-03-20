package edu.language.kbee.controller;

import edu.language.kbee.payload.request.SubmissionRequest;
import edu.language.kbee.payload.response.SubmissionResponse;
import edu.language.kbee.service.SubmissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/submissions")
@RequiredArgsConstructor
public class SubmissionController {

    private final SubmissionService submissionService;

    @PostMapping("/writing-submit")
    public ResponseEntity<SubmissionResponse> submitWork(@RequestBody @Valid SubmissionRequest request) {

        SubmissionResponse response = submissionService.submitAndGrade(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{lessonId}/{blockId}")
    public ResponseEntity<SubmissionResponse> getLastSubmission(@PathVariable String lessonId, @PathVariable String blockId) {
        SubmissionResponse response = submissionService.findLastSubmission(lessonId, blockId);
        return ResponseEntity.ok(response);
    }
}