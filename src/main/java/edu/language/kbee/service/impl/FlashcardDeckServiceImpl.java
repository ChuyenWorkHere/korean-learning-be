package edu.language.kbee.service.impl;

import edu.language.kbee.enums.RoleName;
import edu.language.kbee.exception.ResourceNotFoundException;
import edu.language.kbee.exception.UnauthorizedException;
import edu.language.kbee.model.FlashcardDeck;
import edu.language.kbee.model.User;
import edu.language.kbee.payload.FlashcardDeckDto;
import edu.language.kbee.payload.request.DeckRequest;
import edu.language.kbee.repository.FlashcardDeckRepository;
import edu.language.kbee.repository.FlashcardProgressRepository;
import edu.language.kbee.service.FlashcardDeckService;
import edu.language.kbee.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FlashcardDeckServiceImpl implements FlashcardDeckService {

    private final FlashcardDeckRepository deckRepository;
    private final FlashcardProgressRepository progressRepository;
    private final AuthUtil authUtil;
    private final ModelMapper mapper;

    @Override
    public List<FlashcardDeckDto> getMyDecks() {
        User user = authUtil.getLoggedInUser();
        List<FlashcardDeck> decks = deckRepository.findByUser(user);
        return decks.stream()
                .map(deck -> {
                    FlashcardDeckDto dto = mapper.map(deck, FlashcardDeckDto.class);
                    Long learnedCount = progressRepository.countByUserAndFlashcard_Deck(user, deck);
                    dto.setLearnedCount(learnedCount != null ? learnedCount : 0L);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<FlashcardDeckDto> getPublicDecks() {
        // Technically public decks might not have a progress for the CURRENT user unless they studied it.
        // We can still try to resolve it if they are logged in.
        User user = null;
        try {
            user = authUtil.getLoggedInUser();
        } catch (Exception e) {
            // Ignore if no user is logged in
        }

        final User finalUser = user;
        List<FlashcardDeck> decks = deckRepository.findByIsPublicTrue();
        return decks.stream()
                .map(deck -> {
                    FlashcardDeckDto dto = mapper.map(deck, FlashcardDeckDto.class);
                    if (finalUser != null) {
                        Long learnedCount = progressRepository.countByUserAndFlashcard_Deck(finalUser, deck);
                        dto.setLearnedCount(learnedCount != null ? learnedCount : 0L);
                    } else {
                        dto.setLearnedCount(0L);
                    }
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public FlashcardDeckDto getDeckById(String deckId) {
        FlashcardDeck deck = deckRepository.findById(deckId)
                .orElseThrow(() -> new ResourceNotFoundException("deckId", HttpStatus.NOT_FOUND));
        return mapper.map(deck, FlashcardDeckDto.class);
    }

    @Override
    public FlashcardDeckDto createDeck(DeckRequest request) {
        User user = authUtil.getLoggedInUser();
        
        FlashcardDeck deck = FlashcardDeck.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .level(request.getLevel() != null ? request.getLevel() : "Beginner")
                .isPublic(request.isPublic())
                .user(user)
                .build();
                
        FlashcardDeck savedDeck = deckRepository.save(deck);
        return mapper.map(savedDeck, FlashcardDeckDto.class);
    }

    @Override
    public FlashcardDeckDto updateDeck(String deckId, DeckRequest request) {
        FlashcardDeck deck = deckRepository.findById(deckId)
                .orElseThrow(() -> new ResourceNotFoundException("deckId", HttpStatus.NOT_FOUND));

        User user = null;
        try {
            user = authUtil.getLoggedInUser();
        } catch (Exception e) {
            // Ignore if no user is logged in
        }
        boolean isAdmin = user != null && user.getRoles().stream().anyMatch(role -> role.getRoleName().equals(RoleName.ADMIN));
        if(user != null  && deck.getUser().getUserId().equals(user.getUserId()) || !isAdmin) {
            throw new UnauthorizedException("Unauthorized", HttpStatus.UNAUTHORIZED);
        }

        deck.setTitle(request.getTitle());
        deck.setDescription(request.getDescription());
        deck.setLevel(request.getLevel() != null ? request.getLevel() : deck.getLevel());
        deck.setPublic(request.isPublic());
        
        FlashcardDeck updatedDeck = deckRepository.save(deck);
        return mapper.map(updatedDeck, FlashcardDeckDto.class);
    }

    @Override
    public void deleteDeck(String deckId) {
        FlashcardDeck deck = deckRepository.findById(deckId)
                .orElseThrow(() -> new ResourceNotFoundException("deckId", HttpStatus.NOT_FOUND));
        deckRepository.delete(deck);
    }
}
