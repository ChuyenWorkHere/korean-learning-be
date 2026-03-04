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


    boolean existsByUser_UserIdAndLesson_LessonIdAndStatus(String userId, String lessonId, ProgressStatus status);


    @Query("SELECT lp FROM LessonProgress lp " +
            "WHERE lp.user.userId = :userId " +
            "AND lp.lesson.unit.course.courseId = :courseId")
    List<LessonProgress> findAllByUserIdAndCourseId(
            @Param("userId") String userId,
            @Param("courseId") String courseId
    );

    @Query("SELECT COUNT(lp) FROM LessonProgress lp " +
            "WHERE lp.user.userId = :userId " +
            "AND lp.lesson.unit.course.courseId = :courseId " +
            "AND lp.status = :status")
    long countByUserIdAndCourseIdAndStatus(
            @Param("userId") String userId,
            @Param("courseId") String courseId,
            @Param("status") ProgressStatus status
    );

    List<LessonProgress> findByUser_UserIdOrderByLastModifiedDateDesc(String userId);
}