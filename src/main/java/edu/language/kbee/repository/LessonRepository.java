package edu.language.kbee.repository;

import edu.language.kbee.enums.LessonStatus;
import edu.language.kbee.model.Lesson;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, String> {

    List<Lesson> findByUnit_UnitIdOrderByOrderIndexAsc(String unitId);

    List<Lesson> findByUnit_Course_CourseIdOrderByOrderIndexAsc(String courseId);

    List<Lesson> findByUnit_Course_CourseIdOrderByUnit_OrderIndexAscOrderIndexAsc(String courseId);

    int countByUnit_UnitId(String unitId);


    @Query("SELECT l FROM Lesson l " +
            "WHERE l.unit.course.courseId = :courseId " +
            "AND l.status = :lessonStatus " +
            "AND ((l.unit.orderIndex = :unitOrderIndex AND l.orderIndex > :lessonOrderIndex) " +
            "     OR (l.unit.orderIndex > :unitOrderIndex)) " +
            "ORDER BY l.unit.orderIndex ASC, l.orderIndex ASC")
    List<Lesson> findNextLessonToLearn(
            @Param("courseId") String courseId,
            @Param("unitOrderIndex") Integer unitOrderIndex,
            @Param("lessonOrderIndex") Integer lessonOrderIndex,
            @Param("lessonStatus") LessonStatus lessonStatus,
            Pageable pageable
    );

    @Query("SELECT l.unit.course.courseId, COUNT(l.lessonId) " +
            "FROM Lesson l WHERE l.unit.course.courseId IN :courseIds " +
            "AND l.status = edu.language.kbee.enums.LessonStatus.PUBLISHED " +
            "GROUP BY l.unit.course.courseId")
    List<Object[]> countTotalPublishedLessonsByCourseIds(@Param("courseIds") List<String> courseIds);

    @Query("SELECT l.unit.course.courseId, COUNT(l.lessonId) " +
            "FROM Lesson l WHERE l.unit.course.courseId = :courseId " +
            "AND l.status = edu.language.kbee.enums.LessonStatus.PUBLISHED " +
            "GROUP BY l.unit.course.courseId")
    List<Object[]> countTotalPublishedLessonsByCourseId(@Param("courseId") String courseId);
}