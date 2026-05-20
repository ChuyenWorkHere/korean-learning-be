package edu.language.kbee.service;

import edu.language.kbee.payload.CourseDto;
import org.springframework.data.domain.Page;

import java.util.List;

public interface CourseService {

    List<CourseDto> getAllCourses();
    Page<CourseDto> getAllCourses(int page, int size);
    List<CourseDto> getUserCourses();
    CourseDto getCourseById(String courseId);
    CourseDto createCourse(CourseDto courseDto);
    CourseDto updateCourse(String courseId, CourseDto courseDto);
    CourseDto toggleCourseStatus(String courseId);
    void deleteCourse(String courseId);

}
