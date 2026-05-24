package edu.language.kbee.model;

import edu.language.kbee.enums.GenderName;
import edu.language.kbee.model.common.DateAuditing;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "users")
@Setter@Getter
@NoArgsConstructor@AllArgsConstructor
@Builder
public class User extends DateAuditing {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String userId;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(length = 100)
    private String fullName;

    @Enumerated(EnumType.STRING)
    private GenderName genderName;

    private byte isLocked;

    @Column(unique = true)
    private String email;

    private LocalDate dateOfBirth;

    private String userAvatar;

    private String phoneNumber;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Builder.Default
    private int currentStreak = 0;

    private LocalDate lastActivityDate;

    @Builder.Default
    private byte isPremium = 0;

    private LocalDate premiumExpiryDate;

    @Builder.Default
    @Column(name = "total_xp", nullable = false, columnDefinition = "INT DEFAULT 0")
    private int totalXp = 0;

    @Builder.Default
    @Column(name = "level", nullable = false, columnDefinition = "INT DEFAULT 1")
    private int level = 1;

    @Builder.Default
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_role",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn( name = "role_id"))
    private Set<Role> roles = new HashSet<>();


    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserCourse> enrolledCourses = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LessonProgress> lessonProgresses = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FlashcardDeck> flashcardDecks = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FlashcardProgress> flashcardProgresses = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserXpEvent> xpEvents = new ArrayList<>();
}
