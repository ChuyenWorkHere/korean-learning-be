package edu.language.kbee.service.impl;

import edu.language.kbee.exception.ResourceNotFoundException;
import edu.language.kbee.model.Flashcard;
import edu.language.kbee.model.FlashcardDeck;
import edu.language.kbee.payload.FlashcardDto;
import edu.language.kbee.payload.request.FlashcardRequest;
import edu.language.kbee.repository.FlashcardDeckRepository;
import edu.language.kbee.repository.FlashcardRepository;
import edu.language.kbee.service.FlashcardService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FlashcardServiceImpl implements FlashcardService {

    private final FlashcardRepository flashcardRepository;
    private final FlashcardDeckRepository deckRepository;
    private final ModelMapper mapper;

    @Override
    public List<FlashcardDto> getFlashcardsByDeck(String deckId) {
        FlashcardDeck deck = deckRepository.findById(deckId)
                .orElseThrow(() -> new ResourceNotFoundException("deckId", HttpStatus.NOT_FOUND));
        return flashcardRepository.findByDeck(deck).stream()
                .map(card -> mapper.map(card, FlashcardDto.class))
                .collect(Collectors.toList());
    }

    @Override
    public FlashcardDto getFlashcardById(String cardId) {
        Flashcard card = flashcardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("deckId", HttpStatus.NOT_FOUND));
        return mapper.map(card, FlashcardDto.class);
    }

    @Override
    public FlashcardDto addFlashcardToDeck(String deckId, FlashcardRequest request) {
        FlashcardDeck deck = deckRepository.findById(deckId)
                .orElseThrow(() -> new ResourceNotFoundException("deckId", HttpStatus.NOT_FOUND));
                
        Flashcard flashcard = Flashcard.builder()
                .word(request.getWord())
                .meaning(request.getMeaning())
                .exampleSentence(request.getExampleSentence())
                .note(request.getNote())
                .deck(deck)
                .build();
                
        Flashcard savedCard = flashcardRepository.save(flashcard);
        return mapper.map(savedCard, FlashcardDto.class);
    }

    @Override
    public FlashcardDto updateFlashcard(String cardId, FlashcardRequest request) {
        Flashcard card = flashcardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("cardId", HttpStatus.NOT_FOUND));
                
        card.setWord(request.getWord());
        card.setMeaning(request.getMeaning());
        card.setExampleSentence(request.getExampleSentence());
        card.setNote(request.getNote());
        
        Flashcard updatedCard = flashcardRepository.save(card);
        return mapper.map(updatedCard, FlashcardDto.class);
    }

    @Override
    public void deleteFlashcard(String cardId) {
        Flashcard card = flashcardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("cardId", HttpStatus.NOT_FOUND));
        flashcardRepository.delete(card);
    }
}
