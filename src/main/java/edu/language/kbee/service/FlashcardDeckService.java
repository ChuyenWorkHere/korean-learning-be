package edu.language.kbee.service;

import edu.language.kbee.payload.FlashcardDeckDto;
import edu.language.kbee.payload.request.DeckRequest;

import java.util.List;

public interface FlashcardDeckService {
    List<FlashcardDeckDto> getMyDecks();
    List<FlashcardDeckDto> getPublicDecks();
    FlashcardDeckDto getDeckById(String deckId);
    FlashcardDeckDto createDeck(DeckRequest request);
    FlashcardDeckDto updateDeck(String deckId, DeckRequest request);
    void deleteDeck(String deckId);
}
