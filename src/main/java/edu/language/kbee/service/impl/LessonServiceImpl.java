package edu.language.kbee.service.impl;

import edu.language.kbee.enums.CourseStatus;
import edu.language.kbee.enums.LessonStatus;
import edu.language.kbee.enums.LessonType;
import edu.language.kbee.enums.ProgressStatus;
import edu.language.kbee.exception.BadRequestException;
import edu.language.kbee.exception.ResourceNotFoundException;
import edu.language.kbee.model.*;
import edu.language.kbee.payload.CourseDto;
import edu.language.kbee.payload.LessonDto;
import edu.language.kbee.payload.request.LessonCompletionRequest;
import edu.language.kbee.repository.LessonProgressRepository;
import edu.language.kbee.repository.LessonRepository;
import edu.language.kbee.repository.UnitRepository;
import edu.language.kbee.service.LessonService;
import edu.language.kbee.service.UserService;
import edu.language.kbee.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.PageRequest;
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
    private final UserService userService;

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
                .orElseThrow(() -> new ResourceNotFoundException("Lesson not found"));

        if (authUtil.isAdmin()) {
            return mapToDto(lesson);
        }

        if (lesson.getStatus() != LessonStatus.PUBLISHED) {
            throw new ResourceNotFoundException("Lesson not found or not published");
        }

        User user = authUtil.getLoggedInUser();
        String courseId = lesson.getUnit().getCourse().getCourseId();

        List<Lesson> courseLessons = lessonRepository.findByUnit_Course_CourseIdOrderByUnit_OrderIndexAscOrderIndexAsc(courseId)
                .stream()
                .filter(l -> l.getStatus() == LessonStatus.PUBLISHED)
                .toList();

        List<LessonProgress> progresses = progressRepository.findAllByUserIdAndCourseId(user.getUserId(), courseId);
        Map<String, ProgressStatus> progressMap = progresses.stream()
                .collect(Collectors.toMap(p -> p.getLesson().getLessonId(), LessonProgress::getStatus));

        boolean canAccess = false;
        boolean foundFirstUncompleted = false;

        for (Lesson l : courseLessons) {
            ProgressStatus status = progressMap.getOrDefault(l.getLessonId(), ProgressStatus.NOT_STARTED);

            if (l.getLessonId().equals(lessonId)) {
                if (status == ProgressStatus.COMPLETED || !foundFirstUncompleted) {
                    canAccess = true;
                }
                break;
            }

            if (status != ProgressStatus.COMPLETED) {
                foundFirstUncompleted = true;
            }
        }

        if (!canAccess) {
            throw new BadRequestException("This lesson is locked. Please complete the previous lessons first.");
        }

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
    @Transactional
    public Map<String, String> completeLessonByUser(String lessonId, LessonCompletionRequest request) {

        User user = authUtil.getLoggedInUser();

        Lesson currentLesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson not found"));

        Unit currentUnit = currentLesson.getUnit();
        String courseId = currentUnit.getCourse().getCourseId();

        LessonProgress currentProgress = progressRepository
                .findByUser_UserIdAndLesson_LessonId(user.getUserId(), lessonId)
                .orElse(LessonProgress.builder()
                        .lesson(currentLesson)
                        .user(user)
                        .status(ProgressStatus.NOT_STARTED)
                        .build());

        // Update progress if not yet completed OR always update score/status if it's a new attempt
        Integer score = request.getScore() != null ? request.getScore() : 0;
        ProgressStatus newStatus = score >= 80 ? ProgressStatus.COMPLETED : ProgressStatus.FAILED;

        if(currentLesson.getType() == LessonType.WRITING) {
            newStatus = ProgressStatus.COMPLETED;
        }

        currentProgress.setScore(score);
        currentProgress.setStatus(newStatus);
        currentProgress.setCompletedAt(LocalDateTime.now());
        progressRepository.save(currentProgress);

        // Logic to determine the NEXT lesson regardless of whether current was already completed
        List<Lesson> nextLessons = lessonRepository.findNextLessonToLearn(
                courseId,
                currentUnit.getOrderIndex(),
                currentLesson.getOrderIndex(),
                LessonStatus.PUBLISHED,
                PageRequest.of(0, 1)
        );

        String nextLessonId = "0"; // Default to end of unit (0)

        if (!nextLessons.isEmpty()) {
            Lesson nextLesson = nextLessons.get(0);
            nextLessonId = nextLesson.getLessonId();

            boolean nextProgressExists = progressRepository
                    .existsByUser_UserIdAndLesson_LessonId(user.getUserId(), nextLessonId);

            if (!nextProgressExists) {
                LessonProgress nextLessonProgress = LessonProgress.builder()
                        .lesson(nextLesson)
                        .status(ProgressStatus.NOT_STARTED)
                        .user(user)
                        .build();

                progressRepository.save(nextLessonProgress);
            }
        }

        userService.updateUserStreak(user.getUserId());

        return Map.of(
            "courseId", courseId,
            "nextLessonId", nextLessonId,
            "message", "Lesson progress tracked successfully"
        );
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
