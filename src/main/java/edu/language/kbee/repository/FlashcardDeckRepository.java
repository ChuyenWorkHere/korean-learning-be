package edu.language.kbee.repository;

import edu.language.kbee.model.FlashcardDeck;
import edu.language.kbee.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FlashcardDeckRepository extends JpaRepository<FlashcardDeck, String> {
    List<FlashcardDeck> findByUserAndDeletedAtIsNull(User user);
    List<FlashcardDeck> findByIsPublicTrueAndDeletedAtIsNull();

    Page<FlashcardDeck> findByUserAndDeletedAtIsNull(User user, Pageable pageable);
    Page<FlashcardDeck> findByIsPublicTrueAndDeletedAtIsNull(Pageable pageable);

    Optional<FlashcardDeck> findByDeckIdAndDeletedAtIsNull(String deckId);
}
