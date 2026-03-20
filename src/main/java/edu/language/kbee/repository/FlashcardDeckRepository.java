package edu.language.kbee.repository;

import edu.language.kbee.model.FlashcardDeck;
import edu.language.kbee.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FlashcardDeckRepository extends JpaRepository<FlashcardDeck, String> {
    List<FlashcardDeck> findByUser(User user);
    List<FlashcardDeck> findByIsPublicTrue();
}
