package edu.language.kbee.payload.request;

import edu.language.kbee.enums.DictationLessonStatus;
import edu.language.kbee.enums.DictationMediaType;
import edu.language.kbee.payload.DictationSentenceDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class DictationLessonRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @NotNull(message = "Media type is required")
    private DictationMediaType mediaType;

    private String youtubeId;

    private String audioUrl;

    private DictationLessonStatus status;

    private String duration;

    private List<DictationSentenceDto> sentences;
}
