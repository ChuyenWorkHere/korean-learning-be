package edu.language.kbee.service;

import edu.language.kbee.payload.DictationLessonDto;
import edu.language.kbee.payload.request.DictationLessonRequest;

import java.util.List;

public interface DictationLessonService {
    List<DictationLessonDto> getLessonsByTopic(String topicId);
    DictationLessonDto getLessonById(String lessonId);
    DictationLessonDto createLesson(String topicId, DictationLessonRequest request);
    DictationLessonDto updateLesson(String lessonId, DictationLessonRequest request);
    void deleteLesson(String lessonId);
    DictationLessonDto toggleStatus(String lessonId);
}
