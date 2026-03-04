package edu.language.kbee.service;

import edu.language.kbee.payload.CourseDto;

import java.util.List;

public interface CourseService {

    List<CourseDto> getAllCourses();
    List<CourseDto> getUserCourses();
    CourseDto getCourseById(String courseId);
    CourseDto createCourse(CourseDto courseDto);
    CourseDto updateCourse(String courseId, CourseDto courseDto);
    CourseDto toggleCourseStatus(String courseId);
    void deleteCourse(String courseId);

}
