package edu.language.kbee.repository;

import edu.language.kbee.model.StudentSubmission;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SubmissionRepository extends JpaRepository<StudentSubmission, String> {

    Optional<StudentSubmission> findTop1ByLesson_LessonIdAndBlockIdAndUser_UserIdOrderByCreatedDateDesc(String lessonId, String blockId, String userId);

}
