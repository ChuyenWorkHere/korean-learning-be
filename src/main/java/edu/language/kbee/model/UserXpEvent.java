package edu.language.kbee.model;

import edu.language.kbee.enums.XpSourceType;
import edu.language.kbee.model.common.DateAuditing;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "user_xp_events",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_xp_source",
                columnNames = {"user_id", "source_type", "source_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserXpEvent extends DateAuditing {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "xp_event_id", updatable = false, nullable = false)
    private String xpEventId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 40)
    private XpSourceType sourceType;

    @Column(name = "source_id", nullable = false, length = 100)
    private String sourceId;

    @Column(name = "xp_amount", nullable = false)
    private int xpAmount;

    @Column(length = 255)
    private String reason;
}
