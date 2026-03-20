package edu.language.kbee.model;

import edu.language.kbee.model.common.DateAuditing;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "flashcard_decks")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlashcardDeck extends DateAuditing {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "deck_id", updatable = false, nullable = false)
    private String deckId;

    @NotBlank(message = "Deck title is required")
    @Size(max = 150, message = "Title must not exceed 150 characters")
    @Column(nullable = false, length = 150)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "level")
    private String level;

    @Builder.Default
    @Column(name = "is_public", nullable = false)
    private boolean isPublic = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Builder.Default
    @OneToMany(mappedBy = "deck", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Flashcard> flashcards = new ArrayList<>();
}
