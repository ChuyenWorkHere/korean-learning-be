package edu.language.kbee.payload;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminUnitDto {

    private String unitId;

    @NotBlank(message = "Unit title is required")
    private String title;

    private Integer orderIndex;

    @NotBlank(message = "Course ID is required")
    private String courseId;

    private List<LessonDto> lessons;
}