package edu.language.kbee.payload;

import edu.language.kbee.enums.ProgressStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserUnitDto {

    private String unitId;

    @NotBlank(message = "Unit title is required")
    private String title;

    private Integer orderIndex;

    private ProgressStatus progressStatus;

    @NotBlank(message = "Course ID is required")
    private String courseId;

    private List<LessonProgressDto> lessons;

}
