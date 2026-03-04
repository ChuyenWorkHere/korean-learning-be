package edu.language.kbee.payload;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseDto {

    private String courseId;

    private String title;

    private String description;

    private String iconName;

    private String level;

    private String status;

}
