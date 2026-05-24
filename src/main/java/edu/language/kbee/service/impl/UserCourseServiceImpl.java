package edu.language.kbee.service.impl;

import edu.language.kbee.enums.CourseStatus;
import edu.language.kbee.exception.ResourceNotFoundException;
import edu.language.kbee.model.Course;
import edu.language.kbee.model.LessonProgress;
import edu.language.kbee.model.User;
import edu.language.kbee.model.UserCourse;
import edu.language.kbee.payload.UserCourseDto;
import edu.language.kbee.repository.CourseRepository;
import edu.language.kbee.repository.LessonProgressRepository;
import edu.language.kbee.repository.LessonRepository;
import edu.language.kbee.repository.UserCourseRepository;
import edu.language.kbee.service.UserCourseService;
import edu.language.kbee.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserCourseServiceImpl implements UserCourseService {

    private final UserCourseRepository userCourseRepository;
    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final LessonProgressRepository progressRepository;
    private final AuthUtil authUtil;

    @Override
    public List<UserCourseDto> getUserCourses(String userId) {

        List<Course> allCourses = courseRepository.findAllByStatus(CourseStatus.PUBLISHED);
        List<UserCourse> enrolledCourses = userCourseRepository.findByUser_UserId(userId);

        List<String> courseIds = allCourses.stream()
                .map(Course::getCourseId)
                .toList();

        Map<String, UserCourse> enrollmentMap = enrolledCourses.stream()
                .collect(Collectors.toMap(uc -> uc.getCourse().getCourseId(), uc -> uc));

        Map<String, Long> enrollmentCountMap = userCourseRepository.countEnrollmentByCourseIds(courseIds)
                .stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> (Long) row[1]
                ));

        Map<String, Long> totalLessonsMap = lessonRepository.countTotalPublishedLessonsByCourseIds(courseIds)
                .stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],   // courseId
                        row -> (Long) row[1]      // count
                ));

        Map<String, Long> completedLessonsMap = progressRepository.countCompletedLessonsByUserAndCourseIds(userId, courseIds)
                .stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],   // courseId
                        row -> (Long) row[1]      // count
                ));

        return allCourses.stream().map(course -> {
            String courseId = course.getCourseId();
            UserCourse userProgress = enrollmentMap.get(courseId);

            int totalLessons = totalLessonsMap.getOrDefault(courseId, 0L).intValue();
            int completedLessons = completedLessonsMap.getOrDefault(courseId, 0L).intValue();
            int totalEnrollment = enrollmentCountMap.getOrDefault(courseId, 0L).intValue();

            int progressPercent = (totalLessons == 0) ? 0 : (completedLessons * 100) / totalLessons;

            if (userProgress != null) {
                UserCourseDto dto = mapToDto(userProgress);
                dto.setIsEnrolled(true);
                dto.setProgress(progressPercent);
                dto.setCompletedLessons(completedLessons);
                dto.setTotalLessons(totalLessons);
                dto.setTotalEnrollment(totalEnrollment);
                dto.setIsLocked(false);
                return dto;
            } else {
                return UserCourseDto.builder()
                        .courseId(course.getCourseId())
                        .title(course.getTitle())
                        .description(course.getDescription())
                        .imageUrl(course.getImageUrl())
                        .courseLevel(course.getLevel())
                        .progress(0)
                        .completedLessons(0)
                        .totalLessons(totalLessons)
                        .totalEnrollment(totalEnrollment)
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

        Map<String, Long> totalLessonsMap = lessonRepository.countTotalPublishedLessonsByCourseId(courseId)
                .stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],   // courseId
                        row -> (Long) row[1]      // count
                ));

        Map<String, Long> completedLessonsMap = progressRepository.countCompletedLessonsByUserAndCourseId(currentUserId, courseId)
                .stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],   // courseId
                        row -> (Long) row[1]      // count
                ));

        Map<String, Long> enrollmentCountMap = userCourseRepository.countEnrollmentByCourseIds(List.of(courseId))
                .stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> (Long) row[1]
                ));

        UserCourse enrolledCourse = userCourseRepository.findByUser_UserIdAndCourse_CourseId(currentUserId, courseId);

        int totalLessons = totalLessonsMap.getOrDefault(courseId, 0L).intValue();
        int completedLessons = completedLessonsMap.getOrDefault(courseId, 0L).intValue();
        int totalEnrollment = enrollmentCountMap.getOrDefault(courseId, 0L).intValue();

        int progressPercent = (totalLessons == 0) ? 0 : (completedLessons * 100) / totalLessons;

        if (enrolledCourse == null) {
            UserCourse newEnrollment = UserCourse.builder()
                    .course(course)
                    .user(user)
                    .build();

            UserCourse savedEnrollment = userCourseRepository.save(newEnrollment);

            UserCourseDto dto = mapToDto(savedEnrollment);
            dto.setIsEnrolled(true);
            dto.setProgress(progressPercent);
            dto.setCompletedLessons(completedLessons);
            dto.setTotalLessons(totalLessons);
            dto.setTotalEnrollment(totalEnrollment);
            dto.setIsLocked(false);
            return dto;
        } else {
            UserCourseDto dto = mapToDto(enrolledCourse);
            dto.setIsEnrolled(true);
            dto.setProgress(progressPercent);
            dto.setCompletedLessons(completedLessons);
            dto.setTotalLessons(totalLessons);
            dto.setTotalEnrollment(totalEnrollment);
            dto.setIsLocked(false);
            return dto;
        }
    }

    @Override
    public UserCourseDto getRecentCourse(String userId) {
        // Find most recent lesson interaction
        java.util.Optional<LessonProgress> latestProgress = progressRepository.findFirstByUser_UserIdOrderByLastModifiedDateDesc(userId);
        
        String recentCourseId = null;
        if (latestProgress.isPresent()) {
            recentCourseId = latestProgress.get().getLesson().getUnit().getCourse().getCourseId();
        } else {
            // Fallback to most recent enrolled course
            java.util.Optional<UserCourse> latestEnrollment = userCourseRepository.findFirstByUser_UserIdOrderByLastModifiedDateDesc(userId);
            if (latestEnrollment.isPresent()) {
                recentCourseId = latestEnrollment.get().getCourse().getCourseId();
            }
        }

        if (recentCourseId != null) {
            return getMyCourseDetail(recentCourseId);
        }
        return null;
    }

    private UserCourseDto mapToDto(UserCourse userCourse) {
        return UserCourseDto.builder()
                .courseId(userCourse.getCourse().getCourseId())
                .title(userCourse.getCourse().getTitle())
                .description(userCourse.getCourse().getDescription())
                .imageUrl(userCourse.getCourse().getImageUrl())
                .courseLevel(userCourse.getCourse().getLevel())
                .build();
    }

}
