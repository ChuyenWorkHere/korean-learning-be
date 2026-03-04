package edu.language.kbee.repository;

import edu.language.kbee.model.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, String> {

    List<Lesson> findByUnit_UnitIdOrderByOrderIndexAsc(String unitId);

    List<Lesson> findByUnit_Course_CourseIdOrderByOrderIndexAsc(String courseId);

    int countByUnit_UnitId(String unitId);

    Optional<Lesson> findByUnit_UnitIdAndOrderIndex(String unitId, Integer orderIndex);
}