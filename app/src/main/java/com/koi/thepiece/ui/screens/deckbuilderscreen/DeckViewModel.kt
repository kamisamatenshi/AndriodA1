package com.koi.thepiece.ui.screens.deckbuilderscreen

import android.app.Application
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.koi.thepiece.AppGraph
import com.koi.thepiece.data.model.Card
import com.koi.thepiece.ui.screens.catalogscreen.CatalogSearchQueryExpander
import com.koi.thepiece.ui.screens.catalogscreen.CatalogUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

class DeckViewModel(app: Application) : AndroidViewModel(app) {

    enum class DeckEditorMode { Leader, Card }

    private val repo = AppGraph.provideCatalogRepository(app)
    private val deckRepo = AppGraph.provideDeckRepository(app)
    private val _state = MutableStateFlow(DeckUiState())
    val state: StateFlow<DeckUiState> = _state

    private val _saveResult = MutableStateFlow<Long?>(null)
    val saveResult: StateFlow<Long?> = _saveResult
    var mode = DeckEditorMode.Leader

    init {
        // Observe local DB (offline cache)
        viewModelScope.launch {
            repo.observeCards().collect { cards ->
                _state.update { it.copy(allCards = cards, loading = false, error = null) }
                // keep page in range after updates
                clampPage()
            }
        }

        // Refresh in background (network -> Room upsert)
        refresh()
    }

    fun refresh() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val result = repo.refreshCards(AppGraph.token,preloadFirstPageImages = true)
            result.exceptionOrNull()?.let { e ->
                _state.update { it.copy(loading = false, error = e.message ?: "Failed to refresh") }
            }
            _state.update { it.copy(loading = false) }
        }
    }




    // -------- Filters / Search --------
    fun setLeaderPickMode(){
        mode = DeckEditorMode.Leader
    }

    fun setCardPickMode(){
        mode = DeckEditorMode.Card
    }

    fun setColor(value: String) {
        _state.update { it.copy(color = value, page = 1) }
    }

    fun setCardType(value: String) {
        _state.update { it.copy(cardType = value, page = 1) }
    }


    fun setSetFilter(value: String) {
        _state.update { it.copy(setFilter = value, page = 1) }
    }

    fun setRarityFilter(value: String) {
        _state.update { it.copy(rarityFilter = value, page = 1) }
    }

    fun setSearchQuery(value: String) {
        _state.update { it.copy(searchQuery = value, page = 1) }
    }



    // -------- Modal --------
    fun openCard(card: Card) {
        _state.update { it.copy(selected = card) }
    }

    fun closeModal() {
        _state.update { it.copy(selected = null) }
    }

    fun setSelectedLeader(card: Card) {
        _state.update { it.copy(selectedLeader = card) }
        recomputeLegality()
    }

    // -------- Derived lists (filter + paging) --------
    fun filteredCards(s: DeckUiState): List<Card> {
        // It normalizes the user input ->
        // Then It checks your NAME_MAP ->
        // Then It checks your TRAITS_MAP ->
        // Then It returns a Set<String> of possible matches
        val searchTokens = CatalogSearchQueryExpander.expand(s.searchQuery)

        when (mode) {
            DeckEditorMode.Leader -> {
                Log.d("Deck", "Deck Leader Mode")
                return s.allCards.asSequence()
                    .filter { c ->
                        val matchColor =
                            s.color == "all" ||
                                    c.color.equals(s.color, ignoreCase = true)

                        val matchCardType = c.type.equals(s.cardType, ignoreCase = true)

                        val matchSet =
                            s.setFilter == "all" ||
                                    (c.cardSet ?: "").equals(s.setFilter, ignoreCase = true)

                        val selectedRarity = s.rarityFilter.trim().lowercase()
                        val cardRarity = (c.rarity ?: "").trim().lowercase()
                        val matchRarity =
                            selectedRarity == "all" || cardRarity == getRareKey(selectedRarity)

                        val code = (c.code ?: "").lowercase()
                        val name = c.name.lowercase()
                        val traits = (c.traits ?: "").lowercase()

                        val matchSearch =
                            searchTokens.isEmpty() ||
                                    searchTokens.any { t ->         //search using a set of possible equivalent strings.
                                        // Search “code/name/traits” with the token.
                                        val tn = t.lowercase()   // turns all to lowercase too
                                        code.contains(tn) || name.contains(tn) || traits.contains(tn)
                                    }
                        matchColor && matchCardType && matchSet && matchRarity && matchSearch
                    }
                    .toList()
            }

            DeckEditorMode.Card -> {

                Log.d("Deck", "Deck Editor Mode")
                return s.allCards.asSequence()
                    .filter { c ->
                        val matchColor =
                            s.color == "all" ||
                                    c.color.equals(s.color, ignoreCase = true)

                        val isLeader = c.type.equals("Leader", ignoreCase = true)
                        val isDon = c.type.equals("Don", ignoreCase = true)

                        val matchCardType =
                            (s.cardType == "all" && !isLeader && !isDon) || c.type.equals(s.cardType, ignoreCase = true)

                        val matchSet =
                            s.setFilter == "all" ||
                                    (c.cardSet ?: "").equals(s.setFilter, ignoreCase = true)

                        val selectedRarity = s.rarityFilter.trim().lowercase()
                        val cardRarity = (c.rarity ?: "").trim().lowercase()
                        val matchRarity =
                            selectedRarity == "all" || cardRarity == getRareKey(selectedRarity)

                        val code = (c.code ?: "").lowercase()
                        val name = c.name.lowercase()
                        val traits = (c.traits ?: "").lowercase()

                        val matchSearch =
                            searchTokens.isEmpty() ||
                                    searchTokens.any { t ->         //search using a set of possible equivalent strings.
                                        // Search “code/name/traits” with the token.
                                        val tn = t.lowercase()   // turns all to lowercase too
                                        code.contains(tn) || name.contains(tn) || traits.contains(tn)
                                    }
                        matchColor && matchCardType && matchSet && matchRarity && matchSearch
                    }
                    .toList()
            }
        }
    }

    fun totalPages(s: DeckUiState): Int {
        val total = filteredCards(s).size
        return max(1, ceil(total / s.pageSize.toDouble()).toInt())
    }


    fun pagedCards(s: DeckUiState): List<Card> {
        val list = filteredCards(s)
        val start = (s.page - 1) * s.pageSize
        val end = min(start + s.pageSize, list.size)
        if (start >= list.size) return emptyList()
        return list.subList(start, end)
    }

    fun nextPage() {
        _state.update { s ->
            val tp = totalPages(s)
            s.copy(page = min(tp, s.page + 1))
        }
    }

    fun prevPage() {
        _state.update { s -> s.copy(page = max(1, s.page - 1)) }
    }

    private fun clampPage() {
        _state.update { s ->
            val tp = totalPages(s)
            s.copy(page = s.page.coerceIn(1, tp))
        }
    }

    private fun getRareKey(display: String) : String
    {
        return RARITY_OPTIONS
            .firstOrNull { it.second.equals(display.trim(), ignoreCase = true) }
            ?.first
            ?: "all"
    }

    private val RARITY_OPTIONS = listOf(
        "all" to "All",
        "p-sec" to "A-SEC",
        "sec" to "SEC",
        "p-sr" to "A-SR",
        "sr" to "SR",
        "p-r" to "A-R",
        "r" to "R",
        "p-l" to "A-L",
        "l" to "L",
        "sp" to "SP",
        "uc" to "UC",
        "c" to "C",
    )

    // for deck list
    fun clearDeck() {
        _state.update { it.copy(deck = emptyMap<Int, Int>()) }
        recomputeLegality()
    }

    fun addToDeck(card: Card) {
        _state.update { s ->
            val currentQty = s.deck[card.id] ?: 0
            val totalCards = s.deck.values.sum()
            if (totalCards >= 50) return@update s
            if (currentQty >= 4) return@update s

            val newDeck = s.deck.toMutableMap()
            newDeck[card.id] = currentQty + 1
            s.copy(deck = newDeck)
        }
        recomputeLegality()
    }

    fun removeFromDeck(card: Card) {
        _state.update { s ->
            val currentQty = s.deck[card.id] ?: 0
            if (currentQty <= 0) return@update s

            val newDeck: MutableMap<Int, Int> = s.deck.toMutableMap()

            if (currentQty == 1) newDeck.remove(card.id)
            else newDeck[card.id] = currentQty - 1

            s.copy(deck = newDeck)
        }
        recomputeLegality()
    }

    private val _prices = MutableStateFlow<Map<String, Int>>(emptyMap())
    val prices: StateFlow<Map<String, Int>> = _prices

    fun fetchPrice(cardUrl: String?) {
        if (cardUrl.isNullOrBlank()) return
        if (_prices.value.containsKey(cardUrl)) return // avoid refetch spam

        viewModelScope.launch {
            val price = repo.GetPrice(cardUrl).getOrNull() ?: return@launch
            _prices.update { it + (cardUrl to price) }
        }
    }

    fun fetchPrice2(cardUrl: String?) {
        if (cardUrl.isNullOrBlank()) return
        if (_prices.value.containsKey(cardUrl)) return // avoid refetch spam

        viewModelScope.launch {
            val price = repo.getPrice2(cardUrl).getOrNull() ?: return@launch
            _prices.update { it + (cardUrl to price) }
        }
    }

    fun saveDeck(name: String) {
        val s = _state.value
        val leader = s.selectedLeader ?: return
        val cleanName = name.trim()

        viewModelScope.launch {
            if (s.deckId == null) {
                // NEW deck
                val newId = deckRepo.saveNewDeck(
                    name = cleanName,
                    leaderCardId = leader.id,
                    deckMap = s.deck
                )
                _state.update { it.copy(deckId = newId, deckName = cleanName) }
                _saveResult.value = newId
            } else {
                // UPDATE existing deck
                deckRepo.overwriteExistingDeck(
                    deckId = s.deckId!!,
                    name = cleanName,
                    leaderCardId = leader.id,
                    deckMap = s.deck
                )
                _state.update { it.copy(deckName = cleanName) }
                _saveResult.value = s.deckId
            }
        }
    }

    fun loadDeck(deckId: Long) {
        viewModelScope.launch {
            val loaded = deckRepo.getDeck(deckId) ?: return@launch

            val leaderCard = _state.value.allCards
                .firstOrNull { it.id == loaded.deck.leaderCardId }

            val deckMap = loaded.cards.associate { it.cardId to it.qty }

            _state.update {
                it.copy(
                    deckId = loaded.deck.deckId,
                    deckName = loaded.deck.name,
                    selectedLeader = leaderCard,
                    deck = deckMap
                )
            }
            recomputeLegality()
        }
    }

    fun startNewDeck() {
        _state.update {
            it.copy(
                editingDeckId = null,
                deckId = null,
                deckName = "",
                selectedLeader = null,
                deck = emptyMap(),
                legality = null,
                page = 1,
                searchQuery = ""
            )
        }
    }

    private fun recomputeLegality() {
        val s = _state.value
        val result = DeckLegality.check(
            leader = s.selectedLeader,
            deckMap = s.deck,
            allCards = s.allCards,
            requireExactly50 = true // when you want “Save” to require full deck
        )
        _state.update { it.copy(legality = result) }
    }
}


