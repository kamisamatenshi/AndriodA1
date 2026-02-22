package com.koi.thepiece.data.db

import androidx.room.*

data class DeckWithCards(
    @Embedded val deck: DeckEntity,
    @Relation(
        parentColumn = "deckId",
        entityColumn = "deckId",
        entity = DeckCardEntity::class
    )
    val cards: List<DeckCardEntity>
)

@Dao
interface DeckDao {

    @Insert
    suspend fun insertDeck(deck: DeckEntity): Long

    @Update
    suspend fun updateDeck(deck: DeckEntity)

    @Query("DELETE FROM deck_cards WHERE deckId = :deckId")
    suspend fun clearDeckCards(deckId: Long)

    @Insert
    suspend fun insertDeckCards(cards: List<DeckCardEntity>)

    @Transaction
    @Query("SELECT * FROM decks ORDER BY updatedAtEpochMs DESC")
    suspend fun getAllDecks(): List<DeckWithCards>

    @Transaction
    @Query("SELECT * FROM decks ORDER BY updatedAtEpochMs DESC")
    fun observeAllDecks(): kotlinx.coroutines.flow.Flow<List<DeckWithCards>>

    @Transaction
    @Query("SELECT * FROM decks WHERE deckId = :deckId")
    suspend fun getDeck(deckId: Long): DeckWithCards?

    @Query("DELETE FROM decks WHERE deckId = :deckId")
    suspend fun deleteDeck(deckId: Long)

    @Query("DELETE FROM deck_cards WHERE deckId = :deckId")
    suspend fun deleteDeckCards(deckId: Long)

    @Query("SELECT serverDeckId FROM decks WHERE deckId = :deckId")
    suspend fun getServerDeckId(deckId: Long): Long?

    @Query("SELECT * FROM decks WHERE serverDeckId = :serverDeckId LIMIT 1")
    suspend fun getDeckByServerId(serverDeckId: Long): DeckEntity?

    @Transaction
    suspend fun upsertDeckByServerId(
        serverDeckId: Long,
        name: String,
        leaderCardId: Int,
        updatedAtEpochMs: Long,
        shareCode : String?,
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

        clearDeckCards(localDeckId)
        insertDeckCards(cards.map { (cardId, qty) ->
            DeckCardEntity(deckId = localDeckId, cardId = cardId, qty = qty)
        })
    }

    @Query("""
    DELETE FROM decks
    WHERE serverDeckId IS NOT NULL
      AND serverDeckId NOT IN (:serverIds)
    """)
    suspend fun deleteDecksNotInServer(serverIds: List<Long>)

    @Query("DELETE FROM decks WHERE serverDeckId IS NOT NULL")
    suspend fun deleteAllServerDecks()
}