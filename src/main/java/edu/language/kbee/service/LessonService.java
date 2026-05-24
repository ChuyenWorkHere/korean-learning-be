package edu.language.kbee.service;

import edu.language.kbee.payload.LessonDto;
import edu.language.kbee.payload.request.LessonCompletionRequest;
import edu.language.kbee.payload.response.LessonCompletionResponse;

import java.util.List;
import java.util.Map;

public interface LessonService {
    LessonDto createLesson(LessonDto lessonDto);
    List<LessonDto> getLessonsByUnitId(String unitId);
    LessonDto getLessonById(String lessonId);
    LessonDto updateLesson(String lessonId, LessonDto lessonDto);
    LessonDto updateLessonBlocks(String lessonId, List<Map<String, Object>> blocks);
    void deleteLesson(String lessonId);
    void reorderLessonsInUnit(String unitId, List<String> lessonIdsInNewOrder);

    LessonDto toggleLessonStatus(String lessonId);

    LessonCompletionResponse completeLessonByUser(String lessonId, LessonCompletionRequest request);
    Long countCompletedLessons();
}
