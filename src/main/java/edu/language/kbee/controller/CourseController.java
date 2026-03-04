package edu.language.kbee.controller;

import edu.language.kbee.model.User;
import edu.language.kbee.payload.CourseDto;
import edu.language.kbee.payload.UserCourseDto;
import edu.language.kbee.service.CourseService;
import edu.language.kbee.service.UserCourseService;
import edu.language.kbee.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/courses")
public class CourseController {

    private final CourseService courseService;
    private final UserCourseService userCourseService;
    private final AuthUtil authUtil;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<?> getAllCourses() {
        List<CourseDto> courseDtos = courseService.getAllCourses();
        return ResponseEntity.ok(courseDtos);
    }

    @GetMapping("/my-courses")
    public ResponseEntity<?> getMyCourses() {

        User user = authUtil.getLoggedInUser();

        List<UserCourseDto> userCourseDtos = userCourseService.getUserCourses(user.getUserId());
        return ResponseEntity.ok(userCourseDtos);
    }

    @GetMapping("/{courseId}")
    public ResponseEntity<?> getCourseById(@PathVariable(name = "courseId") String courseId) {
        CourseDto courseDto = courseService.getCourseById(courseId);
        return ResponseEntity.ok(courseDto);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<?> createCourse(@RequestBody CourseDto courseDto) {
        CourseDto createdCourse = courseService.createCourse(courseDto);
        return ResponseEntity.ok(createdCourse);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{courseId}/status")
    public ResponseEntity<?> updateCourseStatus(@PathVariable(name = "courseId") String courseId) {
        CourseDto updatedCourse = courseService.toggleCourseStatus(courseId);
        return ResponseEntity.ok(updatedCourse);
    }

    @GetMapping("/my-courses/{courseId}")
    public ResponseEntity<?> getMyCourseDetail(@PathVariable(name = "courseId") String courseId) {

        UserCourseDto userCourseDto = userCourseService.getMyCourseDetail(courseId);
        return ResponseEntity.ok(userCourseDto);
    }

}
