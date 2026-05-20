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
import edu.language.kbee.service.UserActivityService;
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
    private final UserActivityService userActivityService;

    @Override
    @Transactional
    public List<FlashcardDto> getCardsToReview(String deckId) {
        User user = authUtil.getLoggedInUser();
        FlashcardDeck deck = deckRepository.findById(deckId)
                .orElseThrow(() -> new ResourceNotFoundException("deckId", HttpStatus.NOT_FOUND));

        List<Flashcard> allCards = flashcardRepository.findByDeck(deck);

        // Thẻ người dùng đang học và đến hạn
        List<FlashcardProgress> dueProgresses = progressRepository.findDueCardsForUserAndDeck(user, deck, LocalDateTime.now());
        List<String> dueCardIds = dueProgresses.stream()
                .map(p -> p.getFlashcard().getCardId())
                .toList();

        // Tìm thẻ mới chưa học
        List<String> cardsWithProgress = progressRepository.findCardIdsWithProgress(user, deck);
        List<Flashcard> newCards = new ArrayList<>();
        for (Flashcard card : allCards) {
            if (!cardsWithProgress.contains(card.getCardId())) {
                newCards.add(card);
            }
        }

        // Hợp card mới + card cần ôn
        List<Flashcard> cardsToStudy = new ArrayList<>();

        for (FlashcardProgress p : dueProgresses) {
            cardsToStudy.add(p.getFlashcard());
            if (cardsToStudy.size() >= 20) break;
        }
        
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
        

        if (score == 0) {
            // Khó nên box thành 1
            progress.setBoxLevel(1);
            progress.setConsecutiveCorrectAnswers(0);
            // ần ôn 5 phutsts
            progress.setNextReviewDate(LocalDateTime.now().plusMinutes(5));
        } else {
            progress.setConsecutiveCorrectAnswers(progress.getConsecutiveCorrectAnswers() + 1);

            if (score == 3) { // Easy - thêm 2 box
                progress.setBoxLevel(Math.min(7, currentBox + 2));
            } else if (score == 2) { // Good - thêm 1 box
                progress.setBoxLevel(Math.min(7, currentBox + 1));
            } else { // Hard - giữ nguyên
                progress.setBoxLevel(currentBox);
            }

            // Tính ngày ôn tập
            // Box 1: 1, Box 2: 3 , Box 3: 7, Box 4: 14, Box 5: 30, Box 6: 60, Box 7: 120
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
        userActivityService.recordActivity(user.getUserId());
    }
}
