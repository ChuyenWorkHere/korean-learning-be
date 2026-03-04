package edu.language.kbee.service;

import edu.language.kbee.payload.LessonDto;
import org.jspecify.annotations.Nullable;

import java.util.List;

public interface LessonService {
    LessonDto createLesson(LessonDto lessonDto);
    List<LessonDto> getLessonsByUnitId(String unitId);
    LessonDto getLessonById(String lessonId);
    LessonDto updateLesson(String lessonId, LessonDto lessonDto);
    void deleteLesson(String lessonId);
    void reorderLessonsInUnit(String unitId, List<String> lessonIdsInNewOrder);

    LessonDto toggleLessonStatus(String lessonId);

    String completeLessonByUser(String lessonId);
}