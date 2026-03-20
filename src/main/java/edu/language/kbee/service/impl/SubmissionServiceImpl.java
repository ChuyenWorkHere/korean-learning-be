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
import edu.language.kbee.service.GeminiAiService;
import edu.language.kbee.service.SubmissionService;
import edu.language.kbee.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class SubmissionServiceImpl implements SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final GeminiAiService geminiAiService;
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

        try {
            feedbackNode = geminiAiService.gradeWritingSubmission(
                    request.getSubmittedContent(),
                    request.getSubmissionType().name()
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
                .lessonId(submission.getLesson().getLessonId())
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

}
