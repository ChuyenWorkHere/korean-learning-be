package edu.language.kbee.repository;

import edu.language.kbee.model.StudentSubmission;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubmissionRepository extends JpaRepository<StudentSubmission, String> {

    Optional<StudentSubmission> findTop1ByLesson_LessonIdAndBlock_BlockIdAndUser_UserIdOrderByCreatedDateDesc(String lessonId, String blockId, String userId);

    @EntityGraph(attributePaths = {"lesson", "block"})
    List<StudentSubmission> findByUser_UserIdOrderByCreatedDateDesc(String userId);

    boolean existsByLesson_LessonIdAndBlock_BlockId(String lessonId, String blockId);

}
