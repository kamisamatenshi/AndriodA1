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
    val updatedAtEpochMs: Long,
    val sharecode: String?
)

class DeckListViewModel(app: Application ,private val tokenStore: TokenStore ) : AndroidViewModel(app) {

    private val deckRepo = AppGraph.provideDeckRepository(app)
    private val catalogRepo = AppGraph.provideCatalogRepository(app)

    private val allCardsFlow: Flow<List<Card>> = catalogRepo.observeCards()

    private val _snackbar = MutableSharedFlow<String>()
    val snackbar = _snackbar.asSharedFlow()

    init{
        viewModelScope.launch {
            val token = tokenStore.tokenFlow.firstOrNull()?.trim().orEmpty()
            if (token.isNotEmpty()) {
                runCatching { deckRepo.syncOwnedDecksFromServer(token) }
            }
        }
    }

    fun refreshDecksFromServer() {
        viewModelScope.launch {
            val token = tokenStore.tokenFlow.firstOrNull()?.trim().orEmpty()
            if (token.isNotEmpty()) {
                runCatching { deckRepo.syncOwnedDecksFromServer(token) }
            }
        }
    }

    fun importDeckByShareCode(code: String) {
        viewModelScope.launch {
            runCatching {
                val token = tokenStore.tokenFlow.firstOrNull()?.trim().orEmpty()
                val resp = deckRepo.importDeckByShareCode(token, code)

                if (resp.alreadyOwned == true) {
                    _snackbar.emit("Already owned!")
                } else {
                    _snackbar.emit("Imported successfully!")
                    deckRepo.syncOwnedDecksFromServer(token)
                }
            }.onFailure {
                _snackbar.emit(it.message ?: "Import failed")
            }
        }
    }

    fun deleteDeck(deckId: Long) {
        viewModelScope.launch {
            val token =tokenStore.tokenFlow.firstOrNull()?.trim().orEmpty()
            deckRepo.deleteDeckServerFirst(token,deckId)
        }
    }

    val decksUi: StateFlow<List<DeckListItemUi>> =
        combine(
            deckRepo.observeAllDecks(),
            allCardsFlow,
            deckRepo.shareCodeMap
        ) { decks, cards ,shareMap ->
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
                    updatedAtEpochMs = d.deck.updatedAtEpochMs,
                    sharecode = shareMap[d.deck.serverDeckId]
                )
            }
        }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}