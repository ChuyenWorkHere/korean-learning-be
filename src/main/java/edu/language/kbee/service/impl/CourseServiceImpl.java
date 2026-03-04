package edu.language.kbee.service.impl;

import edu.language.kbee.enums.CourseStatus;
import edu.language.kbee.exception.BadRequestException;
import edu.language.kbee.exception.ResourceNotFoundException;
import edu.language.kbee.model.Course;
import edu.language.kbee.payload.CourseDto;
import edu.language.kbee.repository.CourseRepository;
import edu.language.kbee.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;
    private final ModelMapper modelMapper;

    @Override
    public List<CourseDto> getAllCourses() {

        List<Course> courses = courseRepository.findAll();

        return courses.stream().map(course -> modelMapper.map(course, CourseDto.class)).toList();
    }

    @Override
    public List<CourseDto> getUserCourses() {
        return List.of();
    }

    @Override
    public CourseDto getCourseById(String courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));

        return modelMapper.map(course, CourseDto.class);
    }

    @Override
    public CourseDto createCourse(CourseDto courseDto) {
        Course course = modelMapper.map(courseDto, Course.class);
        Course savedCourse = courseRepository.save(course);
        return modelMapper.map(savedCourse, CourseDto.class);
    }

    @Override
    public CourseDto updateCourse(String courseId, CourseDto courseDto) {
        return null;
    }

    @Override
    public CourseDto toggleCourseStatus(String courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));

        if(course.getStatus() == CourseStatus.PUBLISHED) {
            course.setStatus(CourseStatus.DRAFT);
        } else if (course.getStatus() == CourseStatus.DRAFT) {
            course.setStatus(CourseStatus.PUBLISHED);
        }

        Course updatedCourse = courseRepository.save(course);
        return modelMapper.map(updatedCourse, CourseDto.class);
    }

    @Override
    public void deleteCourse(String courseId) {

    }
}
