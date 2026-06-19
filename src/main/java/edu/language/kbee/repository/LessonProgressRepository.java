package edu.language.kbee.repository;

import edu.language.kbee.enums.ProgressStatus;
import edu.language.kbee.model.LessonProgress;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import edu.language.kbee.enums.LessonStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface LessonProgressRepository extends JpaRepository<LessonProgress, Long> {


    @EntityGraph(attributePaths = {"lesson", "lesson.unit", "lesson.unit.course"})
    Optional<LessonProgress> findFirstByUser_UserIdOrderByLastModifiedDateDesc(String userId);

    Optional<LessonProgress> findByUser_UserIdAndLesson_LessonId(String userId, String lessonId);

    @EntityGraph(attributePaths = {"lesson", "lesson.unit", "lesson.unit.course"})
    List<LessonProgress> findByUser_UserIdAndStatusOrderByCompletedAtDesc(String userId, ProgressStatus status);

    @Query("SELECT lp FROM LessonProgress lp " +
            "JOIN FETCH lp.lesson l " +
            "JOIN FETCH l.unit u " +
            "JOIN FETCH u.course c " +
            "WHERE lp.user.userId = :userId " +
            "AND c.courseId = :courseId")
    List<LessonProgress> findAllByUserIdAndCourseId(
            @Param("userId") String userId,
            @Param("courseId") String courseId
    );

    @Query("SELECT l.unit.course.courseId, COUNT(ul.id) " +
            "FROM LessonProgress ul JOIN ul.lesson l " +
            "WHERE ul.user.userId = :userId AND ul.status = ProgressStatus.COMPLETED " +
            "AND l.unit.course.courseId IN :courseIds " +
            "AND l.status = LessonStatus.PUBLISHED " +
            "GROUP BY l.unit.course.courseId")
    List<Object[]> countCompletedLessonsByUserAndCourseIds(@Param("userId") String userId, @Param("courseIds") List<String> courseIds);

    @Query("SELECT l.unit.course.courseId, COUNT(ul.id) " +
            "FROM LessonProgress ul JOIN ul.lesson l " +
            "WHERE ul.user.userId = :userId AND ul.status = ProgressStatus.COMPLETED " +
            "AND l.unit.course.courseId = :courseId " +
            "AND l.status = LessonStatus.PUBLISHED " +
            "GROUP BY l.unit.course.courseId")
    List<Object[]> countCompletedLessonsByUserAndCourseId(@Param("userId") String userId, @Param("courseId") String courseId);

    boolean existsByUser_UserIdAndLesson_LessonId(String userId, String lessonId);

    boolean existsByLesson_LessonId(String lessonId);

    boolean existsByLesson_Unit_UnitId(String unitId);

    @Query("SELECT COUNT(lp.id) FROM LessonProgress lp WHERE lp.user.userId = :userId AND lp.status = ProgressStatus.COMPLETED")
    Long countAllCompletedLessonsByUserId(@Param("userId") String userId);

    @Query("SELECT COUNT(lp) FROM LessonProgress lp WHERE lp.status = ProgressStatus.COMPLETED")
    long countAllCompletedLessons();
}
