package edu.language.kbee.repository;

import edu.language.kbee.model.Flashcard;
import edu.language.kbee.model.FlashcardDeck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FlashcardRepository extends JpaRepository<Flashcard, String> {
    List<Flashcard> findByDeck(FlashcardDeck deck);

    @Query("SELECT f FROM Flashcard f JOIN FETCH f.deck d WHERE d = :deck AND f.cardId IN :cardIds")
    List<Flashcard> findByDeckAndCardIds(
            @Param("deck") FlashcardDeck deck,
            @Param("cardIds") List<String> cardIds
    );

    @Query("SELECT f FROM Flashcard f JOIN FETCH f.deck d WHERE d.deckId IN :deckIds")
    List<Flashcard> findByDeck_DeckIdIn(@Param("deckIds") List<String> deckIds);
}
