package com.koi.thepiece.ui.screens.catalogscreen

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.koi.thepiece.AppGraph
import com.koi.thepiece.data.model.Card
import com.koi.thepiece.ui.screens.CardEntry
import com.koi.thepiece.ui.screens.CardVariant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

class CatalogViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = AppGraph.provideCatalogRepository(app)

    private val _state = MutableStateFlow(CatalogUiState())
    val state: StateFlow<CatalogUiState> = _state

    private val _prices = MutableStateFlow<Map<String, Int>>(emptyMap())
    val prices: StateFlow<Map<String, Int>> = _prices

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
            val result = repo.refreshCards(preloadFirstPageImages = true)
            result.exceptionOrNull()?.let { e ->
                _state.update { it.copy(loading = false, error = e.message ?: "Failed to refresh") }
            }
            _state.update { it.copy(loading = false) }
        }

    }




    // -------- Filters / Search --------
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
        _state.update { it.copy(selected = card)
                        it.copy(selectedID = card.id)
        }
    }

    fun closeModal() {
        _state.update { it.copy(selected = null)
                        it.copy(selectedID = null)
        }
    }

    // -------- Qty updates --------
    fun incrementQty(card: Card) {
        updateQty(card, card.ownedQty + 1)
    }

    fun decrementQty(card: Card) {
        updateQty(card, max(0, card.ownedQty - 1))
    }


    private fun updateQty(card: Card, newQty: Int) {
        viewModelScope.launch {
            val result = repo.updateOwnedQty(card.id, newQty)
            result.exceptionOrNull()?.let { e ->
                _state.update { it.copy(error = e.message ?: "Failed to update qty") }
            }
        }
    }

    // -------- Derived lists (filter + paging) --------
    fun filteredCards(s: CatalogUiState): List<Card> {
        val q = s.searchQuery.trim().lowercase()

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
                    selectedRarity == "all" ||cardRarity== getRareKey( selectedRarity)

                val matchSearch =
                    q.isEmpty() ||
                            (c.code ?: "").lowercase().contains(q) ||
                            c.name.lowercase().contains(q) ||
                            (c.traits ?: "").lowercase().contains(q)

                matchColor && matchSet && matchRarity && matchSearch&&matchCardType
            }
            .toList()
    }

    fun totalPages(s: CatalogUiState): Int {
        val total = filteredCards(s).size
        return max(1, ceil(total / s.pageSize.toDouble()).toInt())
    }

    fun pagedCards(s: CatalogUiState): List<Card> {
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

    //For OnePieceCardScan
    private val _detectedCards = MutableStateFlow<LinkedHashMap<String, CardEntry>>(linkedMapOf())
    val detectedCards: StateFlow<LinkedHashMap<String, CardEntry>> = _detectedCards.asStateFlow()

    fun updateDetectedCards(cards: LinkedHashMap<String, CardEntry>) {
        _detectedCards.value = cards
    }

    fun clearDetectedCards() {
        _detectedCards.value = linkedMapOf()
    }

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
                    repo.updateOwnedQty(cardId, current + entry.quantity)
                }
            clearDetectedCards()
        }
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


