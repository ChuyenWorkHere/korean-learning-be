package edu.language.kbee.service.impl;

import edu.language.kbee.enums.LessonStatus;
import edu.language.kbee.exception.BadRequestException;
import edu.language.kbee.exception.ResourceNotFoundException;
import edu.language.kbee.model.Lesson;
import edu.language.kbee.model.StudentSubmission;
import edu.language.kbee.model.User;
import edu.language.kbee.payload.request.SubmissionRequest;
import edu.language.kbee.payload.response.SubmissionResponse;
import edu.language.kbee.repository.LessonRepository;
import edu.language.kbee.repository.SubmissionRepository;
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
    private final AiService geminiAiService;
    private final LessonRepository lessonRepository;
    private final ObjectMapper objectMapper;
    private final AuthUtil authUtil;

    @Override
    @Transactional
    public SubmissionResponse submitAndGrade(SubmissionRequest request) {

        User loggedInUser = authUtil.getLoggedInUser();

        Lesson lesson = lessonRepository.findById(request.getLessonId())
                .orElseThrow(() -> new BadRequestException("Lesson not found"));

        if(lesson.getStatus() != LessonStatus.PUBLISHED) {
            throw new BadRequestException("Lesson is not published");
        }

        JsonNode feedbackNode = null;
        int score = 0;

        String blockContext = "";
        try {
            JsonNode blocks = objectMapper.readTree(lesson.getContent());
            for (JsonNode block : blocks) {
                if (block.path("id").asText().equals(request.getBlockId())) {
                    if (request.getSubmissionType().name().equals("ESSAY")) {
                         blockContext = "Essay Title/Topic: " + block.path("title").asText() + "\nInstructions: " + block.path("instructions").asText();
                    } else if (request.getSubmissionType().name().equals("TRANSLATION")) {
                         blockContext = "Câu cần dịch sang tiếng Hàn: " + block.path("english").asText();
                    }
                    break;
                }
            }
        } catch (Exception e) {
            // ignore JSON parse error
        }

        try {
            feedbackNode = geminiAiService.gradeWritingSubmission(
                    request.getSubmittedContent(),
                    request.getSubmissionType().name(),
                    blockContext
            );

            if (feedbackNode != null && feedbackNode.has("score")) {
                score = feedbackNode.get("score").asInt();
            }
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi gọi AI chấm điểm: " + e.getMessage());
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
                .blockId(request.getBlockId())
                .submissionType(request.getSubmissionType())
                .submittedContent(request.getSubmittedContent())
                .aiScore(score)
                .aiFeedback(aiFeedbackString)
                .build();

        submission = submissionRepository.save(submission);

        return SubmissionResponse.builder()
                .id(submission.getId())
                .lessonId(submission.getLesson().getLessonId())
                .blockId(submission.getBlockId())
                .submissionType(submission.getSubmissionType())
                .submittedContent(submission.getSubmittedContent())
                .aiScore(submission.getAiScore())
                .aiFeedback(feedbackNode)
                .createdAt(submission.getCreatedDate())
                .build();
    }

    @Override
    public SubmissionResponse findLastSubmission(String lessonId, String blockId) {

        User loggedInUser = authUtil.getLoggedInUser();
        StudentSubmission submission = submissionRepository.findTop1ByLesson_LessonIdAndBlockIdAndUser_UserIdOrderByCreatedDateDesc(lessonId, blockId, loggedInUser.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found"));

        JsonNode aiFeedbackNode = objectMapper.readTree(submission.getAiFeedback());

        return SubmissionResponse.builder()
                .id(submission.getId())
                .lessonId(lessonId)
                .blockId(submission.getBlockId())
                .submissionType(submission.getSubmissionType())
                .submittedContent(submission.getSubmittedContent())
                .aiScore(submission.getAiScore())
                .aiFeedback(aiFeedbackNode)
                .createdAt(submission.getCreatedDate())
                .tutorScore(submission.getTutorScore())
                .tutorFeedback(submission.getTutorFeedback())
                .build();
    }

    @Override
    @Transactional
    public SubmissionResponse submitAndCheckTranslation(SubmissionRequest request) {
        User loggedInUser = authUtil.getLoggedInUser();
        Lesson lesson = lessonRepository.findById(request.getLessonId())
                .orElseThrow(() -> new BadRequestException("Lesson not found"));

        if (lesson.getStatus() != LessonStatus.PUBLISHED) {
            throw new BadRequestException("Lesson is not published");
        }

        String correctAnswer = "Không có đáp án tham khảo";
        int score = 0;

        try {
            JsonNode blocks = objectMapper.readTree(lesson.getContent());
            for (JsonNode block : blocks) {
                if (block.path("id").asText().equals(request.getBlockId())) {
                    if (request.getSubmissionType().name().equals("TRANSLATION")) {
                        correctAnswer = block.path("korean").asText();
                        String normalizedSubmit = request.getSubmittedContent().replaceAll("[^a-zA-Z0-9가-힣ㄱ-ㅎㅏ-ㅣ]", "").toLowerCase();
                        String normalizedCorrect = correctAnswer.replaceAll("[^a-zA-Z0-9가-힣ㄱ-ㅎㅏ-ㅣ]", "").toLowerCase();
                        
                        boolean isCorrect = normalizedSubmit.equals(normalizedCorrect);

                        if (!isCorrect && block.has("variations") && block.path("variations").isArray()) {
                            for (JsonNode variationNode : block.path("variations")) {
                                String normalizedVariation = variationNode.asText().replaceAll("[^a-zA-Z0-9가-힣ㄱ-ㅎㅏ-ㅣ]", "").toLowerCase();
                                if (normalizedSubmit.equals(normalizedVariation)) {
                                    isCorrect = true;
                                    correctAnswer = variationNode.asText();
                                    break;
                                }
                            }
                        }

                        if (isCorrect) {
                            score = 100;
                        } else {
                            score = 50;
                        }
                    }
                    break;
                }
            }
        } catch (Exception e) {
            // ignore JSON parse error
        }

        String mockFeedbackStr = String.format("""
                {
                  "original": "%s",
                  "userMeaning": "Câu dịch của bạn",
                  "corrected": "%s",
                  "aiMeaning": "Đáp án mẫu",
                  "score": %d,
                  "performanceText": "Hãy đối chiếu và luyện tập thêm nhé!",
                  "nativeNuance": "Bạn đang xem đáp án tự động từ hệ thống. Nếu muốn phân tích lỗi sai, hãy thử dùng tính năng AI Chấm.",
                  "breakdowns": []
                }
                """, 
                request.getSubmittedContent().replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", ""),
                correctAnswer.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", ""),
                score
        );

        JsonNode feedbackNode;
        try {
            feedbackNode = objectMapper.readTree(mockFeedbackStr);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi parse JSON: " + e.getMessage());
        }

        StudentSubmission submission = StudentSubmission.builder()
                .user(loggedInUser)
                .lesson(lesson)
                .blockId(request.getBlockId())
                .submissionType(request.getSubmissionType())
                .submittedContent(request.getSubmittedContent())
                .aiScore(score)
                .aiFeedback(mockFeedbackStr)
                .build();

        submission = submissionRepository.save(submission);

        return SubmissionResponse.builder()
                .id(submission.getId())
                .lessonId(submission.getLesson().getLessonId())
                .blockId(submission.getBlockId())
                .submissionType(submission.getSubmissionType())
                .submittedContent(submission.getSubmittedContent())
                .aiScore(submission.getAiScore())
                .aiFeedback(feedbackNode)
                .createdAt(submission.getCreatedDate())
                .build();
    }

}
