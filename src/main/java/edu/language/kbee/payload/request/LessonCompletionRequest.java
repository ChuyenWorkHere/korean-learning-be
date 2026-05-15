package edu.language.kbee.payload.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonCompletionRequest {
    private Integer score;
    private Map<String, String> answers;
}
