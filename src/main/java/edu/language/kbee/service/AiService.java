package edu.language.kbee.service;

import tools.jackson.databind.JsonNode;

public interface AiService {

    JsonNode generateWritingPrompt(String topic) throws Exception;

    JsonNode generateTranslationPrompt(String topic) throws Exception;


    JsonNode gradeWritingSubmission(String studentContent, String taskType, String blockContext) throws Exception;

    JsonNode generateQuizzes(String context, String skillType, int quizCount) throws Exception;

    JsonNode generateGrammarLesson(String grammarPoint, String level) throws Exception;

    JsonNode splitSubtitleWithAI(String rawSubtitleText) throws Exception;

    JsonNode extractVocabulary(String content) throws Exception;
}
