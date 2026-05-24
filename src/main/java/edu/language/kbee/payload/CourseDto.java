package edu.language.kbee.payload;

import lombok.*;
import jakarta.validation.constraints.Size;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseDto {

    private String courseId;

    private String title;

    private String description;

    @Size(max = 1000, message = "Image URL must not exceed 1000 characters")
    private String imageUrl;

    private String level;

    private String status;

    private Integer totalUnits;
    private Integer totalLessons;
    private Integer totalEnrollment;

}
