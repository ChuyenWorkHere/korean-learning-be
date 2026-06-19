package edu.language.kbee.service.impl;

import edu.language.kbee.enums.XpSourceType;
import edu.language.kbee.exception.BadRequestException;
import edu.language.kbee.exception.ResourceNotFoundException;
import edu.language.kbee.model.Flashcard;
import edu.language.kbee.model.FlashcardDeck;
import edu.language.kbee.model.FlashcardProgress;
import edu.language.kbee.model.User;
import edu.language.kbee.payload.FlashcardDto;
import edu.language.kbee.payload.request.StudyBatchResultRequest;
import edu.language.kbee.payload.request.StudyResultRequest;
import edu.language.kbee.payload.response.StudyResultResponse;
import edu.language.kbee.repository.FlashcardDeckRepository;
import edu.language.kbee.repository.FlashcardProgressRepository;
import edu.language.kbee.repository.FlashcardRepository;
import edu.language.kbee.service.StudyService;
import edu.language.kbee.service.UserActivityService;
import edu.language.kbee.service.XpService;
import edu.language.kbee.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
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
    private final XpService xpService;

    @Override
    @Transactional
    public List<FlashcardDto> getCardsToReview(String deckId) {
        User user = authUtil.getLoggedInUser();
        FlashcardDeck deck = deckRepository.findByDeckIdAndDeletedAtIsNull(deckId)
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
    public StudyResultResponse recordStudyResult(StudyResultRequest request) {
        Flashcard card = flashcardRepository.findById(request.getCardId())
                .orElseThrow(() -> new ResourceNotFoundException("cardId", HttpStatus.NOT_FOUND));

        return recordStudyResults(StudyBatchResultRequest.builder()
                .deckId(card.getDeck().getDeckId())
                .results(List.of(StudyBatchResultRequest.Item.builder()
                        .cardId(request.getCardId())
                        .score(request.getScore())
                        .build()))
                .build());
    }

    @Override
    @Transactional
    public StudyResultResponse recordStudyResults(StudyBatchResultRequest request) {
        User user = authUtil.getLoggedInUser();
        FlashcardDeck deck = deckRepository.findByDeckIdAndDeletedAtIsNull(request.getDeckId())
                .orElseThrow(() -> new ResourceNotFoundException("deckId", HttpStatus.NOT_FOUND));

        Map<String, Integer> scoreByCardId = new LinkedHashMap<>();
        for (StudyBatchResultRequest.Item result : request.getResults()) {
            scoreByCardId.put(result.getCardId(), result.getScore());
        }

        if (scoreByCardId.isEmpty()) {
            throw new BadRequestException("Study results are required");
        }

        List<String> cardIds = new ArrayList<>(scoreByCardId.keySet());
        List<Flashcard> cards = flashcardRepository.findByDeckAndCardIds(deck, cardIds);
        if (cards.size() != cardIds.size()) {
            throw new ResourceNotFoundException("deckId", HttpStatus.NOT_FOUND);
        }

        Map<String, FlashcardProgress> progressByCardId = progressRepository
                .findByUserAndFlashcardCardIds(user, cardIds)
                .stream()
                .collect(Collectors.toMap(p -> p.getFlashcard().getCardId(), p -> p));

        List<FlashcardProgress> progressesToSave = new ArrayList<>();
        int newLearnedCount = 0;

        for (Flashcard card : cards) {
            int score = scoreByCardId.get(card.getCardId());
            FlashcardProgress progress = progressByCardId.get(card.getCardId());
            boolean isNewCardProgress = progress == null;

            if (progress == null) {
                progress = FlashcardProgress.builder()
                        .user(user)
                        .flashcard(card)
                        .boxLevel(1)
                        .consecutiveCorrectAnswers(0)
                        .nextReviewDate(LocalDateTime.now())
                        .build();
            }

            applyStudyScore(progress, score);
            progressesToSave.add(progress);

            if (isNewCardProgress && score > 0) {
                newLearnedCount++;
            }
        }

        progressRepository.saveAll(progressesToSave);
        userActivityService.recordActivity(user.getUserId());

        int awardedXp = newLearnedCount * 5;
        var xpAward = awardedXp > 0
                ? xpService.awardXp(
                        user.getUserId(),
                        XpSourceType.VOCABULARY_LEARNED,
                        "vocab-batch-" + UUID.randomUUID(),
                        awardedXp,
                        "Học " + newLearnedCount + " từ vựng mới"
                )
                : xpService.awardXp(user.getUserId(), XpSourceType.VOCABULARY_LEARNED, null, 0, "Không cộng XP");

        return StudyResultResponse.builder()
                .message("Study result recorded successfully")
                .xpAward(xpAward)
                .build();
    }

    private void applyStudyScore(FlashcardProgress progress, int score) {
        int currentBox = progress.getBoxLevel();

        if (score == 0) {
            progress.setBoxLevel(1);
            progress.setConsecutiveCorrectAnswers(0);
            progress.setNextReviewDate(LocalDateTime.now().plusMinutes(5));
            return;
        }

        progress.setConsecutiveCorrectAnswers(progress.getConsecutiveCorrectAnswers() + 1);

        if (score == 3) {
            progress.setBoxLevel(Math.min(7, currentBox + 2));
        } else if (score == 2) {
            progress.setBoxLevel(Math.min(7, currentBox + 1));
        } else {
            progress.setBoxLevel(currentBox);
        }

        int daysToAdd = switch (progress.getBoxLevel()) {
            case 2 -> 3;
            case 3 -> 7;
            case 4 -> 14;
            case 5 -> 30;
            case 6 -> 60;
            case 7 -> 120;
            default -> 1;
        };

        progress.setNextReviewDate(LocalDateTime.now().plusDays(daysToAdd));
    }
}
