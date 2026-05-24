package edu.language.kbee.service;

import edu.language.kbee.payload.FlashcardDto;
import edu.language.kbee.payload.request.StudyResultRequest;
import edu.language.kbee.payload.response.StudyResultResponse;

import java.util.List;

public interface StudyService {
    List<FlashcardDto> getCardsToReview(String deckId);
    StudyResultResponse recordStudyResult(StudyResultRequest request);
}
