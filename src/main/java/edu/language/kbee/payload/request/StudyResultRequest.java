package edu.language.kbee.payload.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudyResultRequest {
    @NotBlank(message = "Card ID is required")
    private String cardId;

    @Min(value = 0, message = "Score must be between 0 (Forgot) and 3 (Easy)")
    @Max(value = 3, message = "Score must be between 0 (Forgot) and 3 (Easy)")
    private int score;
}
