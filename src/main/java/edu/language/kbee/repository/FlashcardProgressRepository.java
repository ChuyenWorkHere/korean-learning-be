package edu.language.kbee.repository;

import edu.language.kbee.model.Flashcard;
import edu.language.kbee.model.FlashcardDeck;
import edu.language.kbee.model.FlashcardProgress;
import edu.language.kbee.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FlashcardProgressRepository extends JpaRepository<FlashcardProgress, String> {

    Optional<FlashcardProgress> findByUserAndFlashcard(User user, Flashcard flashcard);

    @Query("SELECT fp FROM FlashcardProgress fp " +
           "JOIN FETCH fp.flashcard f " +
           "WHERE fp.user = :user AND f.cardId IN :cardIds")
    List<FlashcardProgress> findByUserAndFlashcardCardIds(
            @Param("user") User user,
            @Param("cardIds") List<String> cardIds
    );

    long countByFlashcard_Deck(FlashcardDeck deck);

    Long countByUserAndFlashcard_Deck(User user, FlashcardDeck deck);

    @Query("SELECT fp.flashcard.cardId FROM FlashcardProgress fp WHERE fp.user = :user AND fp.flashcard.deck = :deck")
    List<String> findCardIdsWithProgress(@Param("user") User user, @Param("deck") FlashcardDeck deck);

    // Get cards in a deck that have progress (either learning or due)
    @Query("SELECT fp FROM FlashcardProgress fp " +
           "JOIN FETCH fp.flashcard f " +
           "WHERE fp.user = :user AND f.deck = :deck " +
           "AND fp.nextReviewDate <= :currentTime")
    List<FlashcardProgress> findDueCardsForUserAndDeck(
        @Param("user") User user, 
        @Param("deck") FlashcardDeck deck, 
        @Param("currentTime") LocalDateTime currentTime
    );
    
    // Get count of mastered cards (boxLevel >= 5 logic based)
    @Query("SELECT COUNT(fp) FROM FlashcardProgress fp " +
           "WHERE fp.user = :user AND fp.flashcard.deck = :deck AND fp.boxLevel >= 5")
    Long countMasteredCards(@Param("user") User user, @Param("deck") FlashcardDeck deck);

    @Query("SELECT fp.flashcard.deck.deckId, COUNT(fp) " +
           "FROM FlashcardProgress fp " +
           "WHERE fp.user = :user AND fp.flashcard.deck.deckId IN :deckIds " +
           "GROUP BY fp.flashcard.deck.deckId")
    List<Object[]> countLearnedByUserAndDeckIds(@Param("user") User user, @Param("deckIds") List<String> deckIds);
}
