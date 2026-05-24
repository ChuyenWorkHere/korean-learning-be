package edu.language.kbee.repository;

import edu.language.kbee.model.WritingTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WritingTaskRepository extends JpaRepository<WritingTask, String> {
    Optional<WritingTask> findFirstByBlock_BlockIdAndTaskType(String blockId, edu.language.kbee.enums.SubmissionType taskType);
}
