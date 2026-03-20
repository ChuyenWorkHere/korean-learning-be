package edu.language.kbee.service;

import edu.language.kbee.payload.FlashcardDto;
import edu.language.kbee.payload.request.FlashcardRequest;

import java.util.List;

public interface FlashcardService {
    List<FlashcardDto> getFlashcardsByDeck(String deckId);
    FlashcardDto getFlashcardById(String cardId);
    FlashcardDto addFlashcardToDeck(String deckId, FlashcardRequest request);
    FlashcardDto updateFlashcard(String cardId, FlashcardRequest request);
    void deleteFlashcard(String cardId);
}
