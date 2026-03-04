package edu.language.kbee.model;

import edu.language.kbee.model.common.DateAuditing;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_courses")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCourse extends DateAuditing {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_course_id")
    private String userCourseId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Builder.Default
    @Column(name = "progress_percentage", nullable = false)
    private Integer progressPercentage = 0;

    @Builder.Default
    @Column(name = "completed_lessons", nullable = false)
    private Integer completedLessons = 0;

    @Builder.Default
    @Column(name = "is_completed", nullable = false)
    private boolean isCompleted = false;

    @Builder.Default
    @Column(name = "is_locked", nullable = false)
    private boolean isLocked = false;
}