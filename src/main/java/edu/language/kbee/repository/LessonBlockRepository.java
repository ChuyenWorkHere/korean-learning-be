package edu.language.kbee.repository;

import edu.language.kbee.model.LessonBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LessonBlockRepository extends JpaRepository<LessonBlock, String> {
    List<LessonBlock> findByLesson_LessonIdOrderByOrderIndexAsc(String lessonId);
}
