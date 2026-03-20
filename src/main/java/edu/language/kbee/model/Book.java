package edu.language.kbee.model;

import edu.language.kbee.enums.BookStatus;
import edu.language.kbee.enums.CourseLevel;
import edu.language.kbee.model.common.DateAuditing;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "books")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Book extends DateAuditing {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "book_id", updatable = false, nullable = false)
    private String bookId;

    @NotBlank(message = "Book title is required")
    @Size(max = 150, message = "Title must not exceed 150 characters")
    @Column(nullable = false, length = 150)
    private String title;

    @NotNull(message = "Book level is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CourseLevel level;

    @Column(length = 50)
    private String duration; // e.g. "15 min read"

    @Column(columnDefinition = "TEXT")
    private String image; // Cover image URL

    @NotNull(message = "Book status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookStatus status;

    @Builder.Default
    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Chapter> chapters = new ArrayList<>();
}
