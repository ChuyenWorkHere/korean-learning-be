package edu.language.kbee.service;

import edu.language.kbee.payload.request.SubmissionRequest;
import edu.language.kbee.payload.response.SubmissionResponse;

public interface SubmissionService {

    SubmissionResponse submitAndGrade(SubmissionRequest request);

    SubmissionResponse findLastSubmission(String lessonId, String blockId);
}
