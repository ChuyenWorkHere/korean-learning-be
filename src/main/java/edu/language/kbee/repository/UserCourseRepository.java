package edu.language.kbee.repository;

import edu.language.kbee.model.UserCourse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserCourseRepository extends JpaRepository<UserCourse, String> {
    List<UserCourse> findByUser_UserId(String userId);
    UserCourse findByUser_UserIdAndCourse_CourseId(String userId, String courseId);

    @Query("SELECT uc.course.courseId, COUNT(uc.id) " +
            "FROM UserCourse uc " +
            "WHERE uc.course.courseId IN :courseIds " +
            "GROUP BY uc.course.courseId ")
    List<Object[]> countEnrollmentByCourseIds(@Param("courseIds") List<String> courseIds);
}