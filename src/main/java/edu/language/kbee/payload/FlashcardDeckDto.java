package edu.language.kbee.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlashcardDeckDto {
    private String deckId;
    private String title;
    private String description;
    private String level;
    private long learnedCount;
    private boolean isPublic;
    private UserDto user;
    private List<FlashcardDto> flashcards;
}
