package edu.language.kbee.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlashcardDto {
    private String cardId;
    private String word;
    private String meaning;
    private String exampleSentence;
    private String note;
}
