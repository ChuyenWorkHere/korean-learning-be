package edu.language.kbee.payload.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudyBatchResultRequest {

    @NotBlank(message = "Deck ID is required")
    private String deckId;

    @Valid
    @NotEmpty(message = "Study results are required")
    private List<Item> results;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        @NotBlank(message = "Card ID is required")
        private String cardId;

        @Min(value = 0, message = "Score must be between 0 (Forgot) and 3 (Easy)")
        @Max(value = 3, message = "Score must be between 0 (Forgot) and 3 (Easy)")
        private int score;
    }
}
