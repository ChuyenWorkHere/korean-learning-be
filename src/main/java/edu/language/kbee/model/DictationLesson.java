package edu.language.kbee.model;

import edu.language.kbee.enums.DictationLessonStatus;
import edu.language.kbee.enums.DictationMediaType;
import edu.language.kbee.model.common.DateAuditing;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "dictation_lessons")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DictationLesson extends DateAuditing {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "lesson_id", updatable = false, nullable = false)
    private String id;

    @NotBlank(message = "Lesson title is required")
    @Column(nullable = false, length = 150)
    private String title;

    @NotNull(message = "Media type is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DictationMediaType mediaType;

    @Column(name = "youtube_id", length = 50)
    private String youtubeId;

    @Column(name = "audio_url", columnDefinition = "TEXT")
    private String audioUrl;

    @Column(name = "sentence_count", nullable = false)
    private int sentenceCount = 0;

    @NotNull(message = "Lesson status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DictationLessonStatus status = DictationLessonStatus.DRAFT;

    @Column(length = 10)
    private String duration = "--:--";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", nullable = false)
    private DictationTopic topic;

    @OneToMany(mappedBy = "lesson", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    @Builder.Default
    private List<DictationSentence> sentences = new ArrayList<>();
}
