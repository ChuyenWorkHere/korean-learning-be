package edu.language.kbee.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Table(name = "quiz_options")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizOption {

    @Id
    @Column(name = "option_id", updatable = false, nullable = false)
    private String optionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private QuizQuestion question;

    @NotBlank(message = "Option text is required")
    @Column(name = "option_text", columnDefinition = "TEXT", nullable = false)
    private String optionText;

    @Column(name = "is_correct", nullable = false)
    private boolean correct;

    @NotNull(message = "Order index is required")
    @Min(value = 0, message = "Order index cannot be negative")
    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;
}
