package edu.language.kbee.repository;

import edu.language.kbee.enums.CourseStatus;
import edu.language.kbee.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<Course, String> {

    List<Course> findAllByStatus(CourseStatus status);

    @Query("SELECT c.courseId, COUNT(DISTINCT u.unitId) FROM Course c LEFT JOIN c.units u WHERE c.courseId IN :courseIds GROUP BY c.courseId")
    List<Object[]> countTotalUnitsByCourseIds(@Param("courseIds") List<String> courseIds);

    @Query("SELECT c.courseId, COUNT(l.lessonId) FROM Course c LEFT JOIN c.units u LEFT JOIN u.lessons l WHERE c.courseId IN :courseIds GROUP BY c.courseId")
    List<Object[]> countTotalLessonsByCourseIds(@Param("courseIds") List<String> courseIds);

}
