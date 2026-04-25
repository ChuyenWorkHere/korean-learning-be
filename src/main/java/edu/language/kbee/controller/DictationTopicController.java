package edu.language.kbee.controller;

import edu.language.kbee.payload.DictationTopicDto;
import edu.language.kbee.payload.request.DictationTopicRequest;
import edu.language.kbee.service.DictationTopicService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dictation/topics")
@RequiredArgsConstructor
public class DictationTopicController {

    private final DictationTopicService dictationTopicService;

    @GetMapping
    public ResponseEntity<List<DictationTopicDto>> getAllTopics() {
        return ResponseEntity.ok(dictationTopicService.getAllTopics());
    }

    @GetMapping("/published")
    public ResponseEntity<List<DictationTopicDto>> getPublishedTopics() {
        return ResponseEntity.ok(dictationTopicService.getPublishedTopics());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DictationTopicDto> getTopicById(@PathVariable String id) {
        return ResponseEntity.ok(dictationTopicService.getTopicById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DictationTopicDto> createTopic(@Valid @RequestBody DictationTopicRequest request) {
        return new ResponseEntity<>(dictationTopicService.createTopic(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DictationTopicDto> updateTopic(@PathVariable String id, @Valid @RequestBody DictationTopicRequest request) {
        return ResponseEntity.ok(dictationTopicService.updateTopic(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteTopic(@PathVariable String id) {
        dictationTopicService.deleteTopic(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/toggle-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DictationTopicDto> toggleStatus(@PathVariable String id) {
        return ResponseEntity.ok(dictationTopicService.toggleStatus(id));
    }
}
