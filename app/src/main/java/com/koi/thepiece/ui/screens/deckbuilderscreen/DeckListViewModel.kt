package com.koi.thepiece.ui.screens.deckbuilderscreen

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.koi.thepiece.AppGraph
import com.koi.thepiece.data.local.TokenStore
import com.koi.thepiece.data.model.Card
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DeckListItemUi(
    val deckId: Long,
    val name: String,
    val leaderCardId: Int,
    val leaderName: String?,
    val leaderImageUrl: String?,
    val totalCards: Int,
    val updatedAtEpochMs: Long
)

class DeckListViewModel(app: Application ,private val tokenStore: TokenStore ) : AndroidViewModel(app) {

    private val deckRepo = AppGraph.provideDeckRepository(app)
    private val catalogRepo = AppGraph.provideCatalogRepository(app)

    private val allCardsFlow: Flow<List<Card>> = catalogRepo.observeCards()

    fun deleteDeck(deckId: Long) {
        viewModelScope.launch {
            deckRepo.deleteDeck(deckId)
        }
    }

    val decksUi: StateFlow<List<DeckListItemUi>> =
        combine(deckRepo.observeAllDecks(), allCardsFlow) { decks, cards ->
            val cardById = cards.associateBy { it.id }

            decks.map { d ->
                val leader = cardById[d.deck.leaderCardId]
                DeckListItemUi(
                    deckId = d.deck.deckId,
                    name = d.deck.name,
                    leaderCardId = d.deck.leaderCardId,
                    leaderName = leader?.name,
                    leaderImageUrl = leader?.imageUrl,
                    totalCards = d.cards.sumOf { it.qty },
                    updatedAtEpochMs = d.deck.updatedAtEpochMs
                )
            }
        }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}