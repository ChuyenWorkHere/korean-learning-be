package edu.language.kbee.model;

import edu.language.kbee.enums.LessonBlockType;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "lesson_blocks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonBlock {

    @Id
    @Column(name = "block_id", updatable = false, nullable = false)
    private String blockId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @NotNull(message = "Block type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "block_type", nullable = false, length = 40)
    private LessonBlockType blockType;

    @NotNull(message = "Order index is required")
    @Min(value = 0, message = "Order index cannot be negative")
    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @Column(length = 200)
    private String title;

    @Column(name = "content_text", columnDefinition = "TEXT")
    private String contentText;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata_json", columnDefinition = "json")
    private String metadataJson;

    @Builder.Default
    @OneToMany(mappedBy = "block", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<QuizQuestion> quizQuestions = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "block", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<DictationItem> dictationItems = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "block", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<WritingTask> writingTasks = new ArrayList<>();
}
