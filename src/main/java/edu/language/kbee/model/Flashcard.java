package edu.language.kbee.model;

import edu.language.kbee.model.common.DateAuditing;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Entity
@Table(name = "flashcards")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Flashcard extends DateAuditing {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "card_id", updatable = false, nullable = false)
    private String cardId;

    @NotBlank(message = "Word is required")
    @Column(nullable = false, length = 200)
    private String word;

    @Column(columnDefinition = "TEXT")
    private String meaning;

    @Column(columnDefinition = "TEXT")
    private String exampleSentence;

    @Column(columnDefinition = "TEXT")
    private String note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deck_id", nullable = false)
    private FlashcardDeck deck;
}
