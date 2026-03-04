package edu.language.kbee.service.impl;

import edu.language.kbee.enums.CourseStatus;
import edu.language.kbee.exception.ResourceNotFoundException;
import edu.language.kbee.model.Course;
import edu.language.kbee.model.User;
import edu.language.kbee.model.UserCourse;
import edu.language.kbee.payload.UserCourseDto;
import edu.language.kbee.repository.CourseRepository;
import edu.language.kbee.repository.UserCourseRepository;
import edu.language.kbee.service.UserCourseService;
import edu.language.kbee.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserCourseServiceImpl implements UserCourseService {

    private final UserCourseRepository userCourseRepository;
    private final CourseRepository courseRepository;
    private final AuthUtil authUtil;

    @Override
    public List<UserCourseDto> getUserCourses(String userId) {
        List<Course> allCourses = courseRepository.findAllByStatus(CourseStatus.PUBLISHED);
        List<UserCourse> enrolledCourses = userCourseRepository.findByUser_UserId(userId);

        Map<String, UserCourse> enrollmentMap = enrolledCourses.stream()
                .collect(Collectors.toMap(uc -> uc.getCourse().getCourseId(), uc -> uc));

        return allCourses.stream().map(course -> {
            UserCourse userProgress = enrollmentMap.get(course.getCourseId());

            if (userProgress != null) {
                UserCourseDto dto = mapToDto(userProgress);
                dto.setIsEnrolled(true);
                dto.setIsLocked(userProgress.isLocked());
                return dto;
            } else {
                return UserCourseDto.builder()
                        .courseId(course.getCourseId())
                        .title(course.getTitle())
                        .description(course.getDescription())
                        .iconName(course.getIconName())
                        .courseLevel(course.getLevel())
                        .progress(0)
                        .completedLessons(0)
                        .totalLessons(getTotalLesson(course))
                        .isLocked(false)
                        .isEnrolled(false)
                        .build();
            }
        }).collect(Collectors.toList());
    }

    @Override
    public UserCourseDto getMyCourseDetail(String courseId) {
        User user = authUtil.getLoggedInUser();
        String currentUserId = user.getUserId();

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));

        UserCourse enrolledCourse = userCourseRepository.findByUser_UserIdAndCourse_CourseId(currentUserId, courseId);

        if (enrolledCourse == null) {
            UserCourse newEnrollment = UserCourse.builder()
                    .course(course)
                    .user(user)
                    .completedLessons(0)
                    .progressPercentage(0)
                    .isCompleted(false)
                    .isLocked(false)
                    .build();

            UserCourse savedEnrollment = userCourseRepository.save(newEnrollment);

            UserCourseDto dto = mapToDto(savedEnrollment);
            dto.setIsEnrolled(true);
            return dto;
        } else {
            UserCourseDto dto = mapToDto(enrolledCourse);
            dto.setIsEnrolled(true);
            return dto;
        }
    }

    private UserCourseDto mapToDto(UserCourse userCourse) {
        return UserCourseDto.builder()
                .courseId(userCourse.getCourse().getCourseId())
                .title(userCourse.getCourse().getTitle())
                .description(userCourse.getCourse().getDescription())
                .iconName(userCourse.getCourse().getIconName())
                .courseLevel(userCourse.getCourse().getLevel())
                .progress(userCourse.getProgressPercentage())
                .completedLessons(userCourse.getCompletedLessons())
                .totalLessons(getTotalLesson(userCourse.getCourse()))
                .build();
    }

    private Integer getTotalLesson(Course course) {
        if (course.getUnits() == null) return 0;
        return course.getUnits().stream()
                .mapToInt(unit -> unit.getLessons() != null ? unit.getLessons().size() : 0)
                .sum();
    }
}