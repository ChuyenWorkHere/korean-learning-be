package edu.language.kbee.repository;

import edu.language.kbee.model.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, String> {
    List<QuizQuestion> findByBlock_BlockIdOrderByOrderIndexAsc(String blockId);

    @Query("SELECT q FROM QuizQuestion q JOIN FETCH q.block b WHERE b.blockId IN :blockIds ORDER BY q.orderIndex ASC")
    List<QuizQuestion> findByBlock_BlockIdInOrderByOrderIndexAsc(@Param("blockIds") List<String> blockIds);
}
