package edu.language.kbee.model;

import edu.language.kbee.enums.SubmissionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "writing_tasks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WritingTask {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "writing_task_id", updatable = false, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "block_id", nullable = false)
    private LessonBlock block;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_type", nullable = false, length = 30)
    private SubmissionType taskType;

    @Column(columnDefinition = "TEXT")
    private String prompt;

    @Column(columnDefinition = "TEXT")
    private String instructions;

    @Column(name = "min_words")
    private Integer minWords;

    @Column(name = "max_words")
    private Integer maxWords;

    @Column(name = "sample_answer", columnDefinition = "TEXT")
    private String sampleAnswer;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "rubric_json", columnDefinition = "json")
    private String rubricJson;
}
