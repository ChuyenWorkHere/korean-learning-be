package edu.language.kbee.controller;

import edu.language.kbee.payload.FlashcardDto;
import edu.language.kbee.payload.request.FlashcardRequest;
import edu.language.kbee.service.FlashcardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/flashcards")
@RequiredArgsConstructor
public class FlashcardController {

    private final FlashcardService flashcardService;

    @GetMapping("/deck/{deckId}")
    public ResponseEntity<List<FlashcardDto>> getFlashcardsByDeck(@PathVariable String deckId) {
        return ResponseEntity.ok(flashcardService.getFlashcardsByDeck(deckId));
    }

    @GetMapping("/{cardId}")
    public ResponseEntity<FlashcardDto> getFlashcardById(@PathVariable String cardId) {
        return ResponseEntity.ok(flashcardService.getFlashcardById(cardId));
    }

    @PostMapping("/deck/{deckId}")
    public ResponseEntity<FlashcardDto> addFlashcardToDeck(@PathVariable String deckId, @Valid @RequestBody FlashcardRequest request) {
        return new ResponseEntity<>(flashcardService.addFlashcardToDeck(deckId, request), HttpStatus.CREATED);
    }

    @PutMapping("/{cardId}")
    public ResponseEntity<FlashcardDto> updateFlashcard(@PathVariable String cardId, @Valid @RequestBody FlashcardRequest request) {
        return ResponseEntity.ok(flashcardService.updateFlashcard(cardId, request));
    }

    @DeleteMapping("/{cardId}")
    public ResponseEntity<String> deleteFlashcard(@PathVariable String cardId) {
        flashcardService.deleteFlashcard(cardId);
        return ResponseEntity.ok("Flashcard deleted successfully");
    }
}
