package com.koi.thepiece.ui.screens.deckbuilderscreen

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.koi.thepiece.AppGraph
import com.koi.thepiece.data.local.TokenStore
import com.koi.thepiece.data.model.Card
import com.koi.thepiece.ui.screens.catalogscreen.CatalogSearchQueryExpander
import com.koi.thepiece.ui.screens.catalogscreen.components.OpJpMaps
import com.koi.thepiece.ui.screens.deckbuilderscreen.deckeditor.DeckLegality
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * ViewModel responsible for the deck builder workflow.
 *
 * This ViewModel was refactored from the Catalogue implementation (CatalogViewModel),
 * reusing its Room observation, filtering/search, pagination, suggestions, and price caching,
 * then extending it with deck-specific state and persistence.
 *
 * Responsibilities:
 * - Observes the local card catalogue (Room, offline-first UI)
 * - Refreshes catalogue data from backend using session token (network -> Room upsert)
 * - Provides filtering, search expansion, paging, and search suggestions
 * - Maintains owned stock quantities map for stock-aware deck building
 * - Manages deck composition (add/remove/clear) with rule constraints
 * - Computes and exposes deck legality
 * - Saves/loads decks via DeckRepository (server-first persistence)
 * - Fetches and caches external card prices keyed by marketplace URL
 *
 * Notes:
 * - Extends AndroidViewModel because Application is required to resolve repositories.
 * - TokenStore is used for authenticated server operations (refresh, save/update deck).
 */
class DeckViewModel(app: Application, private val tokenStore: TokenStore) : AndroidViewModel(app) {

    /** Picker mode controlling which subset of cards is displayed for selection. */
    enum class DeckEditorMode { Leader, Card }

    /**
     * Current editor mode (Leader picking or Card picking).
     * Affects filtering logic in [filteredCards].
     */
    var mode = DeckEditorMode.Leader

    /** [Shared with CatalogViewModel] Catalogue repository for Room observation, refresh, and price fetch. */
    private val repo = AppGraph.provideCatalogRepository(app)

    /** Deck persistence repository (save/load/overwrite deck data). */
    private val deckRepo = AppGraph.provideDeckRepository(app)

    /**
     * Map of cardId -> owned quantity.
     * Used to provide stockQty alongside requiredQty for deck building UI.
     */
    private val _ownedMap = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val ownedMap = _ownedMap.asStateFlow()

    /**
     * Single source of truth for DeckBuilderScreen state.
     * Mirrors the unidirectional StateFlow pattern used in CatalogViewModel.
     */
    private val _state = MutableStateFlow(DeckUiState())
    val state: StateFlow<DeckUiState> = _state

    /**
     * Emits the resulting deckId when a save completes.
     * UI can observe this to show success feedback / navigate.
     */
    private val _saveResult = MutableStateFlow<Long?>(null)
    val saveResult: StateFlow<Long?> = _saveResult

    /** [Shared with CatalogViewModel] Search suggestion dropdown state. */
    private val _suggestions = MutableStateFlow<List<String>>(emptyList())
    val suggestions: StateFlow<List<String>> = _suggestions

    /** [Shared with CatalogViewModel] Cached prices keyed by marketplace URL. */
    private val _prices = MutableStateFlow<Map<String, Int>>(emptyMap())
    val prices: StateFlow<Map<String, Int>> = _prices

    init {
        /**
         * [Shared with CatalogViewModel] Observe Room as offline-first UI source.
         * Any DB changes propagate to UI automatically.
         */
        viewModelScope.launch {
            repo.observeCards().collect { cards ->
                _state.update { it.copy(allCards = cards, loading = false, error = null) }
                clampPage()
            }
        }

        /** Load owned quantities from local DB (IO-bound). */
        viewModelScope.launch(Dispatchers.IO) {
            _ownedMap.value = repo.getOwnedQtyMap()
        }

        /** [Shared with CatalogViewModel] Background refresh on startup (network -> Room upsert). */
        refresh()
    }

    /**
     * [Shared with CatalogViewModel] Forces a network sync to refresh cards into Room.
     *
     * Extension over CatalogViewModel:
     * - Also reloads [_ownedMap] to keep stock quantities in sync with latest ownedQty.
     */
    fun refresh() {
        _state.update { it.copy(loading = true, error = null) }

        viewModelScope.launch {
            val token = tokenStore.tokenFlow.firstOrNull()?.trim().orEmpty()
            val result = repo.refreshCards(token, preloadFirstPageImages = true)

            result.exceptionOrNull()?.let { e ->
                _state.update { it.copy(loading = false, error = e.message ?: "Failed to refresh") }
            }

            _state.update { it.copy(loading = false) }
        }

        viewModelScope.launch(Dispatchers.IO) {
            _ownedMap.value = repo.getOwnedQtyMap()
        }
    }

    /**
     * [Shared with CatalogViewModel] Generates ranked search suggestions.
     *
     * Data sources:
     * - Card names and traits from the local database (Room -> state)
     * - Keys from mapping tables (OpJpMaps.NAME_MAP and OpJpMaps.TRAITS_MAP)
     *
     * Ranking:
     * - Suggestions that start with the query are preferred
     * - Shorter strings are preferred
     * - Final results are limited to 10 items
     */
    private fun updateSuggestions(query: String) {
        // Remove extra spaces
        val trimmed = query.trim()
        // Don't show suggestions if query too short
        if (trimmed.length < 2) {
            _suggestions.value = emptyList()
            return
        }

        val lowerQuery = trimmed.lowercase()

        // Build a big set of all searchable strings
        val allPossibleStrings = buildSet<String> {
            // Card names
            // Add all card names from database
            state.value.allCards.forEach {
                add(it.name)
                // Add traits too (if exists)
                it.traits?.let { trait -> add(trait) }
            }

            // Map keys (english)
            // Add English keys from NAME_MAP
            addAll(OpJpMaps.NAME_MAP.keys)
            // Add English keys from TRAITS_MAP
            addAll(OpJpMaps.TRAITS_MAP.keys)
        }

        // Filter anything that CONTAINS the typed text
        val ranked = allPossibleStrings
            .filter { it.lowercase().contains(lowerQuery) }
            // Ranking logic:
            // 1) Strings that start with query come first
            // 2) Then shorter strings come first
            .sortedWith(
                compareBy<String> {
                    !it.lowercase().startsWith(lowerQuery)
                }.thenBy { it.length }
            )
            // Limit to 10 suggestions
            .take(10)

        // Update state
        _suggestions.value = ranked
    }

    /**
     * [Shared with CatalogViewModel] Applies selected suggestion into search query,
     * resets paging, and clears suggestion dropdown.
     */
    fun selectSuggestion(suggestion: String) {
        // When user clicks suggestion,
        // put it into search bar
        _state.update { it.copy(searchQuery = suggestion, page = 1) }

        // Clear dropdown after selection
        _suggestions.value = emptyList()
    }

    /** Reloads owned quantities from local DB. */
    fun refreshOwnedMap() {
        viewModelScope.launch(Dispatchers.IO) {
            _ownedMap.value = repo.getOwnedQtyMap()
        }
    }

    // -------- Filters / Search --------

    /** Switches picker into Leader selection mode. */
    fun setLeaderPickMode() {
        mode = DeckEditorMode.Leader
    }

    /** Switches picker into Card selection mode (non-Leader/Don by default). */
    fun setCardPickMode() {
        mode = DeckEditorMode.Card
    }

    /** [Shared with CatalogViewModel] Updates active color filter and resets paging. */
    fun setColor(value: String) {
        _state.update { it.copy(color = value, page = 1) }
    }

    /** [Shared with CatalogViewModel] Updates active card type filter and resets paging. */
    fun setCardType(value: String) {
        _state.update { it.copy(cardType = value, page = 1) }
    }

    /** [Shared with CatalogViewModel] Updates active set filter and resets paging. */
    fun setSetFilter(value: String) {
        _state.update { it.copy(setFilter = value, page = 1) }
    }

    /** [Shared with CatalogViewModel] Updates active rarity filter and resets paging. */
    fun setRarityFilter(value: String) {
        _state.update { it.copy(rarityFilter = value, page = 1) }
    }

    /**
     * [Shared with CatalogViewModel] Updates search query, resets paging,
     * and updates suggestion dropdown.
     */
    fun setSearchQuery(value: String) {
        _state.update { it.copy(searchQuery = value, page = 1) }
        updateSuggestions(value)
    }

    // -------- Modal --------

    /** [Shared with CatalogViewModel] Opens card preview dialog. */
    fun openCard(card: Card) {
        _state.update { it.copy(selected = card) }
    }

    /** [Shared with CatalogViewModel] Closes card preview dialog. */
    fun closeModal() {
        _state.update { it.copy(selected = null) }
    }

    /**
     * Sets the chosen Leader card for this deck and recomputes legality.
     * (Deck-specific extension over CatalogViewModel selection model.)
     */
    fun setSelectedLeader(card: Card) {
        _state.update { it.copy(selectedLeader = card) }
        recomputeLegality()
    }

    // -------- Derived lists (filter + paging) --------

    /**
     * Returns filtered card list based on current state and editor mode.
     *
     * Derived from CatalogViewModel.filteredCards() with mode-specific rules:
     * - Leader mode: filtered list is intended for selecting a Leader.
     * - Card mode: excludes Leader and Don when cardType == "all", matching deck construction rules.
     *
     * Search expansion:
     * - Uses [CatalogSearchQueryExpander] to expand user query into equivalent tokens.
     * - Matches tokens against code, name, and traits fields (case-insensitive).
     */
    /**
     * Returns the filtered card list according to the current UI state.
     *
     * Shared behaviour with CatalogViewModel.filteredCards():
     * - Applies color / type / set / rarity filters
     * - Expands search query via CatalogSearchQueryExpander
     * - Matches search tokens against code, name, and traits (case-insensitive)
     *
     * DeckViewModel extension:
     * - Filtering differs based on [DeckEditorMode]:
     *   - Leader mode: intended for leader selection context
     *   - Card mode: when cardType == "all", excludes Leader and Don cards by default
     */
    fun filteredCards(s: DeckUiState): List<Card> {
        // Same query-expansion approach as CatalogViewModel.
        val searchTokens = CatalogSearchQueryExpander.expand(s.searchQuery)

        return when (mode) {

            // -----------------------------
            // Leader pick mode
            // -----------------------------
            DeckEditorMode.Leader -> {
                Log.d("Deck", "Deck Leader Mode")

                s.allCards.asSequence()
                    .filter { c ->
                        val matchColor =
                            s.color == "all" || c.color.equals(s.color, ignoreCase = true)

                        val matchCardType =
                            c.type.equals(s.cardType, ignoreCase = true)

                        val matchSet =
                            s.setFilter == "all" || (c.cardSet ?: "").equals(
                                s.setFilter,
                                ignoreCase = true
                            )

                        // Filter: rarity (maps display label -> internal key)
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

            // -----------------------------
            // Card pick mode
            // -----------------------------
            DeckEditorMode.Card -> {
                Log.d("Deck", "Deck Editor Mode")

                s.allCards.asSequence()
                    .filter { c ->
                        val matchColor =
                            s.color == "all" || c.color.equals(s.color, ignoreCase = true)

                        val isLeader = c.type.equals("Leader", ignoreCase = true)
                        val isDon = c.type.equals("Don", ignoreCase = true)
                        
                        // If user selects "all", we default-exclude Leader/Don (deck-building context).
                        // Otherwise, we match the requested type normally.
                        val matchCardType =
                            (s.cardType == "all" && !isLeader && !isDon) ||
                                    c.type.equals(s.cardType, ignoreCase = true)

                        val matchSet =
                            s.setFilter == "all" || (c.cardSet ?: "").equals(
                                s.setFilter,
                                ignoreCase = true
                            )

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

    /** [Shared with CatalogViewModel] Computes total pages from filtered list size and pageSize. */
    fun totalPages(s: DeckUiState): Int {
        val total = filteredCards(s).size
        return max(1, ceil(total / s.pageSize.toDouble()).toInt())
    }

    /** [Shared with CatalogViewModel] Returns the current page slice of the filtered list. */
    fun pagedCards(s: DeckUiState): List<Card> {
        val list = filteredCards(s)
        val start = (s.page - 1) * s.pageSize
        val end = min(start + s.pageSize, list.size)
        if (start >= list.size) return emptyList()
        return list.subList(start, end)
    }

    /** [Shared with CatalogViewModel] Advances to next page, clamped to totalPages. */
    fun nextPage() {
        _state.update { s ->
            val tp = totalPages(s)
            s.copy(page = min(tp, s.page + 1))
        }
    }

    /** [Shared with CatalogViewModel] Goes back one page, clamped at 1. */
    fun prevPage() {
        _state.update { s -> s.copy(page = max(1, s.page - 1)) }
    }

    /** [Shared with CatalogViewModel] Ensures current page stays valid after list changes. */
    private fun clampPage() {
        _state.update { s ->
            val tp = totalPages(s)
            s.copy(page = s.page.coerceIn(1, tp))
        }
    }

    /** [Shared with CatalogViewModel] Maps display rarity label (e.g. "A-SR") to internal key (e.g. "p-sr"). */
    private fun getRareKey(display: String): String {
        return RARITY_OPTIONS
            .firstOrNull { it.second.equals(display.trim(), ignoreCase = true) }
            ?.first
            ?: "all"
    }

    /** [Shared with CatalogViewModel] Rarity display-to-key mapping table. */
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

    // -------- Deck Composition --------

    /**
     * Clears all selected deck cards (keeps leader selection unchanged).
     * Recomputes legality after reset.
     */
    fun clearDeck() {
        _state.update { it.copy(deck = emptyMap<Int, QtyClass>()) }
        recomputeLegality()
    }

    /**
     * Adds a card into the deck with rule enforcement:
     * - Max 50 total cards in deck
     * - Max 4 copies per card
     *
     * Also injects stockQty from [ownedMap] so UI can compare required vs owned.
     * Recomputes legality after mutation.
     */
    fun addToDeck(card: Card) {
        _state.update { s ->
            val current = s.deck[card.id]
            val currentQty = current?.requiredQty ?: 0
            val totalCards = s.deck.values.sumOf { it.requiredQty }

            if (totalCards >= 50) return@update s
            if (currentQty >= 4) return@update s

            val stockQty = _ownedMap.value[card.id] ?: 0
            val newDeck = s.deck.toMutableMap()

            val updated = if (current == null) {
                QtyClass(requiredQty = 1, stockQty = stockQty)
            } else {
                current.copy(requiredQty = currentQty + 1, stockQty = stockQty)
            }

            newDeck[card.id] = updated
            s.copy(deck = newDeck)
        }

        recomputeLegality()
    }

    /**
     * Removes one copy of a card from the deck.
     * If quantity reaches 0, the entry is removed entirely.
     * Recomputes legality after mutation.
     */
    fun removeFromDeck(card: Card) {
        _state.update { s ->
            val current = s.deck[card.id] ?: return@update s
            val currentQty = current.requiredQty
            if (currentQty <= 0) return@update s

            val newDeck = s.deck.toMutableMap()
            if (currentQty == 1) newDeck.remove(card.id)
            else newDeck[card.id] = current.copy(requiredQty = currentQty - 1)

            s.copy(deck = newDeck)
        }
        recomputeLegality()
    }

    /** Returns required quantity for a card currently in the deck. */
    fun getFromDeckQty(card: Card): Int {
        val s = _state.value
        return s.deck[card.id]?.requiredQty ?: 0
    }

    /**
     * Returns owned stock quantity for a given cardId.
     *
     * Note:
     * - This triggers a refresh of ownedMap, so it may return a stale value
     *   for the current call; prefer observing [ownedMap] in UI where possible.
     */
    fun getFromStockQty(cardID: Int): Int {
        refreshOwnedMap()
        return _ownedMap.value[cardID] ?: 0
    }

    // -------- Price Fetch --------

    /** [Shared with CatalogViewModel] Fetches price via POST endpoint and caches by url. */
    fun fetchPrice(cardUrl: String?) {
        if (cardUrl.isNullOrBlank()) return
        if (_prices.value.containsKey(cardUrl)) return // avoid refetch spam

        viewModelScope.launch {
            val price = repo.GetPrice(cardUrl).getOrNull() ?: return@launch
            _prices.update { it + (cardUrl to price) }
        }
    }

    /** [Shared with CatalogViewModel] Fetches price via GET endpoint and caches by url. */
    fun fetchPrice2(cardUrl: String?) {
        if (cardUrl.isNullOrBlank()) return
        if (_prices.value.containsKey(cardUrl)) return // avoid refetch spam

        viewModelScope.launch {
            val price = repo.getPrice2(cardUrl).getOrNull() ?: return@launch
            _prices.update { it + (cardUrl to price) }
        }
    }

    // -------- Deck Persistence --------

    /**
     * Saves the current deck to the server (server-first).
     *
     * Behavior:
     * - If deckId is null -> creates a new deck
     * - Else -> overwrites the existing deck with current state
     *
     * Requires:
     * - Leader must be selected
     *
     * Emits:
     * - saveResult with deckId when completed
     */
    fun saveDeck(name: String) {
        val s = _state.value
        val leader = s.selectedLeader ?: return
        val cleanName = name.trim()

        viewModelScope.launch {
            val token = tokenStore.tokenFlow.firstOrNull()?.trim().orEmpty()

            if (s.deckId == null) {
                val newId = deckRepo.saveNewDeckServerFirst(
                    token,
                    name = cleanName,
                    leaderCardId = leader.id,
                    deckMap = s.deck
                )
                _state.update { it.copy(deckId = newId, deckName = cleanName) }
                _saveResult.value = newId
            } else {
                deckRepo.overwriteExistingDeckServerFirst(
                    token,
                    deckId = s.deckId,
                    name = cleanName,
                    leaderCardId = leader.id,
                    deckMap = s.deck
                )
                _state.update { it.copy(deckName = cleanName) }
                _saveResult.value = s.deckId
            }
        }
    }

    /**
     * Loads a saved deck into the editor state.
     *
     * - Retrieves deck and card quantities from repository
     * - Resolves leader Card instance from current cached allCards list
     * - Builds deckMap with requiredQty + stockQty for UI display
     */
    fun loadDeck(deckId: Long) {
        viewModelScope.launch {
            val loaded = deckRepo.getDeck(deckId) ?: return@launch

            val leaderCard = _state.value.allCards
                .firstOrNull { it.id == loaded.deck.leaderCardId }

            val deckMap = loaded.cards.associate { it.cardId to QtyClass(it.qty, repo.getStockQty(it.cardId)) }

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

    /**
     * Resets editor state for starting a fresh deck build session.
     * Clears leader, deck cards, legality status, and search/paging state.
     */
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

    /**
     * Recomputes legality of the current deck configuration.
     *
     * Checks are delegated to DeckLegality.check(), which evaluates:
     * - Leader presence
     * - Deck size constraints (requireExactly50)
     * - Color/type/duplicate restrictions as implemented by DeckLegality
     */
    private fun recomputeLegality() {
        val s = _state.value
        val result = DeckLegality.check(
            leader = s.selectedLeader,
            deckMap = s.deck,
            allCards = s.allCards,
            requireExactly50 = true
        )
        _state.update { it.copy(legality = result) }
    }
}