package edu.language.kbee.payload.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DictationTopicRequest {
    @NotBlank(message = "Title is required")
    private String title;
    private String description;
    private String icon;
}
