package com.koi.thepiece.data.repo

import com.koi.thepiece.data.db.DeckCardEntity
import com.koi.thepiece.data.db.DeckDao
import com.koi.thepiece.data.db.DeckEntity
import com.koi.thepiece.data.db.DeckWithCards
import com.koi.thepiece.ui.screens.deckbuilderscreen.QtyClass

class DeckRepository(private val deckDao: DeckDao) {

    suspend fun getDeck(deckId: Long): DeckWithCards? = deckDao.getDeck(deckId)

    fun observeAllDecks(): kotlinx.coroutines.flow.Flow<List<DeckWithCards>> {
        return deckDao.observeAllDecks()
    }

    suspend fun saveNewDeck(
        name: String,
        leaderCardId: Int,
        deckMap: Map<Int, QtyClass> // cardId -> qty
    ): Long {
        val now = System.currentTimeMillis()

        val deckId = deckDao.insertDeck(
            DeckEntity(
                name = name,
                leaderCardId = leaderCardId,
                createdAtEpochMs = now,
                updatedAtEpochMs = now
            )
        )

        val deckCards = deckMap.map { (cardId, qty) ->
            DeckCardEntity(deckId = deckId, cardId = cardId, qty.requiredQty)
        }
        deckDao.insertDeckCards(deckCards)

        return deckId
    }

    suspend fun overwriteExistingDeck(
        deckId: Long,
        name: String,
        leaderCardId: Int,
        deckMap: Map<Int, QtyClass>
    ) {
        val now = System.currentTimeMillis()

        deckDao.updateDeck(
            DeckEntity(
                deckId = deckId,
                name = name,
                leaderCardId = leaderCardId,
                createdAtEpochMs = now, // if want keep original, fetch first instead
                updatedAtEpochMs = now
            )
        )

        deckDao.clearDeckCards(deckId)

        val deckCards = deckMap.map { (cardId, qty) ->
            DeckCardEntity(deckId = deckId, cardId = cardId, qty = qty.requiredQty)
        }
        deckDao.insertDeckCards(deckCards)
    }

    suspend fun deleteDeck(deckId: Long) {
        deckDao.clearDeckCards(deckId)   // delete child rows first
        deckDao.deleteDeck(deckId)       // then delete deck
    }
}