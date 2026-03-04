package edu.language.kbee.payload;


import edu.language.kbee.enums.CourseLevel;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserCourseDto {

    private String courseId;
    private String title;
    private String description;
    private String iconName;
    private CourseLevel courseLevel;
    private Integer progress;
    private Integer completedLessons;
    private Integer totalLessons;
    private Boolean isLocked;
    private Boolean isEnrolled;
}