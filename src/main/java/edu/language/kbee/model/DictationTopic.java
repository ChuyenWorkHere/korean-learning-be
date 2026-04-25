package edu.language.kbee.model;

import edu.language.kbee.enums.DictationTopicStatus;
import edu.language.kbee.model.common.DateAuditing;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Table(name = "dictation_topics")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DictationTopic extends DateAuditing {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "topic_id", updatable = false, nullable = false)
    private String id;

    @NotBlank(message = "Topic title is required")
    @Column(nullable = false, length = 150)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 50)
    private String icon = "📺";

    @NotNull(message = "Topic status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DictationTopicStatus status = DictationTopicStatus.DRAFT;

    @Column(name = "lesson_count", nullable = false)
    private int lessonCount = 0;
}
