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
}