package edu.language.kbee.payload.request;

import edu.language.kbee.enums.SubmissionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubmissionRequest {

    @NotBlank(message = "Lesson ID is required")
    private String lessonId;

    @NotBlank(message = "Block ID is required")
    private String blockId;

    @NotNull(message = "Submission Type is required")
    private SubmissionType submissionType;

    @NotBlank(message = "Content cannot be empty")
    private String submittedContent;
}