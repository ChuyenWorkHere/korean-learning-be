package edu.language.kbee.repository;

import edu.language.kbee.model.QuizOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizOptionRepository extends JpaRepository<QuizOption, String> {
    List<QuizOption> findByQuestion_QuestionIdOrderByOrderIndexAsc(String questionId);

    @Query("SELECT o FROM QuizOption o JOIN FETCH o.question q WHERE q.questionId IN :questionIds ORDER BY o.orderIndex ASC")
    List<QuizOption> findByQuestion_QuestionIdInOrderByOrderIndexAsc(@Param("questionIds") List<String> questionIds);
}
