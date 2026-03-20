package edu.language.kbee.model;

import edu.language.kbee.model.common.DateAuditing;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "flashcard_progress")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlashcardProgress extends DateAuditing {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "progress_id", updatable = false, nullable = false)
    private String progressId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id", nullable = false)
    private Flashcard flashcard;

    // For Leitner System: 1, 3, 7, 14, 30 days...
    @Builder.Default
    @Column(name = "box_level", nullable = false)
    private int boxLevel = 1;

    @Column(name = "next_review_date")
    private LocalDateTime nextReviewDate;

    @Builder.Default
    @Column(name = "consecutive_correct_answers", nullable = false)
    private int consecutiveCorrectAnswers = 0;
}
