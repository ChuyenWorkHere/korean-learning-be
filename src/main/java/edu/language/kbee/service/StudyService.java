package edu.language.kbee.service;

import edu.language.kbee.payload.FlashcardDto;
import edu.language.kbee.payload.request.StudyResultRequest;

import java.util.List;

public interface StudyService {
    List<FlashcardDto> getCardsToReview(String deckId);
    void recordStudyResult(StudyResultRequest request);
}
