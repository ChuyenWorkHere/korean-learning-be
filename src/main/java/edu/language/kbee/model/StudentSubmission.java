package edu.language.kbee.model;

import edu.language.kbee.enums.SubmissionType;
import edu.language.kbee.model.common.DateAuditing;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "student_submissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentSubmission extends DateAuditing {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @Column(name = "block_id", nullable = false)
    private String blockId;

    @Enumerated(EnumType.STRING)
    @Column(name = "submission_type", nullable = false)
    private SubmissionType submissionType;

    @Column(name = "submitted_content", columnDefinition = "TEXT", nullable = false)
    private String submittedContent;

    @Column(name = "ai_score")
    private Integer aiScore;

    @Column(name = "ai_feedback", columnDefinition = "TEXT")
    private String aiFeedback;

    @Column(name = "tutor_score")
    private Integer tutorScore;

    @Column(name = "tutor_feedback", columnDefinition = "TEXT")
    private String tutorFeedback;

}
