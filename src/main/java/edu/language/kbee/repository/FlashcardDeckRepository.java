package edu.language.kbee.repository;

import edu.language.kbee.model.FlashcardDeck;
import edu.language.kbee.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FlashcardDeckRepository extends JpaRepository<FlashcardDeck, String> {
    @EntityGraph(attributePaths = {"user"})
    List<FlashcardDeck> findByUserAndDeletedAtIsNull(User user);

    @EntityGraph(attributePaths = {"user"})
    List<FlashcardDeck> findByIsPublicTrueAndDeletedAtIsNull();

    @EntityGraph(attributePaths = {"user"})
    Page<FlashcardDeck> findByUserAndDeletedAtIsNull(User user, Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    Page<FlashcardDeck> findByIsPublicTrueAndDeletedAtIsNull(Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    Optional<FlashcardDeck> findByDeckIdAndDeletedAtIsNull(String deckId);
}
