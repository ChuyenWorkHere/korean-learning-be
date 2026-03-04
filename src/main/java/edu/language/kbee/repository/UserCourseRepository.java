package edu.language.kbee.repository;

import edu.language.kbee.model.UserCourse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserCourseRepository extends JpaRepository<UserCourse, String> {
    List<UserCourse> findByUser_UserId(String userId);
    UserCourse findByUser_UserIdAndCourse_CourseId(String userId, String courseId);

}