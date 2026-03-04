package edu.language.kbee.service;

import edu.language.kbee.payload.UserCourseDto;

import java.util.List;

public interface UserCourseService {

    List<UserCourseDto> getUserCourses(String userId);

    UserCourseDto getMyCourseDetail(String courseId);
}
