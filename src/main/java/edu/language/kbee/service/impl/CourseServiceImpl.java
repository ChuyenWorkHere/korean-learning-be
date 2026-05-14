package edu.language.kbee.service.impl;

import edu.language.kbee.enums.CourseLevel;
import edu.language.kbee.enums.CourseStatus;
import edu.language.kbee.exception.BadRequestException;
import edu.language.kbee.exception.ResourceNotFoundException;
import edu.language.kbee.model.Course;
import edu.language.kbee.payload.CourseDto;
import edu.language.kbee.repository.CourseRepository;
import edu.language.kbee.repository.UserCourseRepository;
import edu.language.kbee.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;
    private final UserCourseRepository userCourseRepository;
    private final ModelMapper modelMapper;

    @Override
    @Transactional
    public List<CourseDto> getAllCourses() {

        List<Course> courses = courseRepository.findAll();
        
        if (courses.isEmpty()) {
            return List.of();
        }

        List<String> courseIds = courses.stream().map(Course::getCourseId).toList();

        List<Object[]> totalEnrollment = userCourseRepository.countEnrollmentByCourseIds(courseIds);

        //courseId + enrollment
        Map<String, Long> enrollmentMap = totalEnrollment.stream()
                .collect(Collectors.toMap(row -> (String) row[0], row -> (Long) row[1]));
                
        List<Object[]> totalUnitsData = courseRepository.countTotalUnitsByCourseIds(courseIds);
        Map<String, Long> unitsMap = totalUnitsData.stream()
                .collect(Collectors.toMap(row -> (String) row[0], row -> (Long) row[1]));

        List<Object[]> totalLessonsData = courseRepository.countTotalLessonsByCourseIds(courseIds);
        Map<String, Long> lessonsMap = totalLessonsData.stream()
                .collect(Collectors.toMap(row -> (String) row[0], row -> (Long) row[1]));

        return courses.stream().map(course -> {
            CourseDto temp = modelMapper.map(course, CourseDto.class);
            temp.setTotalUnits(Math.toIntExact(unitsMap.getOrDefault(course.getCourseId(), 0L)));
            temp.setTotalLessons(Math.toIntExact(lessonsMap.getOrDefault(course.getCourseId(), 0L)));
            temp.setTotalEnrollment(Math.toIntExact(enrollmentMap.getOrDefault(course.getCourseId(), 0L)));
            return temp;
        }).toList();
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
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));

        if (courseDto.getTitle() != null) {
            course.setTitle(courseDto.getTitle());
        }
        if (courseDto.getDescription() != null) {
            course.setDescription(courseDto.getDescription());
        }
        if (courseDto.getIconName() != null) {
            course.setIconName(courseDto.getIconName());
        }
        if (courseDto.getLevel() != null) {
            course.setLevel(CourseLevel.valueOf(courseDto.getLevel()));
        }
        if (courseDto.getStatus() != null) {
            course.setStatus(CourseStatus.valueOf(courseDto.getStatus()));
        }

        Course updatedCourse = courseRepository.save(course);
        return modelMapper.map(updatedCourse, CourseDto.class);
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
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));

        if (userCourseRepository.existsByCourse_CourseId(courseId)) {
            throw new BadRequestException("Khóa học đã có học viên tham gia, không thể xóa");
        }

        courseRepository.delete(course);
    }
}
