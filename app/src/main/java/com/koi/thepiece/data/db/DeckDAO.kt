package com.koi.thepiece.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Composite data model representing a deck and its associated cards.
 *
 * Uses Room's @Relation to automatically fetch all DeckCardEntity
 * entries linked to a DeckEntity via deckId.
 *
 * This enables convenient retrieval of full deck composition
 * in a single transactional query.
 */
data class DeckWithCards(

    /** Embedded deck metadata. */
    @Embedded val deck: DeckEntity,

    /**
     * All cards associated with this deck.
     * Retrieved via deckId foreign key relation.
     */
    @Relation(
        parentColumn = "deckId",
        entityColumn = "deckId",
        entity = DeckCardEntity::class
    )
    val cards: List<DeckCardEntity>
)

/**
 * Data Access Object for deck-related database operations.
 *
 * Responsible for:
 * - Local deck persistence
 * - Deck-card relationship management
 * - Synchronization with server decks
 * - Reactive deck observation
 */
@Dao
interface DeckDao {

    /** Inserts a new deck and returns its generated local ID. */
    @Insert
    suspend fun insertDeck(deck: DeckEntity): Long

    /** Updates existing deck metadata. */
    @Update
    suspend fun updateDeck(deck: DeckEntity)

    /** Removes all card entries belonging to a specific deck. */
    @Query("DELETE FROM deck_cards WHERE deckId = :deckId")
    suspend fun clearDeckCards(deckId: Long)

    /** Inserts multiple deck-card relationships. */
    @Insert
    suspend fun insertDeckCards(cards: List<DeckCardEntity>)

    /**
     * Retrieves all decks with their associated cards.
     * Ordered by last updated timestamp (descending).
     */
    @Transaction
    @Query("SELECT * FROM decks ORDER BY updatedAtEpochMs DESC")
    suspend fun getAllDecks(): List<DeckWithCards>

    /**
     * Observes all decks reactively.
     * Used for real-time UI updates.
     */
    @Transaction
    @Query("SELECT * FROM decks ORDER BY updatedAtEpochMs DESC")
    fun observeAllDecks(): Flow<List<DeckWithCards>>

    /** Retrieves a specific deck with its card list. */
    @Transaction
    @Query("SELECT * FROM decks WHERE deckId = :deckId")
    suspend fun getDeck(deckId: Long): DeckWithCards?

    /** Deletes a deck by its local ID. */
    @Query("DELETE FROM decks WHERE deckId = :deckId")
    suspend fun deleteDeck(deckId: Long)

    /** Deletes all deck-card entries associated with a deck. */
    @Query("DELETE FROM deck_cards WHERE deckId = :deckId")
    suspend fun deleteDeckCards(deckId: Long)

    /** Retrieves the server-side deck ID associated with a local deck. */
    @Query("SELECT serverDeckId FROM decks WHERE deckId = :deckId")
    suspend fun getServerDeckId(deckId: Long): Long?

    /** Retrieves a local deck using its server-side ID. */
    @Query("SELECT * FROM decks WHERE serverDeckId = :serverDeckId LIMIT 1")
    suspend fun getDeckByServerId(serverDeckId: Long): DeckEntity?

    /**
     * Inserts or updates a deck based on serverDeckId.
     *
     * Used during synchronization to:
     * - Insert new decks received from server
     * - Update existing decks
     * - Replace card composition safely
     *
     * This operation runs inside a Room transaction to ensure
     * atomicity and data consistency.
     */
    @Transaction
    suspend fun upsertDeckByServerId(
        serverDeckId: Long,
        name: String,
        leaderCardId: Int,
        updatedAtEpochMs: Long,
        shareCode: String?,
        cards: List<Pair<Int, Int>> // cardId, qty
    ) {
        val existing = getDeckByServerId(serverDeckId)
        val now = System.currentTimeMillis()

        val localDeckId = if (existing == null) {
            insertDeck(
                DeckEntity(
                    serverDeckId = serverDeckId,
                    name = name,
                    leaderCardId = leaderCardId,
                    createdAtEpochMs = now,
                    updatedAtEpochMs = updatedAtEpochMs
                )
            )
        } else {
            updateDeck(
                existing.copy(
                    name = name,
                    leaderCardId = leaderCardId,
                    updatedAtEpochMs = updatedAtEpochMs
                )
            )
            existing.deckId
        }

        // Replace card composition
        clearDeckCards(localDeckId)
        insertDeckCards(cards.map { (cardId, qty) ->
            DeckCardEntity(deckId = localDeckId, cardId = cardId, qty = qty)
        })
    }

    /**
     * Deletes all local decks whose server IDs are not present
     * in the latest server-provided list.
     *
     * Used for server-to-local synchronization cleanup.
     */
    @Query("""
        DELETE FROM decks
        WHERE serverDeckId IS NOT NULL
          AND serverDeckId NOT IN (:serverIds)
    """)
    suspend fun deleteDecksNotInServer(serverIds: List<Long>)

    /** Deletes all decks that originated from the server. */
    @Query("DELETE FROM decks WHERE serverDeckId IS NOT NULL")
    suspend fun deleteAllServerDecks()
}