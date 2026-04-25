package edu.language.kbee.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "dictation_sentences")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DictationSentence {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "sentence_id", updatable = false, nullable = false)
    private String id;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    @Column(name = "start_time", nullable = false)
    private double startTime;

    @Column(name = "end_time", nullable = false)
    private double endTime;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String text;

    @Column(columnDefinition = "TEXT")
    private String translation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    private DictationLesson lesson;
}
