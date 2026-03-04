package edu.language.kbee.payload;

import edu.language.kbee.enums.CourseLevel;
import edu.language.kbee.enums.LessonStatus;
import edu.language.kbee.enums.LessonType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonDto {

    private String lessonId;

    @NotBlank(message = "Lesson title is required")
    private String title;

    @NotNull(message = "Lesson type is required")
    private LessonType type;

    private String content;

    private Integer orderIndex;

    private CourseLevel level;

    @Min(value = 0, message = "Duration cannot be negative")
    private Integer durationMinutes;

    private LessonStatus status;

    @NotBlank(message = "Unit ID is required")
    private String unitId;
}