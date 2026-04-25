package edu.language.kbee.payload;

import edu.language.kbee.enums.DictationLessonStatus;
import edu.language.kbee.enums.DictationMediaType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DictationLessonDto {
    private String id;
    private String title;
    private DictationMediaType mediaType;
    private String youtubeId;
    private String audioUrl;
    private int sentenceCount;
    private DictationLessonStatus status;
    private String duration;
    private String topicId;
    private List<DictationSentenceDto> sentences;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
