package edu.language.kbee.payload.response;

import edu.language.kbee.enums.SubmissionType;
import lombok.Builder;
import lombok.Data;
import tools.jackson.databind.JsonNode;

import java.time.LocalDateTime;

@Data
@Builder
public class SubmissionResponse {
    private String id;
    private String lessonId;
    private String blockId;
    private SubmissionType submissionType;
    private String submittedContent;

    private Integer aiScore;

    private JsonNode aiFeedback;

    private Integer tutorScore;
    private String tutorFeedback;
    private LocalDateTime createdAt;
}
