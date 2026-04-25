package edu.language.kbee.repository;

import edu.language.kbee.model.DictationTopic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DictationTopicRepository extends JpaRepository<DictationTopic, String> {
}
