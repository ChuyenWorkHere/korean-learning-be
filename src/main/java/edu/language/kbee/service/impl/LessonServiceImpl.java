package edu.language.kbee.service.impl;

import edu.language.kbee.enums.CourseStatus;
import edu.language.kbee.enums.LessonStatus;
import edu.language.kbee.enums.LessonType;
import edu.language.kbee.enums.ProgressStatus;
import edu.language.kbee.exception.BadRequestException;
import edu.language.kbee.exception.DataViolationException;
import edu.language.kbee.exception.ResourceNotFoundException;
import edu.language.kbee.model.*;
import edu.language.kbee.payload.CourseDto;
import edu.language.kbee.payload.LessonDto;
import edu.language.kbee.payload.request.LessonCompletionRequest;
import edu.language.kbee.payload.response.LessonCompletionResponse;
import edu.language.kbee.repository.LessonProgressRepository;
import edu.language.kbee.repository.LessonRepository;
import edu.language.kbee.repository.SubmissionRepository;
import edu.language.kbee.repository.UnitRepository;
import edu.language.kbee.service.LessonService;
import edu.language.kbee.service.UserActivityService;
import edu.language.kbee.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.modelmapper.ModelMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LessonServiceImpl implements LessonService {

    private final LessonRepository lessonRepository;
    private final LessonProgressRepository progressRepository;
    private final UnitRepository unitRepository;
    private final ModelMapper modelMapper;
    private final AuthUtil authUtil;
    private final UserActivityService userActivityService;
    private final ObjectMapper objectMapper;
    private final SubmissionRepository submissionRepository;

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

        if (dto.getContent() != null && !dto.getContent().equals(lesson.getContent())) {
            try {
                Set<String> oldBlockIds = extractBlockIds(lesson.getContent());
                Set<String> newBlockIds = extractBlockIds(dto.getContent());
                
                for (String oldId : oldBlockIds) {
                    if (!newBlockIds.contains(oldId)) {
                        if (submissionRepository.existsByLesson_LessonIdAndBlockId(lessonId, oldId)) {
                            throw new BadRequestException("Có học viên nộp bài này, không thể xóa");
                        }
                    }
                }
            } catch (Exception e) {
                if (e instanceof BadRequestException) {
                    throw e;
                }
            }
            lesson.setContent(dto.getContent());
        }
        if (dto.getDurationMinutes() != null) lesson.setDurationMinutes(dto.getDurationMinutes());
        if (dto.getStatus() != null) lesson.setStatus(dto.getStatus());

        Lesson updatedLesson = lessonRepository.save(lesson);
        return mapToDto(updatedLesson);
    }

    private Set<String> extractBlockIds(String contentJson) {
        Set<String> ids = new HashSet<>();
        try {
            if (contentJson == null || contentJson.isBlank()) return ids;
            JsonNode blocks = objectMapper.readTree(contentJson);
            if (blocks.isArray()) {
                for (JsonNode block : blocks) {
                    if (block.has("id")) {
                        ids.add(block.get("id").asText());
                    }
                }
            }
        } catch (Exception ignored) {}
        return ids;
    }

    @Override
    @Transactional
    public void deleteLesson(String lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Lesson not found"));

        if (progressRepository.existsByLesson_LessonId(lessonId)) {
            throw new BadRequestException("Bài học này đã có học viên làm bài, không thể xóa");
        }

        try {
            lessonRepository.delete(lesson);
            lessonRepository.flush();
        } catch (DataIntegrityViolationException | ConstraintViolationException e) {
            throw new DataViolationException("Bài học có bài nộp của học viên. Không thể xóa");
        }
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
    public LessonCompletionResponse completeLessonByUser(String lessonId, LessonCompletionRequest request) {

        User user = authUtil.getLoggedInUser();

        Lesson currentLesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson not found"));

        Unit currentUnit = currentLesson.getUnit();
        String courseId = currentUnit.getCourse().getCourseId();

        int score;
        int correctCount = 0;
        int totalCount = 0;
        Map<String, Boolean> results = new LinkedHashMap<>();

        if (request.getAnswers() != null && !request.getAnswers().isEmpty()) {
            // Backend chấm điểm dựa trên answers gửi lên
            try {
                JsonNode blocks = objectMapper.readTree(currentLesson.getContent());
                for (JsonNode block : blocks) {
                    String blockType = block.path("type").asText();
                    String blockId = block.path("id").asText();

                    if ("quiz".equals(blockType)) {
                        totalCount++;
                        String userAnswer = request.getAnswers().get(blockId);
                        boolean isCorrect = false;

                        if (userAnswer != null && block.has("options")) {
                            for (JsonNode option : block.path("options")) {
                                if (option.path("id").asText().equals(userAnswer) && option.path("isCorrect").asBoolean()) {
                                    isCorrect = true;
                                    break;
                                }
                            }
                        }

                        if (isCorrect) correctCount++;
                        results.put(blockId, isCorrect);

                    } else if ("dictation".equals(blockType)) {
                        String text = block.path("text").asText();
                        // Regex tách chuỗi theo định dạng [từ_cần_điền]
                        String[] parts = text.split("\\[(.*?)\\]", -1);
                        Matcher matcher = Pattern.compile("\\[(.*?)\\]").matcher(text);
                        int blankIndex = 0;

                        while (matcher.find()) {
                            totalCount++;
                            blankIndex++;
                            // Frontend gửi key dạng "blockId-oddIndex", ta tái tạo lại
                            int oddIndex = blankIndex * 2 - 1;
                            String answerKey = blockId + "-" + oddIndex;
                            String expectedWord = matcher.group(1).trim().toLowerCase();
                            String userAnswer = (request.getAnswers().getOrDefault(answerKey, "")).trim().toLowerCase();

                            boolean isCorrect = expectedWord.equals(userAnswer);
                            if (isCorrect) correctCount++;
                            results.put(answerKey, isCorrect);
                        }
                    }
                }
            } catch (Exception e) {
                throw new BadRequestException("Lỗi khi chấm điểm bài học: " + e.getMessage());
            }

            score = totalCount == 0 ? 0 : Math.round((float) correctCount / totalCount * 100);
        } else {
            // Fallback: Frontend gửi score trực tiếp (dùng cho Writing/Translation)
            score = request.getScore() != null ? request.getScore() : 0;
        }

        // --- UPDATE PROGRESS ---
        LessonProgress currentProgress = progressRepository
                .findByUser_UserIdAndLesson_LessonId(user.getUserId(), lessonId)
                .orElse(LessonProgress.builder()
                        .lesson(currentLesson)
                        .user(user)
                        .status(ProgressStatus.NOT_STARTED)
                        .build());

        ProgressStatus newStatus = score >= 80 ? ProgressStatus.COMPLETED : ProgressStatus.FAILED;

        currentProgress.setScore(score);
        currentProgress.setStatus(newStatus);
        currentProgress.setCompletedAt(LocalDateTime.now());
        progressRepository.save(currentProgress);

        // --- FIND NEXT LESSON ---
        String nextLessonId = "0";

        if (newStatus == ProgressStatus.COMPLETED) {
            List<Lesson> nextLessons = lessonRepository.findNextLessonToLearn(
                    courseId,
                    currentUnit.getOrderIndex(),
                    currentLesson.getOrderIndex(),
                    LessonStatus.PUBLISHED,
                    PageRequest.of(0, 1)
            );

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
        }

        userActivityService.recordActivity(user.getUserId());

        return LessonCompletionResponse.builder()
                .courseId(courseId)
                .nextLessonId(nextLessonId)
                .message("Lesson progress tracked successfully")
                .score(score)
                .correctCount(correctCount)
                .totalCount(totalCount)
                .results(results)
                .build();
    }

    @Override
    public Long countCompletedLessons() {
        User user = authUtil.getLoggedInUser();
        return progressRepository.countAllCompletedLessonsByUserId(user.getUserId());
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
                .courseId(lesson.getUnit().getCourse().getCourseId())
                .courseTitle(lesson.getUnit().getCourse().getTitle())
                .build();
    }
}
