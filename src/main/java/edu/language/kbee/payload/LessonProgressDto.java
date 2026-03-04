package edu.language.kbee.payload;

import edu.language.kbee.enums.LessonStatus;
import edu.language.kbee.enums.LessonType;
import edu.language.kbee.enums.ProgressStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonProgressDto {

    private Long progressId;

    private String lessonId;

    private String title;

    private LessonType type;

    private String content;

    private Integer orderIndex;

    private Integer durationMinutes;

    private String unitId;

    private ProgressStatus status;

    private LocalDateTime completedAt;

}