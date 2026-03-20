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

    Long countByUserAndFlashcard_Deck(User user, FlashcardDeck deck);

    // Get cards in a deck that have progress (either learning or due)
    @Query("SELECT fp FROM FlashcardProgress fp " +
           "WHERE fp.user = :user AND fp.flashcard.deck = :deck " +
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
}
