package edu.language.kbee.model;

import edu.language.kbee.model.common.DateAuditing;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "user_activities", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "activity_date"})
})
@Setter @Getter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class UserActivity extends DateAuditing {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String activityId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "activity_date", nullable = false)
    private LocalDate activityDate;

    @Builder.Default
    @Column(name = "activity_count", nullable = false)
    private int activityCount = 0;
}
