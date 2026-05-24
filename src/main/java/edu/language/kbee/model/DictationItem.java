package edu.language.kbee.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Table(name = "dictation_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DictationItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "dictation_item_id", updatable = false, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "block_id", nullable = false)
    private LessonBlock block;

    @NotBlank(message = "Prompt text is required")
    @Column(name = "prompt_text", columnDefinition = "TEXT", nullable = false)
    private String promptText;

    @Column(name = "answer_text", columnDefinition = "TEXT")
    private String answerText;

    @Column(name = "audio_url", columnDefinition = "TEXT")
    private String audioUrl;

    @NotNull(message = "Order index is required")
    @Min(value = 0, message = "Order index cannot be negative")
    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;
}
