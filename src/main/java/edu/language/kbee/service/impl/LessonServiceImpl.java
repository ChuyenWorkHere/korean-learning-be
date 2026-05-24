package edu.language.kbee.service.impl;

import edu.language.kbee.enums.LessonBlockType;
import edu.language.kbee.enums.LessonStatus;
import edu.language.kbee.enums.ProgressStatus;
import edu.language.kbee.enums.SubmissionType;
import edu.language.kbee.enums.XpSourceType;
import edu.language.kbee.exception.BadRequestException;
import edu.language.kbee.exception.DataViolationException;
import edu.language.kbee.exception.ResourceNotFoundException;
import edu.language.kbee.model.*;
import edu.language.kbee.payload.LessonDto;
import edu.language.kbee.payload.request.LessonCompletionRequest;
import edu.language.kbee.payload.response.LessonCompletionResponse;
import edu.language.kbee.repository.LessonBlockRepository;
import edu.language.kbee.repository.LessonProgressRepository;
import edu.language.kbee.repository.LessonRepository;
import edu.language.kbee.repository.SubmissionRepository;
import edu.language.kbee.repository.UnitRepository;
import edu.language.kbee.service.LessonService;
import edu.language.kbee.service.UserActivityService;
import edu.language.kbee.service.XpService;
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
    private final LessonBlockRepository lessonBlockRepository;
    private final XpService xpService;

    @Override
    @Transactional
    public LessonDto createLesson(LessonDto dto) {
        Unit unit = unitRepository.findById(dto.getUnitId())
                .orElseThrow(() -> new RuntimeException("Unit not found with id: " + dto.getUnitId()));

        int nextOrderIndex = lessonRepository.countByUnit_UnitId(unit.getUnitId());

        Lesson lesson = Lesson.builder()
                .title(dto.getTitle())
                .type(dto.getType())
                .orderIndex(nextOrderIndex)
                .durationMinutes(dto.getDurationMinutes() != null ? dto.getDurationMinutes() : 0)
                .status(dto.getStatus() != null ? dto.getStatus() : LessonStatus.DRAFT)
                .unit(unit)
                .build();

        syncLessonBlocks(lesson, dto.getBlocks());
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

        if (dto.getBlocks() != null) {
            Set<String> oldBlockIds = lessonBlockRepository.findByLesson_LessonIdOrderByOrderIndexAsc(lessonId)
                    .stream()
                    .map(LessonBlock::getBlockId)
                    .collect(Collectors.toSet());
            Set<String> newBlockIds = extractBlockIds(dto.getBlocks());

            for (String oldId : oldBlockIds) {
                if (!newBlockIds.contains(oldId)
                        && submissionRepository.existsByLesson_LessonIdAndBlock_BlockId(lessonId, oldId)) {
                    throw new BadRequestException("Co hoc vien da nop block nay, khong the xoa");
                }
            }

            syncLessonBlocks(lesson, dto.getBlocks());
        }

        if (dto.getDurationMinutes() != null) lesson.setDurationMinutes(dto.getDurationMinutes());
        if (dto.getStatus() != null) lesson.setStatus(dto.getStatus());

        Lesson updatedLesson = lessonRepository.save(lesson);
        return mapToDto(updatedLesson);
    }

    @Override
    @Transactional
    public LessonDto updateLessonBlocks(String lessonId, List<Map<String, Object>> blocks) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Lesson not found"));

        Set<String> oldBlockIds = lessonBlockRepository.findByLesson_LessonIdOrderByOrderIndexAsc(lessonId)
                .stream()
                .map(LessonBlock::getBlockId)
                .collect(Collectors.toSet());
        Set<String> newBlockIds = extractBlockIds(blocks);

        for (String oldId : oldBlockIds) {
            if (!newBlockIds.contains(oldId)
                    && submissionRepository.existsByLesson_LessonIdAndBlock_BlockId(lessonId, oldId)) {
                throw new BadRequestException("Co hoc vien da nop block nay, khong the xoa");
            }
        }

        syncLessonBlocks(lesson, blocks);
        Lesson updatedLesson = lessonRepository.save(lesson);
        return mapToDto(updatedLesson);
    }

    private Set<String> extractBlockIds(List<Map<String, Object>> blocks) {
        Set<String> ids = new HashSet<>();
        if (blocks == null) {
            return ids;
        }

        for (Map<String, Object> block : blocks) {
            Object id = block.get("id");
            if (id != null && !String.valueOf(id).isBlank()) {
                ids.add(String.valueOf(id));
            }
        }
        return ids;
    }

    @Override
    @Transactional
    public void deleteLesson(String lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Lesson not found"));

        if (progressRepository.existsByLesson_LessonId(lessonId)) {
            throw new BadRequestException("Bai hoc nay da co hoc vien lam bai, khong the xoa");
        }

        try {
            lessonRepository.delete(lesson);
            lessonRepository.flush();
        } catch (DataIntegrityViolationException | ConstraintViolationException e) {
            throw new DataViolationException("Bai hoc co bai nop cua hoc vien. Khong the xoa");
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
        return mapToDto(updatedLesson);
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
            try {
                List<LessonBlock> blocks = lessonBlockRepository.findByLesson_LessonIdOrderByOrderIndexAsc(lessonId);
                for (LessonBlock block : blocks) {
                    if (block.getBlockType() == LessonBlockType.QUIZ) {
                        for (QuizQuestion question : block.getQuizQuestions()) {
                            totalCount++;
                            String userAnswer = request.getAnswers().get(block.getBlockId());
                            boolean isCorrect = question.getOptions().stream()
                                    .anyMatch(option -> option.getOptionId().equals(userAnswer) && option.isCorrect());

                            if (isCorrect) correctCount++;
                            results.put(block.getBlockId(), isCorrect);
                        }
                    } else if (block.getBlockType() == LessonBlockType.DICTATION) {
                        for (DictationItem item : block.getDictationItems()) {
                            Matcher matcher = Pattern.compile("\\[(.*?)\\]").matcher(item.getPromptText());
                            int blankIndex = 0;

                            while (matcher.find()) {
                                totalCount++;
                                blankIndex++;
                                int oddIndex = blankIndex * 2 - 1;
                                String answerKey = block.getBlockId() + "-" + oddIndex;
                                String expectedWord = matcher.group(1).trim().toLowerCase();
                                String userAnswer = request.getAnswers().getOrDefault(answerKey, "").trim().toLowerCase();

                                boolean isCorrect = expectedWord.equals(userAnswer);
                                if (isCorrect) correctCount++;
                                results.put(answerKey, isCorrect);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                throw new BadRequestException("Loi khi cham diem bai hoc: " + e.getMessage());
            }

            score = totalCount == 0 ? 0 : Math.round((float) correctCount / totalCount * 100);
        } else {
            score = request.getScore() != null ? request.getScore() : 0;
        }

        LessonProgress currentProgress = progressRepository
                .findByUser_UserIdAndLesson_LessonId(user.getUserId(), lessonId)
                .orElse(LessonProgress.builder()
                        .lesson(currentLesson)
                        .user(user)
                        .status(ProgressStatus.NOT_STARTED)
                        .build());
        boolean wasAlreadyCompleted = currentProgress.getStatus() == ProgressStatus.COMPLETED;

        ProgressStatus newStatus = score >= 80 ? ProgressStatus.COMPLETED : ProgressStatus.FAILED;

        currentProgress.setScore(score);
        currentProgress.setStatus(newStatus);
        currentProgress.setCompletedAt(LocalDateTime.now());
        progressRepository.save(currentProgress);

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
        var xpAward = newStatus == ProgressStatus.COMPLETED && !wasAlreadyCompleted
                ? xpService.awardXp(user.getUserId(), XpSourceType.LESSON_COMPLETED, lessonId, 50, "Hoàn thành bài học lần đầu")
                : xpService.awardXp(user.getUserId(), XpSourceType.LESSON_COMPLETED, lessonId, 0, "Không cộng XP");

        return LessonCompletionResponse.builder()
                .courseId(courseId)
                .nextLessonId(nextLessonId)
                .message("Lesson progress tracked successfully")
                .score(score)
                .correctCount(correctCount)
                .totalCount(totalCount)
                .results(results)
                .xpAward(xpAward)
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
                .blocks(buildLessonBlockMaps(lesson))
                .level(lesson.getUnit().getCourse().getLevel())
                .orderIndex(lesson.getOrderIndex())
                .durationMinutes(lesson.getDurationMinutes())
                .status(lesson.getStatus())
                .unitId(lesson.getUnit().getUnitId())
                .courseId(lesson.getUnit().getCourse().getCourseId())
                .courseTitle(lesson.getUnit().getCourse().getTitle())
                .build();
    }

    private void syncLessonBlocks(Lesson lesson, List<Map<String, Object>> blockMaps) {
        if (blockMaps == null || blockMaps.isEmpty()) {
            lesson.getBlocks().clear();
            return;
        }

        JsonNode blocks = objectMapper.valueToTree(blockMaps);

        Map<String, LessonBlock> existingBlocks = lesson.getBlocks().stream()
                .collect(Collectors.toMap(LessonBlock::getBlockId, Function.identity()));
        Set<String> incomingBlockIds = new HashSet<>();

        for (JsonNode blockNode : blocks) {
            String blockId = firstNonBlank(blockNode.path("id").asText(), null);
            if (blockId != null) {
                incomingBlockIds.add(blockId);
            }
        }

        lesson.getBlocks().removeIf(block -> !incomingBlockIds.contains(block.getBlockId()));

        int index = 0;
        for (JsonNode blockNode : blocks) {
            String blockId = firstNonBlank(blockNode.path("id").asText(), UUID.randomUUID().toString());
            LessonBlock block = existingBlocks.get(blockId);
            if (block == null) {
                block = LessonBlock.builder()
                        .blockId(blockId)
                        .lesson(lesson)
                        .quizQuestions(new ArrayList<>())
                        .dictationItems(new ArrayList<>())
                        .writingTasks(new ArrayList<>())
                        .build();
                lesson.getBlocks().add(block);
            }
            populateLessonBlock(block, blockNode, index++);
        }
    }

    private void populateLessonBlock(LessonBlock block, JsonNode blockNode, int orderIndex) {
        LessonBlockType blockType = parseBlockType(blockNode.path("type").asText());

        block.setBlockType(blockType);
        block.setOrderIndex(orderIndex);
        block.setTitle(readText(blockNode, "title"));
        block.setContentText(readContentText(blockNode, blockType));
        block.setMetadataJson(objectMapper.writeValueAsString(blockNode));
        block.getQuizQuestions().clear();
        block.getDictationItems().clear();
        block.getWritingTasks().clear();

        if (blockType == LessonBlockType.QUIZ) {
            QuizQuestion question = QuizQuestion.builder()
                    .block(block)
                    .questionText(firstNonBlank(readText(blockNode, "question"), readText(blockNode, "questionText")))
                    .explanation(readText(blockNode, "explanation"))
                    .orderIndex(0)
                    .build();

            JsonNode options = blockNode.path("options");
            if (options.isArray()) {
                int optionIndex = 0;
                for (JsonNode optionNode : options) {
                    QuizOption option = QuizOption.builder()
                            .optionId(firstNonBlank(optionNode.path("id").asText(), UUID.randomUUID().toString()))
                            .question(question)
                            .optionText(readText(optionNode, "text"))
                            .correct(optionNode.path("isCorrect").asBoolean())
                            .orderIndex(optionIndex++)
                            .build();
                    question.getOptions().add(option);
                }
            }
            block.getQuizQuestions().add(question);
        } else if (blockType == LessonBlockType.DICTATION) {
            String promptText = readText(blockNode, "text");
            DictationItem item = DictationItem.builder()
                    .block(block)
                    .promptText(promptText)
                    .answerText(extractDictationAnswers(promptText))
                    .audioUrl(readText(blockNode, "audioUrl"))
                    .orderIndex(0)
                    .build();
            block.getDictationItems().add(item);
        } else if (blockType == LessonBlockType.ESSAY || blockType == LessonBlockType.TRANSLATION) {
            WritingTask task = WritingTask.builder()
                    .block(block)
                    .taskType(blockType == LessonBlockType.ESSAY ? SubmissionType.ESSAY : SubmissionType.TRANSLATION)
                    .prompt(blockType == LessonBlockType.ESSAY ? readText(blockNode, "title") : readText(blockNode, "english"))
                    .instructions(readText(blockNode, "instructions"))
                    .minWords(readInt(blockNode, "minWords"))
                    .maxWords(readInt(blockNode, "maxWords"))
                    .sampleAnswer(readText(blockNode, "korean"))
                    .rubricJson(blockNode.has("rubric") ? objectMapper.writeValueAsString(blockNode.path("rubric")) : null)
                    .build();
            block.getWritingTasks().add(task);
        }
    }

    private List<Map<String, Object>> buildLessonBlockMaps(Lesson lesson) {
        List<LessonBlock> blocks = lessonBlockRepository.findByLesson_LessonIdOrderByOrderIndexAsc(lesson.getLessonId());
        List<Map<String, Object>> result = new ArrayList<>();

        for (LessonBlock block : blocks) {
            Map<String, Object> item = new LinkedHashMap<>();
            putMetadata(item, block.getMetadataJson());
            item.put("id", block.getBlockId());
            item.put("type", block.getBlockType().name().toLowerCase());
            if (block.getTitle() != null) item.put("title", block.getTitle());

            if (block.getBlockType() == LessonBlockType.QUIZ && !block.getQuizQuestions().isEmpty()) {
                QuizQuestion question = block.getQuizQuestions().get(0);
                item.put("question", question.getQuestionText());
                item.put("explanation", question.getExplanation());
                item.put("options", question.getOptions().stream().map(option -> {
                    Map<String, Object> optionMap = new LinkedHashMap<>();
                    optionMap.put("id", option.getOptionId());
                    optionMap.put("text", option.getOptionText());
                    optionMap.put("isCorrect", option.isCorrect());
                    return optionMap;
                }).toList());
            } else if (block.getBlockType() == LessonBlockType.DICTATION && !block.getDictationItems().isEmpty()) {
                DictationItem itemData = block.getDictationItems().get(0);
                item.put("text", itemData.getPromptText());
                if (itemData.getAudioUrl() != null) item.put("audioUrl", itemData.getAudioUrl());
            } else if ((block.getBlockType() == LessonBlockType.ESSAY || block.getBlockType() == LessonBlockType.TRANSLATION)
                    && !block.getWritingTasks().isEmpty()) {
                WritingTask task = block.getWritingTasks().get(0);
                if (block.getBlockType() == LessonBlockType.ESSAY) {
                    item.put("title", task.getPrompt());
                    item.put("instructions", task.getInstructions());
                    item.put("minWords", task.getMinWords());
                    item.put("maxWords", task.getMaxWords());
                } else {
                    item.put("english", task.getPrompt());
                    item.put("korean", task.getSampleAnswer());
                }
            } else if (block.getContentText() != null) {
                item.put("content", block.getContentText());
            }

            result.add(item);
        }

        return result;
    }

    private void putMetadata(Map<String, Object> target, String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) return;
        try {
            Map<String, Object> metadata = objectMapper.readValue(metadataJson, Map.class);
            if (metadata != null) {
                target.putAll(metadata);
            }
        } catch (Exception ignored) {
        }
    }

    private LessonBlockType parseBlockType(String type) {
        try {
            return LessonBlockType.valueOf(type.toUpperCase());
        } catch (Exception e) {
            throw new BadRequestException("Unsupported lesson block type: " + type);
        }
    }

    private String readContentText(JsonNode blockNode, LessonBlockType blockType) {
        if (blockType == LessonBlockType.THEORY || blockType == LessonBlockType.LONG_TEXT) {
            return readText(blockNode, "content");
        }
        return null;
    }

    private String readText(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }

    private Integer readInt(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asInt();
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    private String extractDictationAnswers(String text) {
        if (text == null) return null;
        List<String> answers = new ArrayList<>();
        Matcher matcher = Pattern.compile("\\[(.*?)\\]").matcher(text);
        while (matcher.find()) {
            answers.add(matcher.group(1).trim());
        }
        return String.join(",", answers);
    }
}
