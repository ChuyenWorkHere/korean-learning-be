package edu.language.kbee.model;

import edu.language.kbee.model.common.DateAuditing;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Entity
@Table(name = "chapters")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Chapter extends DateAuditing {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "chapter_id", updatable = false, nullable = false)
    private String chapterId;

    @NotBlank(message = "Chapter title is required")
    @Column(nullable = false, length = 150)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;
    
    @Column(name = "chapter_order")
    private Integer chapterOrder;
}
