package edu.language.kbee.repository;

import edu.language.kbee.model.DictationLesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DictationLessonRepository extends JpaRepository<DictationLesson, String> {
    List<DictationLesson> findByTopicIdOrderByCreatedDateAsc(String topicId);
}
