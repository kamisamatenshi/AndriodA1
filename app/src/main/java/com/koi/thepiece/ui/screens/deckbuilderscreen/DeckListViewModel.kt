package com.koi.thepiece.ui.screens.deckbuilderscreen

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.koi.thepiece.AppGraph
import com.koi.thepiece.data.local.TokenStore
import com.koi.thepiece.data.model.Card
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * UI projection model for rendering a single deck item in the deck list screen.
 *
 * This is a presentation-layer model derived from:
 * - Deck entity (Room / server)
 * - Leader card metadata (resolved from catalogue cache)
 * - Share code mapping
 */
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

/**
 * ViewModel for the "My Decks" screen.
 *
 * Responsibilities:
 * - Sync owned decks from server
 * - Expose combined UI state for deck list
 * - Handle import/delete actions
 * - Emit snackbar feedback events
 */
class DeckListViewModel(app: Application ,private val tokenStore: TokenStore ) : AndroidViewModel(app) {

    /** Repository for deck persistence and server sync */
    private val deckRepo = AppGraph.provideDeckRepository(app)

    /** Repository for card metadata (leader name/image resolution) */
    private val catalogRepo = AppGraph.provideCatalogRepository(app)

    /** Observes full card list from Room (offline-first source) */
    private val allCardsFlow: Flow<List<Card>> = catalogRepo.observeCards()

    /** One-shot snackbar message stream */
    private val _snackbar = MutableSharedFlow<String>()
    val snackbar = _snackbar.asSharedFlow()

    /**
     * On initialization:
     * - Attempt to sync owned decks from server if session token exists.
     */
    init{
        viewModelScope.launch {
            val token = tokenStore.tokenFlow.firstOrNull()?.trim().orEmpty()
            if (token.isNotEmpty()) {
                runCatching { deckRepo.syncOwnedDecksFromServer(token) }
            }
        }
    }

    /**
     * Manually refresh owned decks from server.
     */
    fun refreshDecksFromServer() {
        viewModelScope.launch {
            val token = tokenStore.tokenFlow.firstOrNull()?.trim().orEmpty()
            if (token.isNotEmpty()) {
                runCatching { deckRepo.syncOwnedDecksFromServer(token) }
            }
        }
    }

    /**
     * Import a deck using a share code.
     *
     * - Emits snackbar feedback.
     * - Syncs owned decks after successful import.
     */
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

    /**
     * Deletes a deck using server-first strategy.
     */
    fun deleteDeck(deckId: Long) {
        viewModelScope.launch {
            val token =tokenStore.tokenFlow.firstOrNull()?.trim().orEmpty()
            deckRepo.deleteDeckServerFirst(token,deckId)
        }
    }

    /**
     * Reactive UI list combining:
     * - Local decks
     * - Card metadata (leader resolution)
     * - Share code mapping
     *
     * Automatically updates when any source flow changes.
     */
    val decksUi: StateFlow<List<DeckListItemUi>> =
        combine(
            deckRepo.observeAllDecks(),
            allCardsFlow,
            deckRepo.shareCodeMap
        ) { decks, cards ,shareMap ->

            // Build lookup table for resolving leader metadata
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