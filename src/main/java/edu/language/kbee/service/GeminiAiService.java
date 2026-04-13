package edu.language.kbee.service;

import tools.jackson.databind.JsonNode;

public interface GeminiAiService {

    JsonNode generateWritingPrompt(String topic) throws Exception;

    JsonNode gradeWritingSubmission(String studentContent, String taskType, String blockContext) throws Exception;

    JsonNode generateQuizzes(String context, String skillType) throws Exception;
}
