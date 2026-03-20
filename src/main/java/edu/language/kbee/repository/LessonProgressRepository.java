package edu.language.kbee.repository;

import edu.language.kbee.enums.ProgressStatus;
import edu.language.kbee.model.LessonProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LessonProgressRepository extends JpaRepository<LessonProgress, Long> {


    Optional<LessonProgress> findByUser_UserIdAndLesson_LessonId(String userId, String lessonId);


    @Query("SELECT lp FROM LessonProgress lp " +
            "WHERE lp.user.userId = :userId " +
            "AND lp.lesson.unit.course.courseId = :courseId")
    List<LessonProgress> findAllByUserIdAndCourseId(
            @Param("userId") String userId,
            @Param("courseId") String courseId
    );

    @Query("SELECT l.unit.course.courseId, COUNT(ul.id) " +
            "FROM LessonProgress ul JOIN ul.lesson l " +
            "WHERE ul.user.userId = :userId AND ul.status = edu.language.kbee.enums.ProgressStatus.COMPLETED " +
            "AND l.unit.course.courseId IN :courseIds " +
            "AND l.status = edu.language.kbee.enums.LessonStatus.PUBLISHED " +
            "GROUP BY l.unit.course.courseId")
    List<Object[]> countCompletedLessonsByUserAndCourseIds(@Param("userId") String userId, @Param("courseIds") List<String> courseIds);

    @Query("SELECT l.unit.course.courseId, COUNT(ul.id) " +
            "FROM LessonProgress ul JOIN ul.lesson l " +
            "WHERE ul.user.userId = :userId AND ul.status = edu.language.kbee.enums.ProgressStatus.COMPLETED " +
            "AND l.unit.course.courseId = :courseId " +
            "AND l.status = edu.language.kbee.enums.LessonStatus.PUBLISHED " +
            "GROUP BY l.unit.course.courseId")
    List<Object[]> countCompletedLessonsByUserAndCourseId(@Param("userId") String userId, @Param("courseId") String courseId);

    boolean existsByUser_UserIdAndLesson_LessonId(String userId, String lessonId);
}