package edu.language.kbee.service;

import edu.language.kbee.payload.LessonDto;
import edu.language.kbee.payload.request.LessonCompletionRequest;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

public interface LessonService {
    LessonDto createLesson(LessonDto lessonDto);
    List<LessonDto> getLessonsByUnitId(String unitId);
    LessonDto getLessonById(String lessonId);
    LessonDto updateLesson(String lessonId, LessonDto lessonDto);
    void deleteLesson(String lessonId);
    void reorderLessonsInUnit(String unitId, List<String> lessonIdsInNewOrder);

    LessonDto toggleLessonStatus(String lessonId);

    Map<String, String> completeLessonByUser(String lessonId, LessonCompletionRequest request);
}