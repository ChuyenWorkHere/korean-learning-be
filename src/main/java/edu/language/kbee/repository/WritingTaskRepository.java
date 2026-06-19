package edu.language.kbee.repository;

import edu.language.kbee.model.WritingTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WritingTaskRepository extends JpaRepository<WritingTask, String> {
    Optional<WritingTask> findFirstByBlock_BlockIdAndTaskType(String blockId, edu.language.kbee.enums.SubmissionType taskType);

    @Query("SELECT w FROM WritingTask w JOIN FETCH w.block b WHERE b.blockId IN :blockIds")
    List<WritingTask> findByBlock_BlockIdIn(@Param("blockIds") List<String> blockIds);
}
