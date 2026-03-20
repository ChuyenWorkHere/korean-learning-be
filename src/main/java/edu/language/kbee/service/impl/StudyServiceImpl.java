package edu.language.kbee.service.impl;

import edu.language.kbee.exception.ResourceNotFoundException;
import edu.language.kbee.model.Flashcard;
import edu.language.kbee.model.FlashcardDeck;
import edu.language.kbee.model.FlashcardProgress;
import edu.language.kbee.model.User;
import edu.language.kbee.payload.FlashcardDto;
import edu.language.kbee.payload.request.StudyResultRequest;
import edu.language.kbee.repository.FlashcardDeckRepository;
import edu.language.kbee.repository.FlashcardProgressRepository;
import edu.language.kbee.repository.FlashcardRepository;
import edu.language.kbee.service.StudyService;
import edu.language.kbee.service.UserService;
import edu.language.kbee.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudyServiceImpl implements StudyService {

    private final FlashcardRepository flashcardRepository;
    private final FlashcardDeckRepository deckRepository;
    private final FlashcardProgressRepository progressRepository;
    private final AuthUtil authUtil;
    private final ModelMapper mapper;
    private final UserService userService;

    @Override
    @Transactional
    public List<FlashcardDto> getCardsToReview(String deckId) {
        User user = authUtil.getLoggedInUser();
        FlashcardDeck deck = deckRepository.findById(deckId)
                .orElseThrow(() -> new ResourceNotFoundException("deckId", HttpStatus.NOT_FOUND));

        // 1. Get all cards in the deck
        List<Flashcard> allCards = flashcardRepository.findByDeck(deck);

        // 2. Get cards the user has already started studying and are due now
        List<FlashcardProgress> dueProgresses = progressRepository.findDueCardsForUserAndDeck(user, deck, LocalDateTime.now());
        List<String> dueCardIds = dueProgresses.stream()
                .map(p -> p.getFlashcard().getCardId())
                .collect(Collectors.toList());

        // 3. Find completely new cards (cards without any progress record)
        List<Flashcard> newCards = new ArrayList<>();
        for (Flashcard card : allCards) {
            // If it's not in the due list, check if it has a progress record at all
            if (!dueCardIds.contains(card.getCardId())) {
                boolean hasProgress = progressRepository.findByUserAndFlashcard(user, card).isPresent();
                if (!hasProgress) {
                    newCards.add(card);
                }
            }
        }

        // 4. Combine Due cards + New cards (Limit to maximum 20 cards per study session for performance)
        List<Flashcard> cardsToStudy = new ArrayList<>();
        
        // Add due cards first
        for (FlashcardProgress p : dueProgresses) {
            cardsToStudy.add(p.getFlashcard());
            if (cardsToStudy.size() >= 20) break;
        }
        
        // Add new cards if there's still room
        for (Flashcard card : newCards) {
            if (cardsToStudy.size() >= 20) break;
            cardsToStudy.add(card);
        }

        return cardsToStudy.stream()
                .map(card -> mapper.map(card, FlashcardDto.class))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void recordStudyResult(StudyResultRequest request) {
        User user = authUtil.getLoggedInUser();
        Flashcard card = flashcardRepository.findById(request.getCardId())
                .orElseThrow(() -> new ResourceNotFoundException("deckId", HttpStatus.NOT_FOUND));

        FlashcardProgress progress = progressRepository.findByUserAndFlashcard(user, card)
                .orElse(FlashcardProgress.builder()
                        .user(user)
                        .flashcard(card)
                        .boxLevel(1)
                        .consecutiveCorrectAnswers(0)
                        .nextReviewDate(LocalDateTime.now())
                        .build());

        int score = request.getScore(); // 0=Forgot, 1=Hard, 2=Good, 3=Easy
        int currentBox = progress.getBoxLevel();
        
        // Leitner System Implementation
        if (score == 0) {
            // Forgot: Reset to Box 1
            progress.setBoxLevel(1);
            progress.setConsecutiveCorrectAnswers(0);
            // Review again in 5 minutes
            progress.setNextReviewDate(LocalDateTime.now().plusMinutes(5));
        } else {
            progress.setConsecutiveCorrectAnswers(progress.getConsecutiveCorrectAnswers() + 1);
            
            // Advance Box based on score
            if (score == 3) { // Easy - skips a box
                progress.setBoxLevel(Math.min(7, currentBox + 2));
            } else if (score == 2) { // Good - advances 1 box
                progress.setBoxLevel(Math.min(7, currentBox + 1));
            } else { // Hard - stays in same box
                progress.setBoxLevel(currentBox);
            }

            // Calculate next review based on Box Level
            // Box 1: 1 day, Box 2: 3 days, Box 3: 7 days, Box 4: 14 days, Box 5: 30 days, Box 6: 60 days, Box 7: 120 days
            int daysToAdd = 1;
            switch(progress.getBoxLevel()) {
                case 1: daysToAdd = 1; break;
                case 2: daysToAdd = 3; break;
                case 3: daysToAdd = 7; break;
                case 4: daysToAdd = 14; break;
                case 5: daysToAdd = 30; break;
                case 6: daysToAdd = 60; break;
                case 7: daysToAdd = 120; break;
            }
            
            progress.setNextReviewDate(LocalDateTime.now().plusDays(daysToAdd));
        }

        progressRepository.save(progress);
        userService.updateUserStreak(user.getUserId());
    }
}
