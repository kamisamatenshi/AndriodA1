package com.koi.thepiece.ui.screens.catalogscreen

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.koi.thepiece.AppGraph
import com.koi.thepiece.data.local.TokenStore
import com.koi.thepiece.data.model.Card
import com.koi.thepiece.data.repo.SetCompletion
import com.koi.thepiece.ui.screens.CardEntry
import com.koi.thepiece.ui.screens.CardVariant
import com.koi.thepiece.ui.screens.catalogscreen.components.OpJpMaps
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel for the Catalogue feature.
 *
 * Responsibilities:
 * - Observes the local Room cache of cards (offline-first UI)
 * - Triggers a background refresh to sync server card data into Room
 * - Stores and updates catalogue UI state (filters, paging, selection, loading/error)
 * - Performs derived computations (filtered lists, pagination, completion, total net worth)
 * - Coordinates owned quantity updates (optimistic local update in repository + server sync)
 * - Fetches and caches external price values keyed by marketplace URL
 * - Provides search suggestions and query expansion support
 * - Supports scanner "save detected cards" flow (batch apply quantities)
 *
 * Architecture:
 * - MVVM + unidirectional data flow
 * - State stored as immutable CatalogUiState in a MutableStateFlow
 * - Repository injected via AppGraph (service locator pattern)
 */
class CatalogViewModel(app: Application , private val tokenStore: TokenStore) : AndroidViewModel(app) {

    /** Repository providing Room observation, sync, qty updates and price fetch. */
    private val repo = AppGraph.provideCatalogRepository(app)

    // -------------------------
    // UI State
    // -------------------------

    /** Backing state for the catalogue screen (single source of truth). */
    private val _state = MutableStateFlow(CatalogUiState())

    /** Exposed immutable state flow to the UI. */
    val state: StateFlow<CatalogUiState> = _state


    // -------------------------
    // Price Cache State
    // -------------------------

    /**
     * Cached prices keyed by yuyuUrl.
     * Used by card detail dialog and for computing total net worth.
     */
    private val _prices = MutableStateFlow<Map<String, Int>>(emptyMap())
    val prices: StateFlow<Map<String, Int>> = _prices

    // -------------------------
    // Search Suggestions State
    // -------------------------

    /**
     * Search suggestion results shown under the search bar.
     * Example: typing "mon" -> ["Monkey D. Luffy", "Monkey D. Garp", ...]
     */
    private val _suggestions = MutableStateFlow<List<String>>(emptyList())
    // Exposed as immutable StateFlow so UI can observe it
    val suggestions: StateFlow<List<String>> = _suggestions

    // -------------------------
    // Currency Toggle State
    // -------------------------

    /** Controls whether UI displays SGD or JPY values. */
    private val _isSgd = MutableStateFlow(false)
    val isSgd: StateFlow<Boolean> = _isSgd

    /**
     * Derived state: total net worth based on owned quantities and cached prices.
     *
     * - Source-of-truth currency is JPY (as returned by get_price endpoint).
     * - A card contributes value only when ownedQty > 0 and a price exists in cache.
     * - Recomputed whenever either card list state or price map changes.
     */
    val totalNetWorth: StateFlow<Int> =
        combine(_state, _prices) { state, pricesMap ->

            state.allCards.sumOf { card ->
                val url = card.yuyuUrl
                val price = if (!url.isNullOrBlank()) pricesMap[url] else null

                if (card.ownedQty > 0 && price != null) {
                    price * card.ownedQty
                } else 0
            }

        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            0
        )

    // -------------------------
    // Initialization
    // -------------------------
    init {
        /**
         * Observes Room as the primary UI source (offline-first).
         * Whenever the DB changes, the screen updates automatically.
         */
        viewModelScope.launch {
            repo.observeCards().collect { cards ->
                _state.update { it.copy(allCards = cards, loading = false, error = null) }
                // keep page in range after updates
                clampPage()
            }


        }
        /**
         * Trigger a background refresh on startup to synchronize with server.
         * Repository handles network -> Room upsert.
         */
        refresh()
    }

    // -------------------------
    // Data Refresh
    // -------------------------

    /**
     * Forces a network sync to refresh cards and store into Room.
     * UI remains driven by Room; refresh updates loading/error flags only.
     */
    fun refresh() {

        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val token =tokenStore.tokenFlow.firstOrNull()?.trim().orEmpty()
            val result = repo.refreshCards(token,preloadFirstPageImages = true)
            result.exceptionOrNull()?.let { e ->
                _state.update { it.copy(loading = false, error = e.message ?: "Failed to refresh") }
            }
            _state.update { it.copy(loading = false) }
        }

    }




    // -------------------------
    // Filters / Search
    // -------------------------

    /** Updates active color filter and resets paging to first page. */
    fun setColor(value: String) {
        _state.update { it.copy(color = value, page = 1) }
    }

    /** Updates active card type filter and resets paging to first page. */
    fun setCardType(value: String) {
        _state.update { it.copy(cardType = value, page = 1) }
    }

    /** Updates active set filter and resets paging to first page. */
    fun setSetFilter(value: String) {
        _state.update { it.copy(setFilter = value, page = 1) }
    }

    /** Updates active rarity filter and resets paging to first page. */
    fun setRarityFilter(value: String) {
        _state.update { it.copy(rarityFilter = value, page = 1) }
    }

    /**
     * Updates search query string and resets paging.
     * Also regenerates search suggestions as user types.
     */
    fun setSearchQuery(value: String) {
        _state.update { it.copy(searchQuery = value, page = 1) }
        updateSuggestions(value)
    }



    // -------------------------
    // Modal / Selection
    // -------------------------

    /**
     * Opens the card preview dialog by storing both the selected card reference
     * and its id. selectedID is typically used to resolve the latest card instance
     * from allCards.
     */
    fun openCard(card: Card) {
        _state.update {
            it.copy(
                selected = card,
                selectedID = card.id
            )
        }
    }

    /** Closes the card preview dialog by clearing selection state. */
    fun closeModal() {
        _state.update {
            it.copy(
                selected = null,
                selectedID = null
            )
        }
    }

    // -------------------------
    // Owned Quantity Updates
    // -------------------------

    /** Increments owned quantity by one. */
    fun incrementQty(card: Card) {
        updateQty(card, card.ownedQty + 1)
    }

    /** Decrements owned quantity by one, clamped at 0. */
    fun decrementQty(card: Card) {
        updateQty(card, max(0, card.ownedQty - 1))
    }

    /**
     * Updates owned quantity through repository.
     *
     * Repository performs:
     * - local optimistic update in Room
     * - server update via update_qty endpoint
     *
     * ViewModel is responsible for token retrieval and surfacing error messages.
     */
    private fun updateQty(card: Card, newQty: Int) {
        viewModelScope.launch {
            val token =tokenStore.tokenFlow.firstOrNull()?.trim().orEmpty()
            val result = repo.updateOwnedQty(token,card.id, newQty)
            result.exceptionOrNull()?.let { e ->
                _state.update { it.copy(error = e.message ?: "Failed to update qty") }
            }
        }
    }

    // -------------------------
    // Derived Lists (Filtering + Paging)
    // -------------------------

    /**
     * Returns the filtered card list according to current UI state:
     * - color, type, set, rarity filters
     * - expanded search tokens matched against code, name and traits
     */
    fun filteredCards(s: CatalogUiState): List<Card> {
        // It normalizes the user input ->
        // Then It checks your NAME_MAP ->
        // Then It checks your TRAITS_MAP ->
        // Then It returns a Set<String> of possible matches
        val searchTokens = CatalogSearchQueryExpander.expand(s.searchQuery)


        return s.allCards.asSequence()
            .filter { c ->
                val matchColor =
                    s.color == "all" ||
                            c.color.equals(s.color, ignoreCase = true)

                val matchCardType =
                    s.cardType =="all" ||
                            c.type.equals(s.cardType, ignoreCase = true)

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

    /** Computes total pages based on filtered list size and current pageSize. */
    fun totalPages(s: CatalogUiState): Int {
        val total = filteredCards(s).size
        return max(1, ceil(total / s.pageSize.toDouble()).toInt())
    }

    /** Returns the current page slice of the filtered list. */
    fun pagedCards(s: CatalogUiState): List<Card> {
        val list = filteredCards(s)
        val start = (s.page - 1) * s.pageSize
        val end = min(start + s.pageSize, list.size)
        if (start >= list.size) return emptyList()
        return list.subList(start, end)
    }

    /** Advances to next page, clamped to totalPages. */
    fun nextPage() {
        _state.update { s ->
            val tp = totalPages(s)
            s.copy(page = min(tp, s.page + 1))
        }
    }

    /** Goes back to previous page, clamped at 1. */
    fun prevPage() {
        _state.update { s -> s.copy(page = max(1, s.page - 1)) }
    }

    /** Ensures current page stays valid after list changes. */
    private fun clampPage() {
        _state.update { s ->
            val tp = totalPages(s)
            s.copy(page = s.page.coerceIn(1, tp))
        }
    }

    /**
     * Maps the displayed rarity option (e.g., "A-SR") into the internal rarity key
     * used by the card data (e.g., "p-sr").
     */
    private fun getRareKey(display: String) : String
    {
        return RARITY_OPTIONS
            .firstOrNull { it.second.equals(display.trim(), ignoreCase = true) }
            ?.first
            ?: "all"
    }

    // -------------------------
    // Completion Tracking
    // -------------------------

    /**
     * Observes completion for a selected set.
     * Completion counts a card as collected when ownedQty > 0.
     */
    fun observeSetCompletion(cardSet: String): Flow<SetCompletion> {
        return repo.observeSetCompletion(cardSet)
    }



    // -------------------------
    // Price Fetch + Cache
    // -------------------------

    /**
     * Fetches price via repository using POST endpoint and caches by url.
     * Skips if url is blank or already cached.
     */

    fun fetchPrice(cardUrl: String?) {
        if (cardUrl.isNullOrBlank()) return
        if (_prices.value.containsKey(cardUrl)) return // avoid refetch spam

        viewModelScope.launch {
            val price = repo.GetPrice(cardUrl).getOrNull() ?: return@launch
            _prices.update { it + (cardUrl to price) }
        }
    }

    /**
     * Fetches price via repository using GET endpoint and caches by url.
     * Skips if url is blank or already cached.
     */
    fun fetchPrice2(cardUrl: String?) {
        if (cardUrl.isNullOrBlank()) return
        if (_prices.value.containsKey(cardUrl)) return // avoid refetch spam

        viewModelScope.launch {
            val price = repo.getPrice2(cardUrl).getOrNull() ?: return@launch
            _prices.update { it + (cardUrl to price) }
        }
    }

    // -------------------------
    // Currency Toggle
    // -------------------------

    /** Toggles currency display mode (SGD / JPY). */
    fun toggleCurrency() {
        _isSgd.value = !_isSgd.value
    }

    // -------------------------
    // Scanner Integration
    // -------------------------

    /**
     * Stores OCR-detected card entries (code -> CardEntry).
     * LinkedHashMap preserves insertion order for UI display.
     */
    private val _detectedCards = MutableStateFlow<LinkedHashMap<String, CardEntry>>(linkedMapOf())
    val detectedCards: StateFlow<LinkedHashMap<String, CardEntry>> = _detectedCards.asStateFlow()

    /** Replaces the detected cards map with the latest OCR detection results. */
    fun updateDetectedCards(cards: LinkedHashMap<String, CardEntry>) {
        _detectedCards.value = cards
    }

    /** Clears detection results after saving or when user resets scan. */
    fun clearDetectedCards() {
        _detectedCards.value = linkedMapOf()
    }

    /**
     * Applies scanned quantities into owned quantities.
     *
     * Flow:
     * - Ignore UNKNOWN variants
     * - Resolve selectedCardId if present, otherwise match by (code + rarity->variant)
     * - Read current ownedQty from allCards
     * - Call repository updateOwnedQty to persist changes (Room + server)
     * - Clear detection map after completion
     */
    fun saveDetectedCards(
        entries: List<CardEntry>,
        allCards: List<Card>
    ) {
        viewModelScope.launch {
            entries
                .filter { it.variant != CardVariant.UNKNOWN }
                .forEach { entry ->
                    val cardId = entry.selectedCardId
                        ?: allCards.find { card ->
                            card.code.equals(entry.code, ignoreCase = true) &&
                                    CardVariant.fromRarity(card.rarity) == entry.variant
                        }?.id
                        ?: return@forEach

                    val current = allCards.find { it.id == cardId }?.ownedQty ?: 0
                    val token =tokenStore.tokenFlow.firstOrNull()?.trim().orEmpty()
                    repo.updateOwnedQty(token,cardId, current + entry.quantity)
                }
            clearDetectedCards()
        }
    }

    // -------------------------
    // Search Suggestions
    // -------------------------

    /**
     * Generates suggestion strings for the search dropdown.
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

    fun selectSuggestion(suggestion: String) {
        // When user clicks suggestion,
        // put it into search bar
        _state.update { it.copy(searchQuery = suggestion, page = 1) }

        // Clear dropdown after selection
        _suggestions.value = emptyList()
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

}


