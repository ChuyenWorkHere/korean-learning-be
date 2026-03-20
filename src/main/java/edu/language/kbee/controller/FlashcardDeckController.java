package edu.language.kbee.controller;

import edu.language.kbee.payload.FlashcardDeckDto;
import edu.language.kbee.payload.request.DeckRequest;
import edu.language.kbee.service.FlashcardDeckService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/decks")
@RequiredArgsConstructor
public class FlashcardDeckController {

    private final FlashcardDeckService deckService;

    @GetMapping("/my-decks")
    public ResponseEntity<List<FlashcardDeckDto>> getMyDecks() {
        return ResponseEntity.ok(deckService.getMyDecks());
    }

    @GetMapping("/public")
    public ResponseEntity<List<FlashcardDeckDto>> getPublicDecks() {
        return ResponseEntity.ok(deckService.getPublicDecks());
    }

    @GetMapping("/{deckId}")
    public ResponseEntity<FlashcardDeckDto> getDeckById(@PathVariable String deckId) {
        return ResponseEntity.ok(deckService.getDeckById(deckId));
    }

    @PostMapping
    public ResponseEntity<FlashcardDeckDto> createDeck(@Valid @RequestBody DeckRequest request) {
        return new ResponseEntity<>(deckService.createDeck(request), HttpStatus.CREATED);
    }

    @PutMapping("/{deckId}")
    public ResponseEntity<FlashcardDeckDto> updateDeck(@PathVariable String deckId, @Valid @RequestBody DeckRequest request) {
        return ResponseEntity.ok(deckService.updateDeck(deckId, request));
    }

    @DeleteMapping("/{deckId}")
    public ResponseEntity<String> deleteDeck(@PathVariable String deckId) {
        deckService.deleteDeck(deckId);
        return ResponseEntity.ok("Flashcard deck deleted successfully");
    }
}
