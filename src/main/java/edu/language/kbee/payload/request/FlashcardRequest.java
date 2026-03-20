package edu.language.kbee.payload.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlashcardRequest {

    @NotBlank(message = "Word is required")
    @Size(max = 200, message = "Word must not exceed 200 characters")
    private String word;

    private String meaning;
    private String exampleSentence;
    private String note;
}
