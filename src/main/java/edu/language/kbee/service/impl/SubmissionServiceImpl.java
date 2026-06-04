package edu.language.kbee.service.impl;

import edu.language.kbee.enums.LessonStatus;
import edu.language.kbee.enums.SubmissionType;
import edu.language.kbee.exception.BadRequestException;
import edu.language.kbee.exception.ResourceNotFoundException;
import edu.language.kbee.model.Lesson;
import edu.language.kbee.model.LessonBlock;
import edu.language.kbee.model.StudentSubmission;
import edu.language.kbee.model.User;
import edu.language.kbee.model.WritingTask;
import edu.language.kbee.payload.request.SubmissionRequest;
import edu.language.kbee.payload.response.SubmissionResponse;
import edu.language.kbee.repository.LessonBlockRepository;
import edu.language.kbee.repository.LessonRepository;
import edu.language.kbee.repository.SubmissionRepository;
import edu.language.kbee.repository.WritingTaskRepository;
import edu.language.kbee.service.AiService;
import edu.language.kbee.service.SubmissionService;
import edu.language.kbee.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;


@Service
@RequiredArgsConstructor
public class SubmissionServiceImpl implements SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final AiService aiService;
    private final LessonRepository lessonRepository;
    private final LessonBlockRepository lessonBlockRepository;
    private final WritingTaskRepository writingTaskRepository;
    private final ObjectMapper objectMapper;
    private final AuthUtil authUtil;

    @Override
    @Transactional
    public SubmissionResponse submitAndGrade(SubmissionRequest request) {
        User loggedInUser = authUtil.getLoggedInUser();
        Lesson lesson = findPublishedLesson(request.getLessonId());
        LessonBlock block = findLessonBlock(lesson.getLessonId(), request.getBlockId());

        JsonNode feedbackNode;
        int score = 0;
        String blockContext = buildWritingContext(block.getBlockId(), request.getSubmissionType());

        try {
            feedbackNode = aiService.gradeWritingSubmission(
                    request.getSubmittedContent(),
                    request.getSubmissionType().name(),
                    blockContext
            );

            if (feedbackNode != null && feedbackNode.has("score")) {
                score = feedbackNode.get("score").asInt();
            }
        } catch (Exception e) {
            throw new RuntimeException("Loi khi goi AI cham diem: " + e.getMessage());
        }

        String aiFeedbackString;
        try {
            aiFeedbackString = objectMapper.writeValueAsString(feedbackNode);
        } catch (Exception e) {
            aiFeedbackString = "{}";
        }

        StudentSubmission submission = StudentSubmission.builder()
                .user(loggedInUser)
                .lesson(lesson)
                .block(block)
                .submissionType(request.getSubmissionType())
                .submittedContent(request.getSubmittedContent())
                .aiScore(score)
                .aiFeedback(aiFeedbackString)
                .build();

        submission = submissionRepository.save(submission);
        return mapToResponse(submission, feedbackNode);
    }

    @Override
    public SubmissionResponse findLastSubmission(String lessonId, String blockId) {
        User loggedInUser = authUtil.getLoggedInUser();
        StudentSubmission submission = submissionRepository
                .findTop1ByLesson_LessonIdAndBlock_BlockIdAndUser_UserIdOrderByCreatedDateDesc(
                        lessonId, blockId, loggedInUser.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found"));

        JsonNode aiFeedbackNode = objectMapper.readTree(submission.getAiFeedback());
        return mapToResponse(submission, aiFeedbackNode);
    }

    @Override
    @Transactional
    public SubmissionResponse submitAndCheckTranslation(SubmissionRequest request) {
        User loggedInUser = authUtil.getLoggedInUser();
        Lesson lesson = findPublishedLesson(request.getLessonId());
        LessonBlock block = findLessonBlock(lesson.getLessonId(), request.getBlockId());
        WritingTask task = findWritingTask(block.getBlockId(), SubmissionType.TRANSLATION);

        String correctAnswer = task.getSampleAnswer() != null ? task.getSampleAnswer() : "Khong co dap an tham khao";
        int score = isTranslationCorrect(request.getSubmittedContent(), correctAnswer, block.getMetadataJson()) ? 100 : 50;

        String mockFeedbackStr = String.format("""
                {
                  "original": "%s",
                  "userMeaning": "Cau dich cua ban",
                  "corrected": "%s",
                  "aiMeaning": "Dap an mau",
                  "score": %d,
                  "performanceText": "Hay doi chieu va luyen tap them nhe!",
                  "nativeNuance": "Ban dang xem dap an tu dong tu he thong. Neu muon phan tich loi sai, hay thu dung tinh nang AI Cham.",
                  "breakdowns": []
                }
                """,
                escapeJson(request.getSubmittedContent()),
                escapeJson(correctAnswer),
                score
        );

        JsonNode feedbackNode;
        try {
            feedbackNode = objectMapper.readTree(mockFeedbackStr);
        } catch (Exception e) {
            throw new RuntimeException("Loi parse JSON: " + e.getMessage());
        }

        StudentSubmission submission = StudentSubmission.builder()
                .user(loggedInUser)
                .lesson(lesson)
                .block(block)
                .submissionType(request.getSubmissionType())
                .submittedContent(request.getSubmittedContent())
                .aiScore(score)
                .aiFeedback(mockFeedbackStr)
                .build();

        submission = submissionRepository.save(submission);
        return mapToResponse(submission, feedbackNode);
    }

    private Lesson findPublishedLesson(String lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new BadRequestException("Lesson not found"));

        if (lesson.getStatus() != LessonStatus.PUBLISHED) {
            throw new BadRequestException("Lesson is not published");
        }

        return lesson;
    }

    private LessonBlock findLessonBlock(String lessonId, String blockId) {
        LessonBlock block = lessonBlockRepository.findById(blockId)
                .orElseThrow(() -> new BadRequestException("Block not found"));

        if (!block.getLesson().getLessonId().equals(lessonId)) {
            throw new BadRequestException("Block does not belong to this lesson");
        }

        return block;
    }

    private WritingTask findWritingTask(String blockId, SubmissionType submissionType) {
        return writingTaskRepository.findFirstByBlock_BlockIdAndTaskType(blockId, submissionType)
                .orElseThrow(() -> new BadRequestException("Writing task not found"));
    }

    private String buildWritingContext(String blockId, SubmissionType submissionType) {
        WritingTask task = findWritingTask(blockId, submissionType);

        if (submissionType == SubmissionType.ESSAY) {
            return "Essay Title/Topic: " + nullToEmpty(task.getPrompt())
                    + "\nInstructions: " + nullToEmpty(task.getInstructions())
                    + "\nYeu cau do dai (BAT BUOC): Tu " + nullToEmpty(task.getMinWords())
                    + " den " + nullToEmpty(task.getMaxWords()) + " ky tu.";
        }

        if (submissionType == SubmissionType.TRANSLATION) {
            return "Cau can dich sang tieng Han: " + nullToEmpty(task.getPrompt());
        }

        return "";
    }

    private boolean isTranslationCorrect(String submittedContent, String correctAnswer, String metadataJson) {
        String normalizedSubmit = normalizeAnswer(submittedContent);
        if (normalizedSubmit.equals(normalizeAnswer(correctAnswer))) {
            return true;
        }

        try {
            JsonNode metadata = objectMapper.readTree(metadataJson);
            JsonNode variations = metadata.path("variations");
            if (variations.isArray()) {
                for (JsonNode variationNode : variations) {
                    if (normalizedSubmit.equals(normalizeAnswer(variationNode.asText()))) {
                        return true;
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return false;
    }

    private String normalizeAnswer(String value) {
        if (value == null) return "";
        return value.replaceAll("[^a-zA-Z0-9가-힣ㄱ-ㅎㅏ-ㅣ]", "").toLowerCase();
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }

    private String nullToEmpty(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private SubmissionResponse mapToResponse(StudentSubmission submission, JsonNode feedbackNode) {
        return SubmissionResponse.builder()
                .id(submission.getId())
                .lessonId(submission.getLesson().getLessonId())
                .blockId(submission.getBlock().getBlockId())
                .submissionType(submission.getSubmissionType())
                .submittedContent(submission.getSubmittedContent())
                .aiScore(submission.getAiScore())
                .aiFeedback(feedbackNode)
                .createdAt(submission.getCreatedDate())
                .tutorScore(submission.getTutorScore())
                .tutorFeedback(submission.getTutorFeedback())
                .build();
    }
}
