package edu.language.kbee.payload.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonCompletionResponse {
    private String courseId;
    private String nextLessonId;
    private String message;
    private Integer score;
    private Integer correctCount;
    private Integer totalCount;
    private Map<String, Boolean> results; // questionId -> isCorrect
    private XpAwardResponse xpAward;
}
