package edu.language.kbee.repository;

import edu.language.kbee.model.Flashcard;
import edu.language.kbee.model.FlashcardDeck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FlashcardRepository extends JpaRepository<Flashcard, String> {
    List<Flashcard> findByDeck(FlashcardDeck deck);
}
