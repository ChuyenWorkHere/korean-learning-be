package edu.language.kbee.service.impl;

import edu.language.kbee.enums.CourseStatus;
import edu.language.kbee.enums.LessonStatus;
import edu.language.kbee.enums.ProgressStatus;
import edu.language.kbee.exception.BadRequestException;
import edu.language.kbee.exception.ResourceNotFoundException;
import edu.language.kbee.model.*;
import edu.language.kbee.payload.CourseDto;
import edu.language.kbee.payload.LessonDto;
import edu.language.kbee.repository.LessonProgressRepository;
import edu.language.kbee.repository.LessonRepository;
import edu.language.kbee.repository.UnitRepository;
import edu.language.kbee.service.LessonService;
import edu.language.kbee.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LessonServiceImpl implements LessonService {

    private final LessonRepository lessonRepository;
    private final LessonProgressRepository progressRepository;
    private final UnitRepository unitRepository;
    private final ModelMapper modelMapper;
    private final AuthUtil authUtil;

    @Override
    @Transactional
    public LessonDto createLesson(LessonDto dto) {
        Unit unit = unitRepository.findById(dto.getUnitId())
                .orElseThrow(() -> new RuntimeException("Unit not found with id: " + dto.getUnitId()));

        int nextOrderIndex = lessonRepository.countByUnit_UnitId(unit.getUnitId());

        Lesson lesson = Lesson.builder()
                .title(dto.getTitle())
                .type(dto.getType())
                .content(dto.getContent() != null ? dto.getContent() : "[]")
                .orderIndex(nextOrderIndex)
                .durationMinutes(dto.getDurationMinutes() != null ? dto.getDurationMinutes() : 0)
                .status(dto.getStatus() != null ? dto.getStatus() : LessonStatus.DRAFT)
                .unit(unit)
                .build();

        Lesson savedLesson = lessonRepository.save(lesson);
        return mapToDto(savedLesson);
    }

    @Override
    public List<LessonDto> getLessonsByUnitId(String unitId) {
        List<Lesson> lessons = lessonRepository.findByUnit_UnitIdOrderByOrderIndexAsc(unitId);
        return lessons.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    public LessonDto getLessonById(String lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Lesson not found"));
        return mapToDto(lesson);
    }

    @Override
    @Transactional
    public LessonDto updateLesson(String lessonId, LessonDto dto) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Lesson not found"));

        lesson.setTitle(dto.getTitle());
        if (dto.getContent() != null) lesson.setContent(dto.getContent());
        if (dto.getDurationMinutes() != null) lesson.setDurationMinutes(dto.getDurationMinutes());
        if (dto.getStatus() != null) lesson.setStatus(dto.getStatus());

        Lesson updatedLesson = lessonRepository.save(lesson);
        return mapToDto(updatedLesson);
    }

    @Override
    @Transactional
    public void deleteLesson(String lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Lesson not found"));
        lessonRepository.delete(lesson);

    }

    @Override
    public void reorderLessonsInUnit(String unitId, List<String> lessonIdsInNewOrder) {
        List<Lesson> existingLessons = lessonRepository.findByUnit_UnitIdOrderByOrderIndexAsc(unitId);

        Map<String, Lesson> lessonMap = existingLessons.stream()
                .collect(Collectors.toMap(Lesson::getLessonId, Function.identity()));

        for (int i = 0; i < lessonIdsInNewOrder.size(); i++) {
            String id = lessonIdsInNewOrder.get(i);
            if (lessonMap.containsKey(id)) {
                Lesson lesson = lessonMap.get(id);
                lesson.setOrderIndex(i);
            }
        }

        lessonRepository.saveAll(existingLessons);
    }

    @Override
    public LessonDto toggleLessonStatus(String lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));

        if (lesson.getStatus() == LessonStatus.PUBLISHED) {
            lesson.setStatus(LessonStatus.DRAFT);
        } else if (lesson.getStatus() == LessonStatus.DRAFT) {
            lesson.setStatus(LessonStatus.PUBLISHED);
        }

        Lesson updatedLesson = lessonRepository.save(lesson);
        return modelMapper.map(updatedLesson, LessonDto.class);
    }

    @Override
    public String completeLessonByUser(String lessonId) {

        User user = authUtil.getLoggedInUser();

        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson not found"));

        if(progressRepository.existsByUser_UserIdAndLesson_LessonIdAndStatus(user.getUserId(), lessonId, ProgressStatus.COMPLETED)) {
            throw new BadRequestException("Lesson already completed");
        }

        LessonProgress progress = LessonProgress.builder()
                .lesson(lesson)
                .completedAt(LocalDateTime.now())
                .status(ProgressStatus.COMPLETED)
                .user(user)
                .build();

        progressRepository.save(progress);

        Optional<Lesson> nextLesson = lessonRepository.findByUnit_UnitIdAndOrderIndex(lesson.getUnit().getUnitId(), lesson.getOrderIndex() + 1);
        if(nextLesson.isPresent()) {
            return nextLesson.get().getLessonId();
        }

        return "0";
    }

    private LessonDto mapToDto(Lesson lesson) {
        return LessonDto.builder()
                .lessonId(lesson.getLessonId())
                .title(lesson.getTitle())
                .type(lesson.getType())
                .content(lesson.getContent())
                .level(lesson.getUnit().getCourse().getLevel())
                .orderIndex(lesson.getOrderIndex())
                .durationMinutes(lesson.getDurationMinutes())
                .status(lesson.getStatus())
                .unitId(lesson.getUnit().getUnitId())
                .build();
    }
}
